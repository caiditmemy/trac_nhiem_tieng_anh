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
import vn.edu.qnu.st.nambede.exercise.service.SubmissionService;
import vn.edu.qnu.st.nambede.exercise.web.dto.CreateExerciseRequest;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseResults;
import vn.edu.qnu.st.nambede.exercise.web.dto.ExerciseSummary;
import vn.edu.qnu.st.nambede.exercise.web.dto.SubmissionView;

/** Role rules are in SecurityConfig; rules that depend on the exercise (who assigned it) are in the services. */
@Tag(name = "Bài tập", description = "Giao bài (sao chép câu hỏi), danh sách, bảng điểm")
@RestController
@RequestMapping("/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final SubmissionService submissionService;

    public ExerciseController(ExerciseService exerciseService, SubmissionService submissionService) {
        this.exerciseService = exerciseService;
        this.submissionService = submissionService;
    }

    @Operation(summary = "Giao bài cho lớp: chép từng câu hỏi từ Learning Service vào bài (TEACHER của lớp)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseSummary create(@Valid @RequestBody CreateExerciseRequest request,
            @AuthenticationPrincipal AuthUser caller, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return exerciseService.create(request, caller, authorization);
    }

    @Operation(summary = "Danh sách bài tập, lọc theo lớp; sinh viên có thêm trạng thái và điểm của mình")
    @GetMapping
    public List<ExerciseSummary> list(@RequestParam(required = false) Long classId,
            @AuthenticationPrincipal AuthUser caller, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return exerciseService.list(classId, caller, authorization);
    }

    @Operation(summary = "Một bài tập")
    @GetMapping("/{id}")
    public ExerciseSummary get(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return exerciseService.get(id, caller, authorization);
    }

    @Operation(summary = "Bảng điểm của lớp và tỉ lệ đúng từng câu (giảng viên giao bài, ADMIN)")
    @GetMapping("/{id}/results")
    public ExerciseResults results(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return exerciseService.results(id, caller, authorization);
    }

    @Operation(summary = "Xóa bài tập cùng các bài làm (giảng viên giao bài, ADMIN)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller) {
        exerciseService.delete(id, caller);
    }

    @Operation(summary = "Mở bài làm, tạo ở lần đầu; hỏi Class Service xem sinh viên có trong lớp (STUDENT)")
    @PostMapping("/{id}/start")
    public SubmissionView start(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return submissionService.start(id, caller, authorization);
    }
}
