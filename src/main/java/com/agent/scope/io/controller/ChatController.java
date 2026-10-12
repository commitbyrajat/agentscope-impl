package com.agent.scope.io.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final OpenAIChatModel openAIChatModel;

    @Autowired
    public ChatController(OpenAIChatModel openAIChatModel) {
        this.openAIChatModel = openAIChatModel;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "Hello! Tell me a fun fact.") String prompt) {
        // 1. Build ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a helpful assistant.")
                .model(openAIChatModel)
                .build();

        // 2. Build User Msg using TextBlock and MsgRole.USER
        Msg userMessage = Msg.builder()
                .name("User")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(prompt).build())
                .build();

        // 3. ReActAgent execution is reactive: agent.call(...) returns Mono<Msg>
        Msg response = agent.call(userMessage).block();

        return (response != null) ? response.getTextContent() : "No response";
    }
}