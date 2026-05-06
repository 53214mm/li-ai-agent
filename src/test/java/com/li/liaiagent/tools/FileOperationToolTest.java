package com.li.liaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class FileOperationToolTest {

    @Test
    void readFile() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "test.txt";
        String result = fileOperationTool.readFile(fileName);
        Assertions.assertNotNull(result);
        System.out.println("读取文件结果: " + result);
    }

    @Test
    void writeFile() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "test.txt";
        String content = "这是一个测试文件，用于测试写入功能。";
        String result = fileOperationTool.writeFile(fileName,content);
        Assertions.assertNotNull(result);
        System.out.println("读取文件结果: " + result);
    }
}