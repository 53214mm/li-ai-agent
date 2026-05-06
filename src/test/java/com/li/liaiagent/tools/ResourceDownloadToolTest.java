package com.li.liaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ResourceDownloadToolTest {

    @Test
    public void testDownloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String url = "https://ts3.tc.mm.bing.net/th?id=ORMS.3b592412707d9d2981344d5b6530c0db&pid=Wdp&w=268&h=140&qlt=90&c=1&rs=1&dpr=1&p=0";
        String fileName = "logo.png";
        String result = tool.downloadResource(url, fileName);
        assertNotNull(result);
    }
}
