package com.lift.domain.lifetransition.model;

import com.lift.domain.lifetransition.enumtype.EligibilityLevel;
import com.lift.domain.lifetransition.enumtype.PriorityLevel;
import com.lift.domain.lifetransition.enumtype.ProcedureType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리포트에 포함되는 개별 행정 절차 항목.
 */
@Entity
@Getter
@Table(name = "life_report_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private LifeReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "procedure_type", nullable = false, length = 40)
    private ProcedureType procedureType;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_level", nullable = false, length = 20)
    private EligibilityLevel eligibilityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 20)
    private PriorityLevel priorityLevel;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "deadline_text", length = 200)
    private String deadlineText;

    @Column(name = "official_url", length = 500)
    private String officialUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "reportItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequiredDocument> requiredDocuments = new ArrayList<>();

    private ReportItem(
            ProcedureType procedureType,
            EligibilityLevel eligibilityLevel,
            PriorityLevel priorityLevel,
            String title,
            String reason,
            String deadlineText,
            String officialUrl,
            int sortOrder
    ) {
        this.procedureType = procedureType;
        this.eligibilityLevel = eligibilityLevel;
        this.priorityLevel = priorityLevel;
        this.title = title;
        this.reason = reason;
        this.deadlineText = deadlineText;
        this.officialUrl = officialUrl;
        this.sortOrder = sortOrder;
    }

    public static ReportItem create(
            ProcedureType procedureType,
            EligibilityLevel eligibilityLevel,
            PriorityLevel priorityLevel,
            String title,
            String reason,
            String deadlineText,
            String officialUrl,
            int sortOrder
    ) {
        return new ReportItem(procedureType, eligibilityLevel, priorityLevel, title, reason, deadlineText, officialUrl, sortOrder);
    }

    void assignReport(LifeReport report) {
        this.report = report;
    }

    public void addRequiredDocument(RequiredDocument document) {
        requiredDocuments.add(document);
        document.assignReportItem(this);
    }
}
