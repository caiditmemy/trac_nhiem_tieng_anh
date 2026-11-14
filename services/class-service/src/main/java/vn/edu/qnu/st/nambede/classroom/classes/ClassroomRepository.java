package vn.edu.qnu.st.nambede.classroom.classes;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    List<Classroom> findAllByOrderByCreatedAtDesc();

    List<Classroom> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    Optional<Classroom> findByCode(String code);

    boolean existsByCode(String code);

    @Query("select c from Classroom c where c.id in "
            + "(select m.classId from ClassMember m where m.studentId = :studentId) order by c.name")
    List<Classroom> findJoinedBy(@Param("studentId") Long studentId);
}
