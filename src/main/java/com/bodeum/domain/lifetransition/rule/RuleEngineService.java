package com.bodeum.domain.lifetransition.rule;

import com.bodeum.domain.lifetransition.enumtype.EligibilityLevel;
import com.bodeum.domain.lifetransition.enumtype.LifeEventType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 룰 엔진.
 *
 * <p>AI가 자격을 판단하지 않는다. 등록된 {@link LifeTransitionRule} 들을 사용자 입력에 대해
 * 결정적으로 평가하고, 우선순위/신청 가능성 기준으로 정렬한 뒤 요약과 총점을 계산한다.
 * 룰 목록은 Spring이 주입하므로, 새 규칙 추가 또는 rules.json/DB 로더로의 교체가 이 서비스를
 * 수정하지 않고도 가능하다.
 */
@Service
public class RuleEngineService {

    private final List<LifeTransitionRule> rules;

    public RuleEngineService(List<LifeTransitionRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public RuleEngineResult analyze(RuleContext context) {
        List<RuleItemResult> collected = new ArrayList<>();
        for (LifeTransitionRule rule : rules) {
            collected.addAll(rule.evaluate(context));
        }

        List<RuleItemResult> sorted = collected.stream()
                .sorted(itemOrder())
                .toList();

        int totalPriorityScore = sorted.stream()
                .mapToInt(this::scoreOf)
                .sum();

        return new RuleEngineResult(
                buildSummaryTitle(context, sorted),
                buildSummaryMessage(context, sorted),
                totalPriorityScore,
                sorted
        );
    }

    /**
     * 우선순위(HIGH 먼저) → 신청 가능성(HIGH 먼저) 순으로 정렬.
     */
    private Comparator<RuleItemResult> itemOrder() {
        return Comparator
                .comparingInt((RuleItemResult item) -> item.priorityLevel().getWeight()).reversed()
                .thenComparing(item -> eligibilityRank(item.eligibilityLevel()));
    }

    private int eligibilityRank(EligibilityLevel level) {
        return switch (level) {
            case HIGH -> 0;
            case NEEDS_CHECK -> 1;
            case LOW -> 2;
        };
    }

    /**
     * 항목 점수 = 우선순위 가중치 + 신청 가능성 보너스(HIGH일수록 가점).
     */
    private int scoreOf(RuleItemResult item) {
        int eligibilityBonus = switch (item.eligibilityLevel()) {
            case HIGH -> 2;
            case NEEDS_CHECK -> 1;
            case LOW -> 0;
        };
        return item.priorityLevel().getWeight() + eligibilityBonus;
    }

    private String buildSummaryTitle(RuleContext context, List<RuleItemResult> items) {
        String eventLabel = eventLabel(context.eventType());
        return eventLabel + " 후 챙겨야 할 행정 절차 " + items.size() + "가지";
    }

    private String buildSummaryMessage(RuleContext context, List<RuleItemResult> items) {
        if (items.isEmpty()) {
            return "입력하신 정보로는 지금 바로 안내드릴 필수 절차가 확인되지 않았습니다. 상황이 바뀌면 다시 진단해 보세요.";
        }

        long highEligibility = items.stream()
                .filter(item -> item.eligibilityLevel() == EligibilityLevel.HIGH)
                .count();

        StringBuilder message = new StringBuilder();
        message.append(eventLabel(context.eventType()))
                .append(" 상황에서 놓치기 쉬운 절차 ")
                .append(items.size())
                .append("가지를 정리했어요. ");

        if (highEligibility > 0) {
            message.append("이 중 ")
                    .append(highEligibility)
                    .append("가지는 신청 가능성이 높으니 마감일을 꼭 확인하세요. ");
        }

        message.append("가장 급한 항목부터 순서대로 안내해 드립니다.");
        return message.toString();
    }

    private String eventLabel(LifeEventType eventType) {
        return switch (eventType) {
            case RETIREMENT -> "퇴직";
            case JOB_CHANGE -> "이직";
            case UNEMPLOYMENT -> "실직";
        };
    }
}
