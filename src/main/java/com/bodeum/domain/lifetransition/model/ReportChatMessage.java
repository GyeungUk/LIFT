package com.bodeum.domain.lifetransition.model;

import com.bodeum.domain.lifetransition.enumtype.ChatSenderType;
import com.bodeum.global.common.entity.BaseCreatedEntity;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리포트 기반 AI 채팅 메시지(사용자 질문 / AI 응답).
 */
@Entity
@Getter
@Table(name = "life_report_chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportChatMessage extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private LifeReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 10)
    private ChatSenderType senderType;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    private ReportChatMessage(LifeReport report, ChatSenderType senderType, String content) {
        this.report = report;
        this.senderType = senderType;
        this.content = content;
    }

    public static ReportChatMessage userMessage(LifeReport report, String content) {
        return new ReportChatMessage(report, ChatSenderType.USER, content);
    }

    public static ReportChatMessage aiMessage(LifeReport report, String content) {
        return new ReportChatMessage(report, ChatSenderType.AI, content);
    }
}
