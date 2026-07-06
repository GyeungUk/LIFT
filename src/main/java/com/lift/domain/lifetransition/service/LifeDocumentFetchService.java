package com.lift.domain.lifetransition.service;

import com.lift.domain.lifetransition.dto.response.DocumentFetchResDTO;
import com.lift.domain.lifetransition.dto.response.DocumentFetchResDTO.FetchedDocument;
import com.lift.domain.lifetransition.dto.response.DocumentFetchResDTO.ItemDocuments;
import com.lift.domain.lifetransition.enumtype.DocumentFetchStatus;
import com.lift.domain.lifetransition.model.LifeReport;
import com.lift.domain.lifetransition.model.RequiredDocument;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 필요 서류 자동 조회(mock) 서비스.
 *
 * <p>실제 공공 마이데이터/전자문서지갑 연동은 후속 과제이며, 현재는 발급 기관 문자열을 기준으로
 * "자동 조회 가능/직접 준비 필요"를 결정적으로 흉내 낸다. 결제 완료된 리포트에서만 이용 가능하다.
 * 추후 이 서비스만 실제 연동 구현으로 교체하면 된다.
 */
@Service
@RequiredArgsConstructor
public class LifeDocumentFetchService {

    private static final String MOCK_SOURCE = "공공 마이데이터(모의 연동)";

    // 자동 조회가 가능한 발급 기관(공공기관) 키워드
    private static final List<String> PUBLIC_ISSUER_KEYWORDS = List.of(
            "공단", "고용센터", "국세청", "홈택스", "건강보험", "국민연금", "근로복지", "고용노동"
    );

    private final LifeReportAccessManager reportAccessManager;

    @Transactional(readOnly = true)
    public DocumentFetchResDTO fetch(Authentication authentication, Long reportId) {
        LifeReport report = reportAccessManager.getPaidOwnedReport(authentication, reportId);

        List<ItemDocuments> items = report.getItems().stream()
                .map(item -> new ItemDocuments(
                        item.getId(),
                        item.getProcedureType(),
                        item.getProcedureType().getDisplayName(),
                        item.getRequiredDocuments().stream()
                                .map(this::resolve)
                                .toList()
                ))
                .toList();

        int totalCount = items.stream().mapToInt(i -> i.documents().size()).sum();
        int autoFetchedCount = (int) items.stream()
                .flatMap(i -> i.documents().stream())
                .filter(d -> d.status() == DocumentFetchStatus.FETCHED)
                .count();

        return new DocumentFetchResDTO(reportId, LocalDateTime.now(), totalCount, autoFetchedCount, items);
    }

    private FetchedDocument resolve(RequiredDocument doc) {
        String issuer = doc.getIssuer() == null ? "" : doc.getIssuer();

        if (isPublicIssuer(issuer)) {
            return new FetchedDocument(
                    doc.getDocumentName(),
                    doc.getIssuer(),
                    DocumentFetchStatus.FETCHED,
                    DocumentFetchStatus.FETCHED.getDisplayName(),
                    MOCK_SOURCE,
                    issuer + "에서 자동으로 불러왔어요. 바로 첨부할 수 있습니다.",
                    // 실제 파일 연동 전까지의 목업 다운로드 경로
                    "/mock/documents/" + doc.getDocumentName() + ".pdf"
            );
        }

        if (issuer.contains("본인")) {
            return new FetchedDocument(
                    doc.getDocumentName(),
                    doc.getIssuer(),
                    DocumentFetchStatus.ACTION_REQUIRED,
                    DocumentFetchStatus.ACTION_REQUIRED.getDisplayName(),
                    "본인 보관 서류",
                    "본인이 직접 준비해야 하는 서류예요(신분증·통장 등).",
                    null
            );
        }

        return new FetchedDocument(
                doc.getDocumentName(),
                doc.getIssuer(),
                DocumentFetchStatus.ACTION_REQUIRED,
                DocumentFetchStatus.ACTION_REQUIRED.getDisplayName(),
                "발급 기관 요청",
                (issuer.isBlank() ? "발급 기관" : issuer) + "에 발급을 요청하세요.",
                null
        );
    }

    private boolean isPublicIssuer(String issuer) {
        return PUBLIC_ISSUER_KEYWORDS.stream().anyMatch(issuer::contains);
    }
}
