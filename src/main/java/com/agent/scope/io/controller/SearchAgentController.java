package com.agent.scope.io.controller;
import com.agent.scope.io.tool.GoogleWebSearchTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchAgentController {

    private final OpenAIChatModel openAIChatModel;
    private final GoogleWebSearchTools googleWebSearchTools;

    @Autowired
    public SearchAgentController(OpenAIChatModel openAIChatModel, GoogleWebSearchTools googleWebSearchTools) {
        this.openAIChatModel = openAIChatModel;
        this.googleWebSearchTools = googleWebSearchTools;
    }

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "What are the latest features in Java?") String prompt) {

        // 1. Register the tool in the AgentScope Toolkit
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(googleWebSearchTools);

        // 2. Build ReActAgent equipped with the tool
        ReActAgent agent = ReActAgent.builder()
                .name("TikaSearchAnalyst")
                .sysPrompt("""
                    You are a real-time research assistant.
                    When answering queries:
                    1. Use the 'webSearch' tool to retrieve current web results and links.
                    2. Analyze the extracted snippet and links.
                    3. Return an informative, structured answer citing the URLs found.
                    """)
                .model(openAIChatModel)
                .toolkit(toolkit)
                .build();

        // 3. Construct input message
        Msg userMessage = Msg.builder()
                .name("User")
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(prompt).build())
                .build();

        // 4. Trigger the reactive execution loop
        Msg response = agent.call(userMessage).block();

        return (response != null && response.getTextContent() != null)
                ? response.getTextContent()
                : "No response received";
    }
}