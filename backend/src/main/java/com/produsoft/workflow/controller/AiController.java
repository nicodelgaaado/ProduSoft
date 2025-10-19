package com.produsoft.workflow.controller;

import com.produsoft.workflow.dto.AiChatRequest;
import com.produsoft.workflow.dto.AiChatResponse;
import com.produsoft.workflow.dto.AiConversationResponse;
import com.produsoft.workflow.dto.AiConversationSummaryResponse;
import com.produsoft.workflow.dto.CreateConversationRequest;
import com.produsoft.workflow.dto.SendMessageRequest;
import com.produsoft.workflow.dto.UpdateConversationTitleRequest;
import com.produsoft.workflow.service.AiChatService;
import com.produsoft.workflow.service.AiConversationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiChatService aiChatService;
    private final AiConversationService conversationService;

    public AiController(AiChatService aiChatService, AiConversationService conversationService) {
        this.aiChatService = aiChatService;
        this.conversationService = conversationService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiChatService.chat(request);
    }

    @GetMapping("/conversations")
    public List<AiConversationSummaryResponse> listConversations(Authentication authentication) {
        return conversationService.listConversations(authentication.getName());
    }

    @PostMapping("/conversations")
    public AiConversationResponse createConversation(Authentication authentication,
                                                     @Valid @RequestBody CreateConversationRequest request) {
        return conversationService.createConversation(authentication.getName(), request);
    }

    @GetMapping("/conversations/{conversationId}")
    public AiConversationResponse getConversation(Authentication authentication,
                                                  @PathVariable Long conversationId) {
        return conversationService.getConversation(authentication.getName(), conversationId);
    }

    @PostMapping(value = "/conversations/{conversationId}/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public AiConversationResponse sendMessage(Authentication authentication,
                                              @PathVariable Long conversationId,
                                              @Valid @RequestBody SendMessageRequest request) {
        return conversationService.sendMessage(authentication.getName(), conversationId, request);
    }

    @PostMapping(value = "/conversations/{conversationId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(Authentication authentication,
                                    @PathVariable Long conversationId,
                                    @Valid @RequestBody SendMessageRequest request) {
        return conversationService.streamMessage(authentication.getName(), conversationId, request);
    }

    @PatchMapping("/conversations/{conversationId}")
    public AiConversationResponse renameConversation(Authentication authentication,
                                                     @PathVariable Long conversationId,
                                                     @Valid @RequestBody UpdateConversationTitleRequest request) {
        return conversationService.renameConversation(authentication.getName(), conversationId, request.title());
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(Authentication authentication,
                                   @PathVariable Long conversationId) {
        conversationService.deleteConversation(authentication.getName(), conversationId);
    }
}
