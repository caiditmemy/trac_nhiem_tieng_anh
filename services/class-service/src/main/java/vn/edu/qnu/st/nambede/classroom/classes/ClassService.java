package vn.edu.qnu.st.nambede.classroom.classes;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qnu.st.nambede.classroom.auth.AuthClient;
import vn.edu.qnu.st.nambede.classroom.auth.UserSummary;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassRequest;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassResponse;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassStudentResponse;
import vn.edu.qnu.st.nambede.classroom.common.ApiException;
import vn.edu.qnu.st.nambede.classroom.security.AuthUser;

/**
 * Classes and their students. A class is managed by its teacher or by an admin.
 * {@code authorization} is the caller's "Bearer ..." header, forwarded to auth-service to read names.
 */
@Service
public class ClassService {

    private static final int MAX_CODE_ATTEMPTS = 10;

    private final ClassroomRepository classes;
    private final ClassMemberRepository members;
    private final ClassCodeGenerator codeGenerator;
    private final AuthClient authClient;

    public ClassService(ClassroomRepository classes, ClassMemberRepository members, ClassCodeGenerator codeGenerator,
            AuthClient authClient) {
        this.classes = classes;
        this.members = members;
        this.codeGenerator = codeGenerator;
        this.authClient = authClient;
    }

    /** Teacher: classes they teach. Student: classes they joined. Admin: every class. */
    @Transactional(readOnly = true)
    public List<ClassResponse> list(AuthUser caller, String authorization) {
        List<Classroom> result = switch (caller.role()) {
            case "TEACHER" -> classes.findByTeacherIdOrderByCreatedAtDesc(caller.id());
            case "STUDENT" -> classes.findJoinedBy(caller.id());
            default -> classes.findAllByOrderByCreatedAtDesc();
        };
        return toResponses(result, authorization);
    }

    @Transactional
    public ClassResponse create(ClassRequest request, AuthUser teacher, String authorization) {
        Classroom classroom = classes.save(
                new Classroom(request.name().trim(), blankToNull(request.description()), newCode(), teacher.id()));
        return toResponses(List.of(classroom), authorization).get(0);
    }

    @Transactional(readOnly = true)
    public ClassResponse get(Long classId, AuthUser caller, String authorization) {
        Classroom classroom = find(classId);
        boolean isMember = "STUDENT".equals(caller.role()) && members.existsByClassIdAndStudentId(classId, caller.id());
        if (!canManage(classroom, caller) && !isMember) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not in this class");
        }
        return toResponses(List.of(classroom), authorization).get(0);
    }

    @Transactional
    public ClassResponse update(Long classId, ClassRequest request, AuthUser caller, String authorization) {
        Classroom classroom = findManageable(classId, caller);
        classroom.setName(request.name().trim());
        classroom.setDescription(blankToNull(request.description()));
        return toResponses(List.of(classroom), authorization).get(0);
    }

    @Transactional
    public void delete(Long classId, AuthUser caller) {
        classes.delete(findManageable(classId, caller));
    }

    @Transactional
    public ClassResponse join(String code, AuthUser student, String authorization) {
        Classroom classroom = classes.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> ApiException.notFound("Class code not found"));
        if (members.existsByClassIdAndStudentId(classroom.getId(), student.id())) {
            throw ApiException.conflict("You already joined this class");
        }
        members.save(new ClassMember(classroom.getId(), student.id()));
        return toResponses(List.of(classroom), authorization).get(0);
    }

    @Transactional
    public void leave(Long classId, AuthUser student) {
        ClassMember member = members.findById(new ClassMember.Key(classId, student.id()))
                .orElseThrow(() -> ApiException.notFound("You are not in this class"));
        members.delete(member);
    }

    @Transactional(readOnly = true)
    public List<ClassStudentResponse> students(Long classId, AuthUser caller, String authorization) {
        findManageable(classId, caller);
        List<ClassMember> list = members.findByClassIdOrderByJoinedAtAsc(classId);
        Map<Long, UserSummary> users = authClient.lookup(list.stream().map(ClassMember::getStudentId).toList(),
                authorization);
        return list.stream().map(member -> {
            UserSummary user = users.get(member.getStudentId());
            return new ClassStudentResponse(member.getStudentId(), user == null ? null : user.fullName(),
                    user == null ? null : user.email(), member.getJoinedAt());
        }).toList();
    }

    @Transactional
    public void removeStudent(Long classId, Long studentId, AuthUser caller) {
        findManageable(classId, caller);
        ClassMember member = members.findById(new ClassMember.Key(classId, studentId))
                .orElseThrow(() -> ApiException.notFound("Student " + studentId + " is not in this class"));
        members.delete(member);
    }

    /** Used by other services (exercise-service): is this student in this class? */
    @Transactional(readOnly = true)
    public boolean isMember(Long classId, Long studentId, AuthUser caller) {
        boolean asksAboutSelf = caller.id().equals(studentId);
        if (!asksAboutSelf && !canManage(find(classId), caller)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot check this membership");
        }
        return members.existsByClassIdAndStudentId(classId, studentId);
    }

    private boolean canManage(Classroom classroom, AuthUser caller) {
        return "ADMIN".equals(caller.role())
                || ("TEACHER".equals(caller.role()) && classroom.getTeacherId().equals(caller.id()));
    }

    private Classroom findManageable(Long classId, AuthUser caller) {
        Classroom classroom = find(classId);
        if (!canManage(classroom, caller)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the teacher of this class can do this");
        }
        return classroom;
    }

    private Classroom find(Long classId) {
        return classes.findById(classId).orElseThrow(() -> ApiException.notFound("Class " + classId + " not found"));
    }

    private String newCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.next();
            if (!classes.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique class code");
    }

    /** Adds student counts and teacher names (one query and one auth-service call for the whole list). */
    private List<ClassResponse> toResponses(List<Classroom> list, String authorization) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : members.countStudents(list.stream().map(Classroom::getId).toList())) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        Map<Long, UserSummary> teachers = authClient.lookup(
                list.stream().map(Classroom::getTeacherId).distinct().toList(), authorization);
        return list.stream()
                .map(c -> ClassResponse.of(c, counts.getOrDefault(c.getId(), 0L), teachers.get(c.getTeacherId())))
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
