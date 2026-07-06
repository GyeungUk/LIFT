package com.lift.domain.onboarding.dto.response;

import com.lift.domain.auth.enumtype.AuthNextStep;
import com.lift.domain.user.model.UserAccount;

public record OnboardingStatusResDTO(
        boolean childProfileRegistered,
        boolean interestRegionRegistered,
        boolean guardianProfileRegistered,
        boolean onboardingCompleted,
        AuthNextStep nextStep
) {

    public static OnboardingStatusResDTO from(UserAccount userAccount) {
        boolean onboardingCompleted = userAccount.isOnboardingCompleted();
        return new OnboardingStatusResDTO(
                userAccount.isChildProfileRegistered(),
                userAccount.isInterestRegionRegistered(),
                userAccount.isGuardianProfileRegistered(),
                onboardingCompleted,
                onboardingCompleted ? AuthNextStep.HOME : AuthNextStep.ONBOARDING
        );
    }
}
