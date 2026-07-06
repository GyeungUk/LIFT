package com.lift.domain.lifetransition.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lift.public-data.gov24")
public class Gov24PublicServiceProperties {

    /**
     * 공공데이터포털 활용신청 후 받은 서비스키가 있을 때만 조회한다.
     */
    private boolean enabled = false;

    private String serviceKey;

    private String baseUrl = "https://api.odcloud.kr/api/gov24/v3";

    private int perPage = 1000;

    private int maxPages = 2;

    private int maxKeywords = 5;

    private int maxResults = 6;

    public boolean isAvailable() {
        return enabled && serviceKey != null && !serviceKey.isBlank();
    }
}
