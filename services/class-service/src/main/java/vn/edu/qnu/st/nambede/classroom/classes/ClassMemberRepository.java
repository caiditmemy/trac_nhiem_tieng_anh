package vn.edu.qnu.st.nambede.classroom.classes;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassMemberRepository extends JpaRepository<ClassMember, ClassMember.Key> {

    List<ClassMember> findByClassIdOrderByJoinedAtAsc(Long classId);

    boolean existsByClassIdAndStudentId(Long classId, Long studentId);

    /** Rows of [classId, number of students]. */
    @Query("select m.classId, count(m.studentId) from ClassMember m where m.classId in :classIds group by m.classId")
    List<Object[]> countStudents(@Param("classIds") Collection<Long> classIds);
}
