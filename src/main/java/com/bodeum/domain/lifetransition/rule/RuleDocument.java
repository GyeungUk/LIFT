package com.bodeum.domain.lifetransition.rule;

/**
 * 룰이 산출하는 필요 서류 값 객체. 엔티티(RequiredDocument)로 변환되기 전의 순수 데이터.
 */
public record RuleDocument(
        String documentName,
        String description,
        String issuer,
        boolean required
) {
}
