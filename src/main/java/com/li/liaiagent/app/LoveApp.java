package com.li.liaiagent.app;

import com.li.liaiagent.advisor.MyLoggerAdvisor;
import com.li.liaiagent.advisor.ReReadingAdvisor;
import com.li.liaiagent.advisor.ProhibitedWordsAdvisor;
import com.li.liaiagent.advisor.exception.DocumentNotFoundException;
import com.li.liaiagent.advisor.exception.QueryTimeoutException;
import com.li.liaiagent.advisor.exception.SimilarityTooLowException;
import com.li.liaiagent.chatMemory.FileBasedChatMemory;
import com.li.liaiagent.chatMemory.RedisBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    /**
     * 初始化 ChatClient
     *
     * @param dashscopeChatModel
     */
    public LoveApp(ChatModel dashscopeChatModel, RedisBasedChatMemory redisBasedChatMemory) {
        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
//        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(20)
//                .build();
        //初始化基于Redis的对话记忆
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(redisBasedChatMemory).build()
                        // 自定义日志 Advisor，可按需开启
                        ,new MyLoggerAdvisor()
                        // 注册敏感词拦截 Advisor
                        ,new ProhibitedWordsAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启
//                        ,new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        try {
            ChatResponse chatResponse = chatClient
                    .prompt()
                    .user(message)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .call()
                    .chatResponse();
            return chatResponse.getResult().getOutput().getText();
        } catch (RuntimeException e) {
            throw translateQueryException(e);
        }
    }


    record LoveReport(String title, List<String> suggestions) {
    }

    /**
     * AI 对话报告 （结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        try {
            return chatClient
                    .prompt()
                    .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                    .user(message)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .call()
                    .entity(LoveReport.class);
        } catch (RuntimeException e) {
            throw translateQueryException(e);
        }
    }

    private RuntimeException translateQueryException(RuntimeException e) {
        String allMessages = buildMessages(e);
        String lower = allMessages.toLowerCase();
        if (isDocumentNotFound(lower)) {
            return new DocumentNotFoundException("未找到可用文档，请补充知识库后重试", e);
        }
        if (isSimilarityTooLow(lower)) {
            return new SimilarityTooLowException("检索文档相似度过低，请换个更具体的问题重试", e);
        }
        if (isQueryTimeout(e, lower)) {
            return new QueryTimeoutException("查询超时，请稍后重试", e);
        }
        return e;
    }

    private boolean isDocumentNotFound(String lower) {
        return lower.contains("no document")
                || lower.contains("document not found")
                || lower.contains("no relevant document")
                || lower.contains("文档未找到")
                || lower.contains("未找到文档")
                || lower.contains("找不到文档");
    }

    private boolean isSimilarityTooLow(String lower) {
        return lower.contains("similarity too low")
                || lower.contains("similarity threshold")
                || lower.contains("相似度过低")
                || lower.contains("低于相似度阈值");
    }

    private boolean isQueryTimeout(Throwable throwable, String lower) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.util.concurrent.TimeoutException
                    || current instanceof java.net.http.HttpTimeoutException
                    || current instanceof java.net.SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return lower.contains("timeout")
                || lower.contains("timed out")
                || lower.contains("query timed out")
                || lower.contains("超时");
    }

    private String buildMessages(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                sb.append(current.getMessage()).append(" ");
            }
            current = current.getCause();
        }
        return sb.toString();
    }
}
