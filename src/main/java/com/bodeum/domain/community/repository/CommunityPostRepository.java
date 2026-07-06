package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.model.CommunityPost;
import com.bodeum.domain.lifetransition.enumtype.LifeEventType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    List<CommunityPost> findAllByOrderByIdDesc(Pageable pageable);

    List<CommunityPost> findByCategoryOrderByIdDesc(LifeEventType category, Pageable pageable);

    List<CommunityPost> findAllByOrderByLikeCountDescIdDesc(Pageable pageable);

    List<CommunityPost> findByCategoryOrderByLikeCountDescIdDesc(LifeEventType category, Pageable pageable);
}
