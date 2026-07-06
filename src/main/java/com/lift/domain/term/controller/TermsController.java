package com.lift.domain.term.controller;

import com.lift.domain.term.dto.response.TermsResDTO;
import com.lift.domain.term.enumtype.TermType;
import com.lift.domain.term.service.TermsService;
import com.lift.global.apiPayload.ApiResponse;
import com.lift.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    @GetMapping("/{type}")
    public ApiResponse<TermsResDTO> getTerms(
            @PathVariable String type
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, termsService.getTerms(TermType.from(type)));
    }
}
