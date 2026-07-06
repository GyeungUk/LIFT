package com.bodeum.domain.lifetransition.dto.response;

import com.bodeum.domain.lifetransition.enumtype.DocumentFetchStatus;
import com.bodeum.domain.lifetransition.enumtype.ProcedureType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 리포트에 필요한 서류를 한 번에 자동 조회(mock)한 결과.
 * 실제 공공 마이데이터/전자문서지갑 연동 전까지의 목업 응답이다.
 */
public record DocumentFetchResDTO(
        Long reportId,
        LocalDateTime fetchedAt,
        int totalCount,
        int autoFetchedCount,
        List<ItemDocuments> items
) {

    public record ItemDocuments(
            Long itemId,
            ProcedureType procedureType,
            String procedureName,
            List<FetchedDocument> documents
    ) {
    }

    public record FetchedDocument(
            String documentName,
            String issuer,
            DocumentFetchStatus status,
            String statusLabel,
            String source,
            String message,
            String mockDownloadUrl
    ) {
    }
}
