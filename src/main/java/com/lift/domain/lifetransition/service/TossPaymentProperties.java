package com.lift.domain.lifetransition.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lift.payments.toss")
public class TossPaymentProperties {

    /**
     * 데모 안정성을 위해 기본값은 꺼져 있다. 테스트 키를 넣고 명시적으로 켠 경우에만 호출한다.
     */
    private boolean enabled = false;

    private String secretKey;

    private String baseUrl = "https://api.tosspayments.com/v1";

    public boolean isAvailable() {
        return enabled && StringUtils.hasText(secretKey);
    }
}
