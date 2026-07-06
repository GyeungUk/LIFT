package com.lift.domain.lifetransition.dto.response;

import com.lift.domain.lifetransition.model.RequiredDocument;

public record RequiredDocumentResDTO(
        String documentName,
        String description,
        String issuer,
        boolean required
) {

    public static RequiredDocumentResDTO from(RequiredDocument document) {
        return new RequiredDocumentResDTO(
                document.getDocumentName(),
                document.getDescription(),
                document.getIssuer(),
                document.isRequired()
        );
    }
}
