package com.lift.domain.lifetransition.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리포트 항목별 필요 서류.
 */
@Entity
@Getter
@Table(name = "life_required_documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequiredDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_item_id", nullable = false)
    private ReportItem reportItem;

    @Column(name = "document_name", nullable = false, length = 150)
    private String documentName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "issuer", length = 150)
    private String issuer;

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;

    private RequiredDocument(String documentName, String description, String issuer, boolean isRequired) {
        this.documentName = documentName;
        this.description = description;
        this.issuer = issuer;
        this.isRequired = isRequired;
    }

    public static RequiredDocument create(String documentName, String description, String issuer, boolean isRequired) {
        return new RequiredDocument(documentName, description, issuer, isRequired);
    }

    void assignReportItem(ReportItem reportItem) {
        this.reportItem = reportItem;
    }
}
