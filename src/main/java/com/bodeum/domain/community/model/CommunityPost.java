package com.bodeum.domain.community.model;

import com.bodeum.domain.lifetransition.enumtype.LifeEventType;
import com.bodeum.domain.user.model.UserAccount;
import com.bodeum.global.common.entity.BaseCreatedUpdatedEntity;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "community_posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost extends BaseCreatedUpdatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private LifeEventType category;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "content", nullable = false, length = 3000)
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CommunityComment> comments = new ArrayList<>();

    private CommunityPost(UserAccount author, LifeEventType category, String title, String content) {
        this.author = author;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public static CommunityPost create(
            UserAccount author,
            LifeEventType category,
            String title,
            String content
    ) {
        return new CommunityPost(author, category, title, content);
    }

    public boolean isAuthoredBy(Long userId) {
        return author != null && author.getId() != null && author.getId().equals(userId);
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        this.commentCount = Math.max(0, this.commentCount - 1);
    }
}
