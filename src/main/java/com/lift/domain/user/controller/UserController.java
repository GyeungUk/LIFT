package com.lift.domain.user.controller;

import com.lift.domain.onboarding.dto.response.OnboardingStatusResDTO;
import com.lift.domain.onboarding.service.OnboardingService;
import com.lift.domain.user.dto.request.UserAgreementReqDTO;
import com.lift.domain.user.dto.request.UserProfileUpdateReqDTO;
import com.lift.domain.user.dto.response.UserAgreementResDTO;
import com.lift.domain.user.dto.response.UserProfileResDTO;
import com.lift.domain.user.dto.response.UserSummaryResDTO;
import com.lift.domain.user.service.UserService;
import com.lift.global.apiPayload.ApiResponse;
import com.lift.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OnboardingService onboardingService;

    @GetMapping("/summary")
    public ApiResponse<UserSummaryResDTO> getSummary(Authentication authentication) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.getSummary(authentication));
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileResDTO> getProfile(Authentication authentication) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.getProfile(authentication));
    }

    @PatchMapping("/profile")
    public ApiResponse<UserProfileResDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateReqDTO request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.updateProfile(authentication, request));
    }

    @GetMapping("/onboarding-status")
    public ApiResponse<OnboardingStatusResDTO> getOnboardingStatus(Authentication authentication) {
        return ApiResponse.of(GeneralSuccessCode.OK, onboardingService.getStatus(authentication));
    }

    @PostMapping("/agreement")
    public ApiResponse<UserAgreementResDTO> agreeTerms(
            Authentication authentication,
            @Valid @RequestBody UserAgreementReqDTO request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, userService.agreeTerms(authentication, request));
    }

    @DeleteMapping
    public ApiResponse<Void> withdraw(Authentication authentication) {
        userService.withdraw(authentication);

        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }
}
