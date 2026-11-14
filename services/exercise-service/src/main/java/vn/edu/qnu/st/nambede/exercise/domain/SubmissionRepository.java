package vn.edu.qnu.st.nambede.exercise.domain;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByExerciseIdAndStudentId(Long exerciseId, Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Submission s where s.id = :id")
    Optional<Submission> findByIdForUpdate(@Param("id") Long id);

    List<Submission> findByExerciseId(Long exerciseId);

    List<Submission> findByStudentIdAndExerciseIdIn(Long studentId, Collection<Long> exerciseIds);

    /** Rows of [exerciseId, number of submitted attempts]. */
    @Query("select s.exercise.id, count(s.id) from Submission s where s.exercise.id in :ids and s.submittedAt is not null group by s.exercise.id")
    List<Object[]> countSubmitted(@Param("ids") Collection<Long> exerciseIds);
}
