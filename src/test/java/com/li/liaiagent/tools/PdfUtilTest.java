package com.li.liaiagent.tools;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PdfUtil 测试类
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PdfUtilTest {

    private static final String TEST_DIR = System.getProperty("user.dir") + "/tmp/file/pdf_test";
    private static final String TEST_PDF_1 = TEST_DIR + "/test_create.pdf";
    private static final String TEST_PDF_2 = TEST_DIR + "/test_page2.pdf";
    private static final String TEST_MERGED = TEST_DIR + "/test_merged.pdf";
    private static final String TEST_SPLIT_DIR = TEST_DIR + "/split";
    private static final String TEST_WATERMARKED = TEST_DIR + "/test_watermarked.pdf";

    @BeforeAll
    static void setUp() {
        new File(TEST_DIR).mkdirs();
    }

    // ==================== 1. 创建 PDF ====================

    @Test
    @Order(1)
    void createPdf() {
        String content = """
                恋爱心理学报告
                
                第一章 亲密关系的建立
                
                亲密关系是人际交往中最深层次的情感连接，
                它需要双方的信任、理解与包容。
                
                研究表明，健康的恋爱关系具有以下特征：
                1. 有效沟通——坦诚而尊重地表达想法
                2. 情感支持——在对方需要时给予陪伴
                3. 边界尊重——允许彼此保留独立空间
                
                第二章 常见问题与应对
                
                恋爱中的冲突是不可避免的，关键在于如何处理。
                建设性的冲突处理方式包括：
                - 就事论事，不翻旧账
                - 使用"我"陈述句而非指责
                - 在情绪平复后再讨论敏感话题
                """;

        String result = PdfUtil.createPdf(TEST_PDF_1, content);
        assertNotNull(result);
        assertTrue(new File(TEST_PDF_1).exists());
        System.out.println("✅ 创建 PDF 成功: " + result);
    }

    // ==================== 2. 提取文本 ====================

    @Test
    @Order(2)
    void extractText() {
        String text = PdfUtil.extractText(TEST_PDF_1);
        assertNotNull(text);
        assertFalse(text.isBlank());
        assertTrue(text.contains("恋爱心理学报告"));
        assertTrue(text.contains("亲密关系"));
        System.out.println("✅ 提取文本成功，长度: " + text.length() + " 字符");
        System.out.println("--- 文本内容预览 ---");
        System.out.println(text.substring(0, Math.min(200, text.length())));
    }

    @Test
    @Order(3)
    void extractTextByPage() {
        List<String> pages = PdfUtil.extractTextByPage(TEST_PDF_1);
        assertNotNull(pages);
        assertTrue(pages.size() > 0);
        System.out.println("✅ 分页提取成功，共 " + pages.size() + " 页");
        for (int i = 0; i < pages.size(); i++) {
            System.out.println("--- 第 " + (i + 1) + " 页 (前80字符) ---");
            String pageText = pages.get(i);
            System.out.println(pageText.substring(0, Math.min(80, pageText.length())));
        }
    }

    @Test
    @Order(4)
    void getPageCount() {
        int count = PdfUtil.getPageCount(TEST_PDF_1);
        assertTrue(count > 0);
        System.out.println("✅ 页数: " + count);
    }

    // ==================== 3. 合并 PDF ====================

    @Test
    @Order(5)
    void mergePdfs() {
        // 先创建第二个测试 PDF
        String content2 = """
                附录：推荐阅读
                
                《爱的五种语言》—— 盖瑞·查普曼
                《亲密关系》—— 罗兰·米勒
                《爱的艺术》—— 艾里希·弗洛姆
                """;
        PdfUtil.createPdf(TEST_PDF_2, content2);

        // 合并
        String result = PdfUtil.mergePdfs(List.of(TEST_PDF_1, TEST_PDF_2), TEST_MERGED);
        assertNotNull(result);
        assertTrue(new File(TEST_MERGED).exists());

        // 验证合并后文本包含两篇的内容
        String mergedText = PdfUtil.extractText(TEST_MERGED);
        assertTrue(mergedText.contains("恋爱心理学报告"));
        assertTrue(mergedText.contains("爱的五种语言"));

        int mergedPages = PdfUtil.getPageCount(TEST_MERGED);
        System.out.println("✅ 合并成功，输出: " + result + "，共 " + mergedPages + " 页");
    }

    // ==================== 4. 拆分 PDF ====================

    @Test
    @Order(6)
    void splitPdf() {
        List<String> result = PdfUtil.splitPdf(TEST_MERGED, TEST_SPLIT_DIR);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        System.out.println("✅ 拆分成功，生成 " + result.size() + " 个文件:");
        result.forEach(path -> System.out.println("  - " + path));

        // 验证每个拆分文件都存在
        for (String path : result) {
            assertTrue(new File(path).exists());
        }
    }

    // ==================== 5. 水印 ====================

    @Test
    @Order(7)
    void addWatermark() {
        String result = PdfUtil.addWatermark(TEST_PDF_1, TEST_WATERMARKED,
                "仅供内部参考", 40f, 0.15f);
        assertNotNull(result);
        assertTrue(new File(TEST_WATERMARKED).exists());
        System.out.println("✅ 水印添加成功: " + result);
    }

    // ==================== 6. 边界测试 ====================

    @Test
    @Order(8)
    void createPdfWithBlankLines() {
        String path = TEST_DIR + "/test_blank.pdf";
        String content = "\n\n只有一行内容\n\n";
        String result = PdfUtil.createPdf(path, content);
        assertTrue(new File(path).exists());
        String text = PdfUtil.extractText(path);
        assertTrue(text.contains("只有一行内容"));
        System.out.println("✅ 空白行处理正常");
    }

    @Test
    @Order(9)
    void mergeWithEmptyList() {
        assertThrows(IllegalArgumentException.class, () ->
                PdfUtil.mergePdfs(List.of(), TEST_DIR + "/should_not_exist.pdf"));
        System.out.println("✅ 空列表合并正确抛出异常");
    }

    @AfterAll
    static void tearDown() {
        // 清理测试文件（可选：注释掉以保留文件用于检查）
        // try {
        //     Files.walk(Path.of(TEST_DIR))
        //         .sorted(java.util.Comparator.reverseOrder())
        //         .map(Path::toFile)
        //         .forEach(File::delete);
        // } catch (Exception ignored) {}
        System.out.println("\n测试文件保留在: " + TEST_DIR);
    }
}
