package com.aiplatform.ai.controller;

import com.aiplatform.ai.dto.CodeReviewRequest;
import com.aiplatform.ai.dto.CodeReviewResponse;
import com.aiplatform.ai.service.CodeReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final CodeReviewService codeReviewService;

    public AiController(CodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }

    @PostMapping("/code-review")
    public CodeReviewResponse reviewCode(@Valid @RequestBody CodeReviewRequest request) {
        return codeReviewService.review(request);
    }
}
