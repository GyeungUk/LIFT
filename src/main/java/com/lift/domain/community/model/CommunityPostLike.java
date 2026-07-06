package com.lift.domain.community.model;

import com.lift.domain.user.model.UserAccount;
import com.lift.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "community_post_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_community_post_likes_post_user",
                columnNames = {"post_id", "user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostLike extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    private CommunityPostLike(CommunityPost post, UserAccount user) {
        this.post = post;
        this.user = user;
    }

    public static CommunityPostLike create(CommunityPost post, UserAccount user) {
        return new CommunityPostLike(post, user);
    }
}
