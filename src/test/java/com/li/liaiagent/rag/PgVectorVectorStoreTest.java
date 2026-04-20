package com.li.liaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.junit4.SpringRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
// 👇 关键：关闭测试自动回滚，让数据真正写入数据库
@Transactional
@Commit // 等价于 rollback = false，Spring 官方推荐写法
public class PgVectorVectorStoreTest {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testVectorStoreOperations() {
        try {
            // 1. 先清空表，避免重复数据干扰
            jdbcTemplate.update("TRUNCATE TABLE public.vector_store");
            log.info("🗑️ 清空 vector_store 表完成");

            // 2. 准备测试文档
            List<Document> documents = List.of(
                    new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
                    new Document("The World is Big and Salvation Lurks Around the Corner"),
                    new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2"))
            );

            // 3. 添加文档到向量库
            vectorStore.add(documents);
            log.info("✅ 文档添加成功！");

            // 4. 相似度查询（极低阈值 + 完全匹配的英文关键词，100% 能查到）
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("Spring AI")
                            .topK(5)
                            .similarityThreshold(0.01) // 极低阈值，确保匹配
                            .build()
            );

            // 5. 打印结果
            log.info("🔍 相似度查询结果数量：{}", results.size());
            results.forEach(doc ->
                    log.info("📄 文档内容：{} | 元数据：{}", doc.getText(), doc.getMetadata())
            );

        } catch (Exception e) {
            log.error("操作失败：", e);
        }
    }
}