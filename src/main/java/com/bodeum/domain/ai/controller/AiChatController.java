package com.bodeum.domain.ai.controller;

import com.bodeum.domain.ai.dto.request.AiChatMessageCreateReqDTO;
import com.bodeum.domain.ai.dto.request.AiMessageFeedbackCreateReqDTO;
import com.bodeum.domain.ai.dto.request.AiTermsAgreeReqDTO;
import com.bodeum.domain.ai.dto.response.AiChatMessageCreateResDTO;
import com.bodeum.domain.ai.dto.response.AiChatMessagesResDTO;
import com.bodeum.domain.ai.dto.response.AiChatRoomResDTO;
import com.bodeum.domain.ai.dto.response.AiMessageFeedbackCreateResDTO;
import com.bodeum.domain.ai.dto.response.AiSuggestedQuestionsResDTO;
import com.bodeum.domain.ai.dto.response.AiTermsAgreeResDTO;
import com.bodeum.domain.ai.dto.response.AiTermsStatusResDTO;
import com.bodeum.domain.ai.enumtype.AiFeedbackType;
import com.bodeum.domain.ai.enumtype.AiFeedbackReasonType;
import com.bodeum.domain.ai.enumtype.AiMessageSenderType;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AiChatController {

    @GetMapping("/ai/suggested-questions")
    public ApiResponse<AiSuggestedQuestionsResDTO> getSuggestedQuestions() {
        return ApiResponse.of(GeneralSuccessCode.OK, new AiSuggestedQuestionsResDTO(List.of(
                "아이가 밤마다 자주 깨요. 어떻게 해야 하나요?",
                "아이의 식습관을 어떻게 잡아주면 좋을까요?",
                "어린이집 적응을 도와주는 방법이 궁금해요.",
                "훈육할 때 어디까지 단호하게 말해야 하나요?",
                "아이와 애착을 높이는 놀이를 추천해 주세요."
        )));
    }

    @GetMapping("/ai/chat-room")
    public ApiResponse<AiChatRoomResDTO> getOrCreateChatRoom() {
        return ApiResponse.of(GeneralSuccessCode.OK, new AiChatRoomResDTO(
                1L,
                LocalDateTime.now(),
                true
        ));
    }

    @GetMapping("/ai/chat-room/messages")
    public ApiResponse<AiChatMessagesResDTO> getChatMessages(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<AiChatMessagesResDTO.MessageDTO> messages = List.of(
                new AiChatMessagesResDTO.MessageDTO(
                        1L,
                        AiMessageSenderType.USER,
                        "아이가 밤마다 자주 깨요.",
                        LocalDateTime.now(),
                        null,
                        null
                ),
                new AiChatMessagesResDTO.MessageDTO(
                        2L,
                        AiMessageSenderType.AI,
                        "수면 환경과 루틴을 먼저 점검해보는 것이 좋아요.",
                        LocalDateTime.now(),
                        new AiChatMessagesResDTO.ResponseSourceDTO(
                                "영유아 수면 가이드",
                                "https://example.com/sleep-guide"
                        ),
                        new AiChatMessagesResDTO.FeedbackDTO(AiFeedbackType.HELPFUL)
                )
        );

        return ApiResponse.of(GeneralSuccessCode.OK, new AiChatMessagesResDTO(messages, false, null));
    }

    @PostMapping("/ai/chat-room/messages")
    public ApiResponse<AiChatMessageCreateResDTO> createChatMessage(
            @Valid @RequestBody AiChatMessageCreateReqDTO request
    ) {
        LocalDateTime now = LocalDateTime.now();

        return ApiResponse.of(GeneralSuccessCode.OK, new AiChatMessageCreateResDTO(
                new AiChatMessageCreateResDTO.MessageDTO(
                        11L,
                        request.content(),
                        now,
                        null
                ),
                new AiChatMessageCreateResDTO.MessageDTO(
                        12L,
                        "아이의 상황을 조금 더 살펴본 뒤, 수면 루틴과 환경을 함께 점검해보세요.",
                        now.plusSeconds(3),
                        List.of(new AiChatMessageCreateResDTO.ResponseSourceDTO(
                                "영유아 수면 가이드",
                                "https://example.com/sleep-guide",
                                1L
                        ))
                ),
                List.of(
                        "수면 루틴은 어떻게 만들어야 하나요?",
                        "밤중 각성이 잦을 때 확인할 점은 무엇인가요?",
                        "낮잠 시간이 밤잠에 영향을 줄 수 있나요?"
                )
        ));
    }

    @GetMapping("/users/me/ai-terms-status")
    public ApiResponse<AiTermsStatusResDTO> getAiTermsStatus() {
        return ApiResponse.of(GeneralSuccessCode.OK, new AiTermsStatusResDTO(true));
    }

    @PostMapping("/users/me/ai-terms")
    public ApiResponse<AiTermsAgreeResDTO> agreeAiTerms(
            @Valid @RequestBody AiTermsAgreeReqDTO request
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, new AiTermsAgreeResDTO(LocalDateTime.now()));
    }

    @PostMapping("/ai/messages/{aiMessageId}/feedback")
    public ApiResponse<AiMessageFeedbackCreateResDTO> createMessageFeedback(
            @PathVariable Long aiMessageId,
            @Valid @RequestBody AiMessageFeedbackCreateReqDTO request
    ) {
        if (request.feedbackType() == AiFeedbackType.INCORRECT
                && (request.reasons() == null || request.reasons().isEmpty())) {
            throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
        }

        List<AiFeedbackReasonType> reasons = request.feedbackType() == AiFeedbackType.INCORRECT
                ? request.reasons()
                : null;

        return ApiResponse.of(GeneralSuccessCode.OK, new AiMessageFeedbackCreateResDTO(
                1L,
                request.feedbackType(),
                reasons
        ));
    }
}
