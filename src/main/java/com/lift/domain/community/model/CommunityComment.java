package com.lift.domain.community.model;

import com.lift.domain.user.model.UserAccount;
import com.lift.global.common.entity.BaseCreatedUpdatedEntity;
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

@Entity
@Getter
@Table(name = "community_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityComment extends BaseCreatedUpdatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    private CommunityComment(CommunityPost post, UserAccount author, String content) {
        this.post = post;
        this.author = author;
        this.content = content;
    }

    public static CommunityComment create(CommunityPost post, UserAccount author, String content) {
        return new CommunityComment(post, author, content);
    }

    public boolean isAuthoredBy(Long userId) {
        return author != null && author.getId() != null && author.getId().equals(userId);
    }
}
