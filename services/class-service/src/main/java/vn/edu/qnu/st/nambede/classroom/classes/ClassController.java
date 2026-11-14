package vn.edu.qnu.st.nambede.classroom.classes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassRequest;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassResponse;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ClassStudentResponse;
import vn.edu.qnu.st.nambede.classroom.classes.dto.ExistsResponse;
import vn.edu.qnu.st.nambede.classroom.classes.dto.JoinClassRequest;
import vn.edu.qnu.st.nambede.classroom.security.AuthUser;

/**
 * Role rules are in SecurityConfig (create: TEACHER, join/leave: STUDENT).
 * Rules that depend on the class itself (only its teacher or an admin) are in ClassService.
 */
@Tag(name = "Lớp học", description = "Lớp, mã lớp, sinh viên trong lớp")
@RestController
@RequestMapping("/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @Operation(summary = "Danh sách lớp: giảng viên thấy lớp mình dạy, sinh viên thấy lớp đã tham gia, admin thấy tất cả")
    @GetMapping
    public List<ClassResponse> list(@AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return classService.list(caller, authorization);
    }

    @Operation(summary = "Tạo lớp, sinh mã lớp 6 ký tự (TEACHER)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse create(@Valid @RequestBody ClassRequest request, @AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return classService.create(request, caller, authorization);
    }

    @Operation(summary = "Tham gia lớp bằng mã, không phân biệt hoa thường (STUDENT)")
    @PostMapping("/join")
    public ClassResponse join(@Valid @RequestBody JoinClassRequest request, @AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return classService.join(request.code(), caller, authorization);
    }

    @Operation(summary = "Chi tiết một lớp")
    @GetMapping("/{id}")
    public ClassResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return classService.get(id, caller, authorization);
    }

    @Operation(summary = "Sửa tên, mô tả lớp (giảng viên của lớp, ADMIN)")
    @PutMapping("/{id}")
    public ClassResponse update(@PathVariable Long id, @Valid @RequestBody ClassRequest request,
            @AuthenticationPrincipal AuthUser caller, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return classService.update(id, request, caller, authorization);
    }

    @Operation(summary = "Xóa lớp (giảng viên của lớp, ADMIN)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller) {
        classService.delete(id, caller);
    }

    @Operation(summary = "Rời lớp (STUDENT)")
    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller) {
        classService.leave(id, caller);
    }

    @Operation(summary = "Sinh viên trong lớp, kèm họ tên lấy từ Auth Service (giảng viên của lớp, ADMIN)")
    @GetMapping("/{id}/students")
    public List<ClassStudentResponse> students(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return classService.students(id, caller, authorization);
    }

    @Operation(summary = "Xóa một sinh viên khỏi lớp (giảng viên của lớp, ADMIN)")
    @DeleteMapping("/{id}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudent(@PathVariable Long id, @PathVariable Long studentId,
            @AuthenticationPrincipal AuthUser caller) {
        classService.removeStudent(id, studentId, caller);
    }

    /** Membership check for the student, the class teacher and admins. */
    @Operation(summary = "Sinh viên có trong lớp không")
    @GetMapping("/{id}/students/{studentId}/exists")
    public ExistsResponse exists(@PathVariable Long id, @PathVariable Long studentId,
            @AuthenticationPrincipal AuthUser caller) {
        return new ExistsResponse(classService.isMember(id, studentId, caller));
    }
}
