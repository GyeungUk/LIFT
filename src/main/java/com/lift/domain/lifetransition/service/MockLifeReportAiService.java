package com.lift.domain.lifetransition.service;

import com.lift.domain.lifetransition.model.LifeReport;
import com.lift.domain.lifetransition.model.ReportItem;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * MVP용 mock AI 서비스.
 *
 * <p>실제 LLM 대신 리포트 요약과 항목을 조합해 결정적인 안내 문구를 만든다.
 * 룰 엔진이 계산한 내용을 벗어난 자격 판단은 하지 않는다.
 * 추후 이 구현을 OpenAI 연동 구현으로 교체하면 된다.
 */
@Service
@ConditionalOnProperty(prefix = "lift.openai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class MockLifeReportAiService implements LifeReportAiService {

    @Override
    public String generateAnswer(LifeReport report, String userQuestion) {
        List<ReportItem> items = report.getItems();

        String itemSummary = items.isEmpty()
                ? "현재 안내된 항목이 없습니다."
                : items.stream()
                        .map(item -> "· " + item.getTitle() + " (신청 가능성: " + item.getEligibilityLevel() + ")")
                        .collect(Collectors.joining("\n"));

        return """
                (안내용 mock 응답입니다. 자격 판단은 리포트의 룰 엔진 결과를 기준으로 하며, 정확한 자격은 관할 기관에서 확인하세요.)

                질문: %s

                이 리포트는 '%s' 상황을 기준으로 아래 항목을 정리했어요.
                %s

                궁금하신 부분은 위 항목의 '필요 서류'와 '공식 신청 링크'를 먼저 확인하시고, 마감일이 있는 항목부터 처리하시는 걸 추천드려요.
                """.formatted(
                userQuestion.strip(),
                report.getSummaryTitle(),
                itemSummary
        );
    }
}
