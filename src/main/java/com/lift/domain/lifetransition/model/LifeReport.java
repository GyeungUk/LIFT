package com.lift.domain.lifetransition.model;

import com.lift.domain.lifetransition.enumtype.PaymentStatus;
import com.lift.global.common.entity.BaseCreatedEntity;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 룰 엔진 분석 결과로 생성되는 리포트. 미리보기/결제/상세/AI 채팅의 중심 엔티티.
 */
@Entity
@Getter
@Table(name = "life_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LifeReport extends BaseCreatedEntity {

    private static final int DEFAULT_AI_QUESTION_LIMIT = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false, unique = true)
    private LifeAssessment assessment;

    @Column(name = "summary_title", nullable = false, length = 150)
    private String summaryTitle;

    @Column(name = "summary_message", nullable = false, length = 500)
    private String summaryMessage;

    @Column(name = "total_priority_score", nullable = false)
    private int totalPriorityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "ai_question_limit", nullable = false)
    private int aiQuestionLimit;

    @Column(name = "ai_question_used_count", nullable = false)
    private int aiQuestionUsedCount;

    @Column(name = "payment_provider", length = 30)
    private String paymentProvider;

    @Column(name = "payment_order_id", length = 80)
    private String paymentOrderId;

    @Column(name = "payment_key", length = 220)
    private String paymentKey;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ReportItem> items = new ArrayList<>();

    private LifeReport(
            LifeAssessment assessment,
            String summaryTitle,
            String summaryMessage,
            int totalPriorityScore
    ) {
        this.assessment = assessment;
        this.summaryTitle = summaryTitle;
        this.summaryMessage = summaryMessage;
        this.totalPriorityScore = totalPriorityScore;
        this.paymentStatus = PaymentStatus.UNPAID;
        this.aiQuestionLimit = DEFAULT_AI_QUESTION_LIMIT;
        this.aiQuestionUsedCount = 0;
    }

    public static LifeReport create(
            LifeAssessment assessment,
            String summaryTitle,
            String summaryMessage,
            int totalPriorityScore
    ) {
        return new LifeReport(assessment, summaryTitle, summaryMessage, totalPriorityScore);
    }

    public void addItem(ReportItem item) {
        items.add(item);
        item.assignReport(this);
    }

    public void markPaid() {
        markPaid("MOCK", null, null);
    }

    public void markTossTestPaid(String orderId, String paymentKey) {
        markPaid("TOSS_TEST", orderId, paymentKey);
    }

    private void markPaid(String provider, String orderId, String paymentKey) {
        this.paymentStatus = PaymentStatus.PAID;
        this.paymentProvider = provider;
        this.paymentOrderId = orderId;
        this.paymentKey = paymentKey;
        this.paidAt = LocalDateTime.now();
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    public boolean isAiQuestionLimitReached() {
        return aiQuestionUsedCount >= aiQuestionLimit;
    }

    public int getAiQuestionRemaining() {
        return Math.max(0, aiQuestionLimit - aiQuestionUsedCount);
    }

    public void increaseAiQuestionUsedCount() {
        this.aiQuestionUsedCount++;
    }

    public boolean isOwnedBy(Long userId) {
        return assessment != null && assessment.isOwnedBy(userId);
    }
}
