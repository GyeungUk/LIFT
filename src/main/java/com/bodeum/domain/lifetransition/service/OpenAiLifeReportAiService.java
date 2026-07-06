package com.bodeum.domain.lifetransition.service;

import com.bodeum.domain.lifetransition.model.LifeReport;
import com.bodeum.domain.lifetransition.model.ReportItem;
import com.bodeum.domain.lifetransition.model.RequiredDocument;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 결제 완료 리포트를 근거로 답변하는 실제 OpenAI 연동 AI 챗봇.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bodeum.openai", name = "enabled", havingValue = "true")
public class OpenAiLifeReportAiService implements LifeReportAiService {

    private final RestClient.Builder restClientBuilder;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateAnswer(LifeReport report, String userQuestion) {
        if (!properties.isAvailable()) {
            throw new ProjectException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            Map<String, Object> response = restClientBuilder.build()
                    .post()
                    .uri(properties.getBaseUrl().replaceAll("/+$", "") + "/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .body(payload(report, userQuestion))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            String answer = extractText(response);
            if (!StringUtils.hasText(answer)) {
                throw new ProjectException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
            }
            return answer.strip();
        } catch (RestClientException | JsonProcessingException | IllegalArgumentException e) {
            log.warn("OpenAI report chat failed.", e);
            throw new ProjectException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> payload(LifeReport report, String userQuestion) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", properties.getModel());
        payload.put("reasoning", Map.of("effort", "low"));
        payload.put("input", List.of(
                Map.of(
                        "role", "system",
                        "content", """
                                너는 LIFT의 한국 행정 로드맵 AI 상담사다.
                                반드시 제공된 리포트 JSON 안의 내용만 근거로 답변한다.
                                자격을 확정하지 말고, '가능성이 높음/확인 필요'처럼 리포트의 판단 수준을 유지한다.
                                법률·세무·노무 최종 판단은 관할 기관 확인이 필요하다고 안내한다.
                                사용자가 바로 행동할 수 있도록 3~6문장 또는 짧은 bullet로 한국어 답변한다.
                                모르는 내용, 리포트에 없는 금액·기한·조건은 만들지 않는다.
                                """
                ),
                Map.of(
                        "role", "user",
                        "content", objectMapper.writeValueAsString(Map.of(
                                "question", userQuestion.strip(),
                                "report", reportContext(report)
                        ))
                )
        ));
        return payload;
    }

    private Map<String, Object> reportContext(LifeReport report) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("summaryTitle", report.getSummaryTitle());
        context.put("summaryMessage", report.getSummaryMessage());
        context.put("totalPriorityScore", report.getTotalPriorityScore());
        context.put("items", report.getItems().stream().map(this::itemContext).toList());
        return context;
    }

    private Map<String, Object> itemContext(ReportItem item) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("procedureType", item.getProcedureType().name());
        context.put("procedureName", item.getProcedureType().getDisplayName());
        context.put("title", item.getTitle());
        context.put("eligibilityLevel", item.getEligibilityLevel().name());
        context.put("priorityLevel", item.getPriorityLevel().name());
        context.put("reason", item.getReason());
        context.put("deadlineText", item.getDeadlineText());
        context.put("officialUrl", item.getOfficialUrl());
        context.put("requiredDocuments", item.getRequiredDocuments().stream().map(this::documentContext).toList());
        return context;
    }

    private Map<String, Object> documentContext(RequiredDocument document) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("documentName", document.getDocumentName());
        context.put("description", document.getDescription());
        context.put("issuer", document.getIssuer());
        context.put("required", document.isRequired());
        return context;
    }

    private String extractText(Object value) {
        if (value instanceof Map<?, ?> map) {
            Object outputText = map.get("output_text");
            if (outputText instanceof String text && StringUtils.hasText(text)) {
                return text;
            }
            Object type = map.get("type");
            Object text = map.get("text");
            if ("output_text".equals(type) && text instanceof String s && StringUtils.hasText(s)) {
                return s;
            }
            for (Object child : map.values()) {
                String found = extractText(child);
                if (StringUtils.hasText(found)) {
                    return found;
                }
            }
        }
        if (value instanceof List<?> list) {
            for (Object child : list) {
                String found = extractText(child);
                if (StringUtils.hasText(found)) {
                    return found;
                }
            }
        }
        return null;
    }
}
