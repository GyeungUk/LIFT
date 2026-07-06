package com.lift.domain.lifetransition.controller;

import com.lift.domain.lifetransition.dto.request.LifeAssessmentCreateReqDTO;
import com.lift.domain.lifetransition.dto.response.LifeAssessmentResDTO;
import com.lift.domain.lifetransition.dto.response.ReportPreviewResDTO;
import com.lift.domain.lifetransition.service.LifeAssessmentService;
import com.lift.global.apiPayload.ApiResponse;
import com.lift.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 생애 전환 진단 API.
 */
@RestController
@RequestMapping("/api/life/assessments")
@RequiredArgsConstructor
public class LifeAssessmentController {

    private final LifeAssessmentService lifeAssessmentService;

    @PostMapping
    public ApiResponse<LifeAssessmentResDTO> createAssessment(
            Authentication authentication,
            @Valid @RequestBody LifeAssessmentCreateReqDTO request
    ) {
        return ApiResponse.of(GeneralSuccessCode.CREATED, lifeAssessmentService.createAssessment(authentication, request));
    }

    @PostMapping("/{assessmentId}/analyze")
    public ApiResponse<ReportPreviewResDTO> analyze(
            Authentication authentication,
            @PathVariable Long assessmentId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, lifeAssessmentService.analyze(authentication, assessmentId));
    }
}
