package com.bodeum.domain.lifetransition.service;

import com.bodeum.domain.lifetransition.dto.response.PublicBenefitResDTO;
import com.bodeum.domain.lifetransition.dto.response.RequiredDocumentResDTO;
import com.bodeum.domain.lifetransition.enumtype.HouseholdType;
import com.bodeum.domain.lifetransition.enumtype.LifeEventType;
import com.bodeum.domain.lifetransition.enumtype.PublicBenefitFitLevel;
import com.bodeum.domain.lifetransition.enumtype.PublicBenefitPriorityGroup;
import com.bodeum.domain.lifetransition.model.LifeAssessment;
import com.bodeum.domain.lifetransition.model.LifeReport;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 공공데이터포털 행정안전부_대한민국 공공서비스(혜택) 정보 연동.
 *
 * 기존 룰 엔진은 설명 가능한 핵심 절차를 만들고, 이 서비스는 정부24/보조금24 공개
 * 카탈로그에서 사용자의 상황과 가까운 추가 혜택 후보를 보강한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Gov24PublicBenefitService {

    private static final String SOURCE_LABEL = "공공데이터포털 · 정부24 공공서비스";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final RestClient.Builder restClientBuilder;
    private final Gov24PublicServiceProperties properties;
    private final PublicBenefitRecommendationService recommendationService;

    private volatile CachedRows cachedRows;
    private volatile CachedRows cachedDetailRows;

    public List<PublicBenefitResDTO> findBenefits(LifeReport report) {
        if (!properties.isAvailable()) {
            return List.of();
        }

        RestClient restClient = restClientBuilder.build();
        Map<String, Map<String, Object>> detailByServiceId = detailByServiceId(restClient);
        Map<String, BenefitCandidate> candidates = new LinkedHashMap<>();

        List<String> keywords = buildKeywords(report).stream()
                .limit(Math.max(1, properties.getMaxKeywords()))
                .toList();

        for (Map<String, Object> row : fetchServicePages(restClient)) {
            String matchedKeyword = findMatchedKeyword(row, keywords);
            if (!StringUtils.hasText(matchedKeyword)) {
                continue;
            }

            String sourceId = value(row, "서비스ID", "서비스아이디", "서비스관리번호", "서비스코드", "id", "serviceId");
            BenefitCandidate candidate = toCandidate(report, row, detailByServiceId.get(sourceId), matchedKeyword);
            if (candidate == null) {
                continue;
            }
            candidates.merge(
                    dedupeKey(candidate.benefit()),
                    candidate,
                    (left, right) -> left.score() >= right.score() ? left : right
            );
        }

        List<PublicBenefitResDTO> preRanked = candidates.values().stream()
                .sorted(Comparator.comparingInt(BenefitCandidate::score).reversed())
                .limit(Math.max(15, properties.getMaxResults()))
                .map(BenefitCandidate::benefit)
                .map(benefit -> trimLongFields(benefit, 1200))
                .toList();

        return recommendationService.recommend(report, preRanked).stream()
                .limit(Math.max(1, properties.getMaxResults()))
                .toList();
    }

    private Map<String, Map<String, Object>> detailByServiceId(RestClient restClient) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> detail : fetchServiceDetailPages(restClient)) {
            String sourceId = value(detail, "서비스ID", "서비스아이디", "서비스관리번호", "서비스코드", "id", "serviceId");
            if (StringUtils.hasText(sourceId)) {
                result.put(sourceId, detail);
            }
        }
        return result;
    }

    private List<Map<String, Object>> fetchServicePages(RestClient restClient) {
        CachedRows cached = cachedRows;
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.rows();
        }

        List<Map<String, Object>> rows = fetchPages(restClient, "serviceList");
        if (!rows.isEmpty()) {
            cachedRows = new CachedRows(List.copyOf(rows), now.plus(CACHE_TTL));
        }
        return rows;
    }

    private List<Map<String, Object>> fetchServiceDetailPages(RestClient restClient) {
        CachedRows cached = cachedDetailRows;
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.rows();
        }

        List<Map<String, Object>> rows = fetchPages(restClient, "serviceDetail");
        if (!rows.isEmpty()) {
            cachedDetailRows = new CachedRows(List.copyOf(rows), now.plus(CACHE_TTL));
        }
        return rows;
    }

    private List<Map<String, Object>> fetchPages(RestClient restClient, String endpoint) {
        int maxPages = Math.max(1, properties.getMaxPages());
        int perPage = Math.max(1, properties.getPerPage());
        List<Map<String, Object>> rows = new ArrayList<>();

        for (int page = 1; page <= maxPages; page++) {
            PageRows pageRows = fetchServicePage(restClient, endpoint, page, perPage);
            rows.addAll(pageRows.rows());
            if (pageRows.currentCount() < perPage || pageRows.rows().isEmpty()) {
                break;
            }
        }
        return rows;
    }

    private PageRows fetchServicePage(RestClient restClient, String endpoint, int page, int perPage) {
        URI uri = UriComponentsBuilder
                .fromUriString(StringUtils.trimTrailingCharacter(properties.getBaseUrl(), '/') + "/" + endpoint)
                .queryParam("page", page)
                .queryParam("perPage", perPage)
                .queryParam("returnType", "JSON")
                .queryParam("serviceKey", properties.getServiceKey())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        try {
            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null) {
                return new PageRows(List.of(), 0);
            }
            Object code = response.get("code");
            if (code != null && !String.valueOf(code).equals("0")) {
                log.warn("Gov24 public benefit API returned code={} message={}", code, response.get("msg"));
                return new PageRows(List.of(), 0);
            }
            Object data = response.get("data");
            if (!(data instanceof List<?> rows)) {
                return new PageRows(List.of(), number(response.get("currentCount")));
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object row : rows) {
                if (row instanceof Map<?, ?> map) {
                    result.add(toStringObjectMap(map));
                }
            }
            return new PageRows(result, number(response.get("currentCount")));
        } catch (RestClientException e) {
            log.warn("Gov24 public benefit API lookup failed. endpoint={}, page={}", endpoint, page, e);
            return new PageRows(List.of(), 0);
        }
    }

    private BenefitCandidate toCandidate(
            LifeReport report,
            Map<String, Object> row,
            Map<String, Object> detail,
            String keyword
    ) {
        String title = value(row, "서비스명", "서비스명칭", "서비스", "serviceName", "srvNm");
        if (!StringUtils.hasText(title)) {
            return null;
        }

        Map<String, Object> source = detail == null ? row : merge(row, detail);
        LifeAssessment assessment = report.getAssessment();

        String summary = value(source, "서비스목적요약", "서비스목적", "지원내용", "서비스내용", "서비스개요", "summary");
        String provider = value(source, "소관기관명", "제공기관명", "기관명", "부서명", "provider");
        String category = value(source, "서비스분야", "지원유형", "생애주기", "대상자", "분야", "category");
        String applicationUrl = value(source, "온라인신청사이트URL", "온라인신청URL", "신청URL", "상세조회URL", "url", "URL");
        String sourceId = value(source, "서비스ID", "서비스아이디", "서비스관리번호", "서비스코드", "id", "serviceId");
        String supportTarget = value(source, "지원대상", "대상자");
        String selectionCriteria = value(source, "선정기준");
        String supportContent = value(source, "지원내용");
        String applicationMethod = value(source, "신청방법");
        String applicationDeadline = value(source, "신청기한");
        String contact = value(source, "전화문의", "문의처");
        List<RequiredDocumentResDTO> documents = extractDocuments(source);
        String reason = buildReason(assessment, title, summary, provider, keyword);
        int score = score(assessment, title, summary, provider, supportTarget, selectionCriteria, supportContent, applicationDeadline, keyword, applicationUrl);
        PublicBenefitFitLevel fitLevel = fitLevel(score, supportTarget, selectionCriteria);
        PublicBenefitPriorityGroup priorityGroup = priorityGroup(assessment, title, supportContent, applicationDeadline, fitLevel);
        List<String> missingInputs = missingInputs(assessment, supportTarget, selectionCriteria);

        PublicBenefitResDTO benefit = new PublicBenefitResDTO(
                title,
                blankToNull(summary),
                blankToNull(provider),
                blankToNull(category),
                blankToNull(applicationUrl),
                blankToNull(sourceId),
                keyword,
                reason,
                SOURCE_LABEL,
                fitLevel,
                priorityGroup,
                blankToNull(supportTarget),
                blankToNull(selectionCriteria),
                blankToNull(supportContent),
                blankToNull(applicationMethod),
                blankToNull(applicationDeadline),
                blankToNull(contact),
                documents,
                missingInputs,
                null,
                score
        );
        return new BenefitCandidate(benefit, score);
    }

    private List<String> buildKeywords(LifeReport report) {
        Set<String> keywords = new LinkedHashSet<>();
        LifeAssessment assessment = report.getAssessment();

        if (assessment.getEventType() == LifeEventType.RETIREMENT) {
            keywords.addAll(List.of("실업급여", "구직급여", "퇴직", "재취업", "직업훈련", "생활안정"));
        } else if (assessment.getEventType() == LifeEventType.UNEMPLOYMENT) {
            keywords.addAll(List.of("실업", "구직", "국민취업지원", "직업훈련", "긴급복지", "생활안정"));
        } else {
            keywords.addAll(List.of("이직", "재취업", "취업", "직업훈련", "내일배움", "고용"));
        }

        report.getItems().forEach(item -> {
            switch (item.getProcedureType()) {
                case UNEMPLOYMENT_BENEFIT -> keywords.addAll(List.of("구직", "고용보험"));
                case HEALTH_INSURANCE_CONTINUATION -> keywords.addAll(List.of("건강보험", "보험료"));
                case NATIONAL_PENSION_EXCEPTION -> keywords.addAll(List.of("국민연금", "납부예외"));
                case TAX_CHECK -> keywords.addAll(List.of("근로장려금", "소득세"));
                case SEVERANCE_PAY -> keywords.addAll(List.of("체불", "임금"));
            }
        });

        if (StringUtils.hasText(assessment.getRegionSigungu())) {
            keywords.add(assessment.getRegionSigungu());
        }
        if (StringUtils.hasText(assessment.getRegionSido())) {
            keywords.add(assessment.getRegionSido());
        }
        return List.copyOf(keywords);
    }

    private String findMatchedKeyword(Map<String, Object> row, List<String> keywords) {
        String merged = joinForSearch(
                value(row, "서비스명", "서비스명칭", "서비스", "serviceName", "srvNm"),
                value(row, "서비스목적요약", "서비스목적", "지원내용", "서비스내용", "서비스개요", "summary"),
                value(row, "지원내용", "지원대상", "선정기준"),
                value(row, "소관기관명", "제공기관명", "기관명", "부서명", "provider"),
                value(row, "서비스분야", "지원유형", "생애주기", "대상자", "분야", "category")
        );
        for (String keyword : keywords) {
            if (contains(merged, keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private String buildReason(
            LifeAssessment assessment,
            String title,
            String summary,
            String provider,
            String keyword
    ) {
        String merged = joinForSearch(title, summary, provider);
        if (contains(merged, assessment.getRegionSigungu())) {
            return assessment.getRegionSigungu() + " 지역 조건이 걸린 공공서비스라 신청 가능성을 확인해 볼 만해요.";
        }
        if (contains(merged, assessment.getRegionSido())) {
            return assessment.getRegionSido() + " 지역 공공서비스라 거주지 기준 확인이 필요해요.";
        }
        return "'" + keyword + "' 상황과 연결되는 공공서비스예요. 소득, 고용보험, 거주지 조건을 확인해 보세요.";
    }

    private int score(
            LifeAssessment assessment,
            String title,
            String summary,
            String provider,
            String supportTarget,
            String selectionCriteria,
            String supportContent,
            String applicationDeadline,
            String matchedKeyword,
            String applicationUrl
    ) {
        int score = 0;
        String merged = joinForSearch(title, summary, provider, supportTarget, selectionCriteria, supportContent);

        if (contains(title, matchedKeyword)) {
            score += 40;
        }
        if (contains(merged, matchedKeyword)) {
            score += 20;
        }
        if (contains(merged, assessment.getRegionSigungu())) {
            score += 24;
        } else if (contains(merged, assessment.getRegionSido())) {
            score += 14;
        }
        if (StringUtils.hasText(applicationUrl)) {
            score += 6;
        }
        if (containsAny(merged, "구직", "취업", "고용", "훈련", "실업", "생계", "생활안정", "긴급")) {
            score += 10;
        }
        if (assessment.getCurrentIncomeStatus() != null && containsAny(merged, "저소득", "소득", "무소득", "생계")) {
            score += 8;
        }
        if (assessment.getHouseholdType() == HouseholdType.WITH_CHILDREN || Boolean.TRUE.equals(assessment.getHasDependentChildren())) {
            if (containsAny(merged, "자녀", "아동", "양육", "가구")) {
                score += 10;
            }
        }
        if (Boolean.TRUE.equals(assessment.getBasicLivelihoodRecipient()) && containsAny(merged, "기초생활", "수급자")) {
            score += 14;
        }
        if (Boolean.TRUE.equals(assessment.getNearPoverty()) && containsAny(merged, "차상위", "저소득")) {
            score += 12;
        }
        if (Boolean.TRUE.equals(assessment.getSingleParent()) && contains(merged, "한부모")) {
            score += 12;
        }
        if (Boolean.TRUE.equals(assessment.getDisabledPerson()) && containsAny(merged, "장애", "장애인")) {
            score += 12;
        }
        if (assessment.getHousingType() != null && containsAny(merged, "월세", "전세", "주거", "임대")) {
            score += 8;
        }
        if (containsAny(applicationDeadline, "상시", "수시")) {
            score += 4;
        } else if (StringUtils.hasText(applicationDeadline)) {
            score += 8;
        }
        return score;
    }

    private PublicBenefitFitLevel fitLevel(int score, String supportTarget, String selectionCriteria) {
        String merged = joinForSearch(supportTarget, selectionCriteria);
        if (containsAny(merged, "제외", "해당되지 않는", "불가") && score < 70) {
            return PublicBenefitFitLevel.NEEDS_CHECK;
        }
        if (score >= 78) {
            return PublicBenefitFitLevel.HIGH;
        }
        if (score >= 42) {
            return PublicBenefitFitLevel.NEEDS_CHECK;
        }
        return PublicBenefitFitLevel.LOW;
    }

    private PublicBenefitPriorityGroup priorityGroup(
            LifeAssessment assessment,
            String title,
            String supportContent,
            String applicationDeadline,
            PublicBenefitFitLevel fitLevel
    ) {
        String merged = joinForSearch(title, supportContent);
        if (fitLevel == PublicBenefitFitLevel.LOW) {
            return PublicBenefitPriorityGroup.LOW;
        }
        if (StringUtils.hasText(applicationDeadline) && !containsAny(applicationDeadline, "상시", "수시")) {
            return PublicBenefitPriorityGroup.DEADLINE;
        }
        if (containsAny(merged, "현금", "지원금", "급여", "장려금", "대출", "보증", "감면")) {
            return PublicBenefitPriorityGroup.TOP_MONEY;
        }
        if (contains(merged, assessment.getRegionSigungu()) || contains(merged, assessment.getRegionSido())) {
            return PublicBenefitPriorityGroup.LOCAL;
        }
        return PublicBenefitPriorityGroup.NEEDS_INFO;
    }

    private List<String> missingInputs(LifeAssessment assessment, String supportTarget, String selectionCriteria) {
        String merged = joinForSearch(supportTarget, selectionCriteria);
        List<String> missing = new ArrayList<>();
        if (containsAny(merged, "연소득", "총소득", "소득요건", "소득기준") && assessment.getAnnualIncomeRange() == null) {
            missing.add("연소득 범위");
        }
        if (containsAny(merged, "재산", "자산") && assessment.getAssetRange() == null) {
            missing.add("재산 범위");
        }
        if (containsAny(merged, "가구", "배우자", "부양", "자녀") && assessment.getHouseholdType() == null) {
            missing.add("가구 형태");
        }
        if (containsAny(merged, "월세", "전세", "주거", "임대") && assessment.getHousingType() == null) {
            missing.add("주거 형태");
        }
        return missing.stream().distinct().limit(4).toList();
    }

    private List<RequiredDocumentResDTO> extractDocuments(Map<String, Object> source) {
        List<RequiredDocumentResDTO> documents = new ArrayList<>();
        addDocuments(documents, value(source, "구비서류"), "신청자 준비", true);
        addDocuments(documents, value(source, "본인확인필요구비서류"), "본인 확인", true);
        addDocuments(documents, value(source, "공무원확인구비서류"), "기관 확인", false);
        return documents.stream()
                .filter(doc -> StringUtils.hasText(doc.documentName()) && !"해당없음".equals(doc.documentName()))
                .limit(8)
                .toList();
    }

    private void addDocuments(List<RequiredDocumentResDTO> documents, String raw, String issuer, boolean required) {
        if (!StringUtils.hasText(raw) || raw.contains("해당없음")) {
            return;
        }
        String normalized = raw.replace("||", "\n");
        for (String line : normalized.split("\\r?\\n")) {
            String name = line.replaceFirst("^[\\-ㆍ○\\*\\s]+", "").trim();
            if (StringUtils.hasText(name) && name.length() >= 2) {
                documents.add(new RequiredDocumentResDTO(name, null, issuer, required));
            }
        }
    }

    private PublicBenefitResDTO trimLongFields(PublicBenefitResDTO benefit, int maxLength) {
        return new PublicBenefitResDTO(
                benefit.title(),
                shorten(benefit.summary(), maxLength),
                benefit.provider(),
                benefit.category(),
                benefit.applicationUrl(),
                benefit.sourceId(),
                benefit.matchedKeyword(),
                benefit.reason(),
                benefit.sourceLabel(),
                benefit.fitLevel(),
                benefit.priorityGroup(),
                shorten(benefit.supportTarget(), maxLength),
                shorten(benefit.selectionCriteria(), maxLength),
                shorten(benefit.supportContent(), maxLength),
                benefit.applicationMethod(),
                benefit.applicationDeadline(),
                benefit.contact(),
                benefit.requiredDocuments() == null ? List.of() : benefit.requiredDocuments(),
                benefit.missingInputs() == null ? List.of() : benefit.missingInputs(),
                benefit.aiSummary(),
                benefit.relevanceScore()
        );
    }

    private String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }

    private String value(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object exact = row.get(key);
            if (exact != null && StringUtils.hasText(String.valueOf(exact))) {
                return String.valueOf(exact).trim();
            }
        }

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String normalizedKey = normalize(entry.getKey());
            for (String key : keys) {
                if (normalizedKey.equals(normalize(key))
                        && entry.getValue() != null
                        && StringUtils.hasText(String.valueOf(entry.getValue()))) {
                    return String.valueOf(entry.getValue()).trim();
                }
            }
        }
        return null;
    }

    private Map<String, Object> toStringObjectMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private Map<String, Object> merge(Map<String, Object> base, Map<String, Object> detail) {
        Map<String, Object> result = new LinkedHashMap<>(base);
        detail.forEach((key, value) -> {
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                result.put(key, value);
            }
        });
        return result;
    }

    private String dedupeKey(PublicBenefitResDTO benefit) {
        if (StringUtils.hasText(benefit.sourceId())) {
            return normalize(benefit.sourceId());
        }
        return normalize(nullToBlank(benefit.title()) + "::" + nullToBlank(benefit.provider()));
    }

    private boolean containsAny(String target, String... needles) {
        for (String needle : needles) {
            if (contains(target, needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String target, String needle) {
        return StringUtils.hasText(target)
                && StringUtils.hasText(needle)
                && target.contains(needle);
    }

    private String joinForSearch(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                builder.append(value).append(' ');
            }
        }
        return builder.toString();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return nullToBlank(value)
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record PageRows(List<Map<String, Object>> rows, int currentCount) {
    }

    private record CachedRows(List<Map<String, Object>> rows, Instant expiresAt) {
    }

    private record BenefitCandidate(PublicBenefitResDTO benefit, int score) {
    }
}
