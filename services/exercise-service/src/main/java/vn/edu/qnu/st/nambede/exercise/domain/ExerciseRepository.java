package vn.edu.qnu.st.nambede.exercise.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByClassIdInOrderByDueAtDesc(Collection<Long> classIds);

    /** Rows of [exerciseId, number of questions]. */
    @Query("select q.exercise.id, count(q.id) from ExerciseQuestion q where q.exercise.id in :ids group by q.exercise.id")
    List<Object[]> countQuestions(@Param("ids") Collection<Long> exerciseIds);
}
