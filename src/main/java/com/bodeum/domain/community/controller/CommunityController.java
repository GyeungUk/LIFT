package com.bodeum.domain.community.controller;

import com.bodeum.domain.community.dto.request.CommunityCommentCreateReqDTO;
import com.bodeum.domain.community.dto.request.CommunityPostCreateReqDTO;
import com.bodeum.domain.community.dto.response.CommunityCommentResDTO;
import com.bodeum.domain.community.dto.response.CommunityPostDetailResDTO;
import com.bodeum.domain.community.dto.response.CommunityPostLikeResDTO;
import com.bodeum.domain.community.dto.response.CommunityPostSummaryResDTO;
import com.bodeum.domain.community.service.CommunityService;
import com.bodeum.domain.lifetransition.enumtype.LifeEventType;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/posts")
    public ApiResponse<List<CommunityPostSummaryResDTO>> getPosts(
            Authentication authentication,
            @RequestParam(required = false) LifeEventType category,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, communityService.getPosts(authentication, category, size));
    }

    @GetMapping("/posts/popular")
    public ApiResponse<List<CommunityPostSummaryResDTO>> getPopularPosts(
            Authentication authentication,
            @RequestParam(required = false) LifeEventType category,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, communityService.getPopularPosts(authentication, category, size));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<CommunityPostDetailResDTO> getPost(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, communityService.getPost(authentication, postId));
    }

    @PostMapping("/posts")
    public ApiResponse<CommunityPostDetailResDTO> createPost(
            Authentication authentication,
            @Valid @RequestBody CommunityPostCreateReqDTO request
    ) {
        return ApiResponse.of(GeneralSuccessCode.CREATED, communityService.createPost(authentication, request));
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Boolean> deletePost(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, communityService.deletePost(authentication, postId));
    }

    @PostMapping("/posts/{postId}/likes")
    public ApiResponse<CommunityPostLikeResDTO> likePost(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, communityService.likePost(authentication, postId));
    }

    @DeleteMapping("/posts/{postId}/likes")
    public ApiResponse<CommunityPostLikeResDTO> unlikePost(
            Authentication authentication,
            @PathVariable Long postId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, communityService.unlikePost(authentication, postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommunityCommentResDTO> createComment(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody CommunityCommentCreateReqDTO request
    ) {
        return ApiResponse.of(GeneralSuccessCode.CREATED, communityService.createComment(authentication, postId, request));
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ApiResponse<Boolean> deleteComment(
            Authentication authentication,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, communityService.deleteComment(authentication, postId, commentId));
    }
}
