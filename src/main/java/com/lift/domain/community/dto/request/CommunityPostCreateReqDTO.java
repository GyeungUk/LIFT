package com.lift.domain.community.dto.request;

import com.lift.domain.lifetransition.enumtype.LifeEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommunityPostCreateReqDTO(
        @NotNull(message = "게시판 분야는 필수입니다.")
        LifeEventType category,

        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 120, message = "제목은 120자까지 입력할 수 있습니다.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(max = 3000, message = "내용은 3000자까지 입력할 수 있습니다.")
        String content
) {
}
