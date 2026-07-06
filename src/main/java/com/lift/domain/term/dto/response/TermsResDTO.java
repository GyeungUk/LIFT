package com.lift.domain.term.dto.response;

import com.lift.domain.term.enumtype.TermType;
import java.time.LocalDate;

public record TermsResDTO(
        String type,
        String title,
        String version,
        LocalDate effectiveDate,
        String content,
        boolean required
) {

    public static TermsResDTO of(TermType type, String version, LocalDate effectiveDate, String content) {
        return new TermsResDTO(
                type.getPath(),
                type.getTitle(),
                version,
                effectiveDate,
                content,
                true
        );
    }
}
