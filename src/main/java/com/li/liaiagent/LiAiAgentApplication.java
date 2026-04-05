package com.li.liaiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class LiAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiAiAgentApplication.class, args);
    }

}
