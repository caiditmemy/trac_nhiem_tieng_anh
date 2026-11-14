package vn.edu.qnu.st.nambede.exercise.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.qnu.st.nambede.exercise.security.AuthUser;
import vn.edu.qnu.st.nambede.exercise.service.SubmissionService;
import vn.edu.qnu.st.nambede.exercise.web.dto.AnswerRequest;
import vn.edu.qnu.st.nambede.exercise.web.dto.SubmissionView;

@Tag(name = "Bài làm", description = "Làm bài, lưu đáp án, nộp và chấm điểm")
@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Operation(summary = "Một bài làm (sinh viên làm bài, giảng viên giao bài, ADMIN)")
    @GetMapping("/{id}")
    public SubmissionView get(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller) {
        return submissionService.get(id, caller);
    }

    /** Saves the answer chosen for one question (called each time the student picks an option). */
    @Operation(summary = "Lưu đáp án cho một câu, gọi mỗi lần sinh viên chọn (STUDENT)")
    @PostMapping("/{id}/answer")
    public SubmissionView answer(@PathVariable Long id, @Valid @RequestBody AnswerRequest request,
            @AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return submissionService.answer(id, request.exerciseQuestionId(), request.chosenOption(), caller, authorization);
    }

    @Operation(summary = "Nộp và chấm điểm thang 10; sau khi nộp, Learning Service cộng điểm và chuỗi ngày học (STUDENT)")
    @PostMapping("/{id}/submit")
    public SubmissionView submit(@PathVariable Long id, @AuthenticationPrincipal AuthUser caller,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return submissionService.submit(id, caller, authorization);
    }
}
