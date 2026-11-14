package vn.edu.qnu.st.nambede.exercise.web;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.qnu.st.nambede.exercise.security.AuthUser;
import vn.edu.qnu.st.nambede.exercise.service.ExerciseService;
import vn.edu.qnu.st.nambede.exercise.web.dto.CreateExerciseRequest;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseDetail;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseSummary;

/** Role rules are in SecurityConfig; rules that depend on the exercise (who assigned it) are in the services. */
@Tag(name = "Bài tập", description = "Giao bài (sao chép câu hỏi), danh sách, chi tiết")
@RestController
@RequestMapping("/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @Operation(summary = "Giao bài cho lớp: chép từng câu hỏi từ Learning Service vào bài (TEACHER của lớp)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseSummary create(@Valid @RequestBody CreateExerciseRequest request,
            @AuthenticationPrincipal AuthUser caller, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return exerciseService.create(request, caller, authorization);
    }

    @Operation(summary = "Danh sách bài tập, lọc theo lớp (giảng viên: lớp mình dạy, sinh viên: lớp đã vào, admin: tất cả)")
    @GetMapping
    public List<ExerciseSummary> list(@RequestParam(required = false) Long classId,
            @AuthenticationPrincipal AuthUser caller, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return exerciseService.list(classId, caller, authorization);
    }

    @Operation(summary = "Một bài tập cùng các câu hỏi và đáp án đã chép (giảng viên giao bài, ADMIN)")
    @GetMapping("/{id}")
    public ExerciseDetail get(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return exerciseService.get(id, caller, authorization);
    }

    @Operation(summary = "Xóa bài tập (giảng viên giao bài, ADMIN)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller) {
        exerciseService.delete(id, caller);
    }
}
