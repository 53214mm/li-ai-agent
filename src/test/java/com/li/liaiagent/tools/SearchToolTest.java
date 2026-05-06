package com.li.liaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SearchToolTest {

    @Autowired
    private SearchTool searchTool;

    @Test
    void search() {
        try {
            String s = searchTool.search("如何提升编程能力");
            System.out.println("搜索结果: " + s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}