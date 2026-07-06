package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.model.CommunityPostLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {

    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    Optional<CommunityPostLike> findByPost_IdAndUser_Id(Long postId, Long userId);

    void deleteByPost_Id(Long postId);
}
