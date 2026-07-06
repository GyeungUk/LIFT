package com.lift.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lift.global.apiPayload.exception.ProjectException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class SocialOAuthClientTest {

    private final SocialOAuthClient socialOAuthClient = new SocialOAuthClient(
            new OAuthProperties(),
            RestClient.builder()
    );

    @Test
    void parseKakaoProfileIgnoresMalformedNestedObjects() {
        SocialUserProfile profile = ReflectionTestUtils.invokeMethod(
                socialOAuthClient,
                "parseKakaoProfile",
                Map.of(
                        "id", 12345,
                        "kakao_account", "malformed",
                        "properties", 123
                )
        );

        assertThat(profile.providerUserId()).isEqualTo("12345");
        assertThat(profile.email()).isNull();
        assertThat(profile.nickname()).isEqualTo("살림 챙김러");
    }

    @Test
    void parseNaverProfileRejectsMalformedResponse() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                socialOAuthClient,
                "parseNaverProfile",
                Map.of("response", "malformed")
        )).isInstanceOf(ProjectException.class);
    }
}
