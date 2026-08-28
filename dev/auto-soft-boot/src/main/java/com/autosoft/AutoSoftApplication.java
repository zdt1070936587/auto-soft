package com.autosoft;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 管理后台启动类。禁止在此编写业务初始化逻辑。
 */
@SpringBootApplication(scanBasePackages = "com.autosoft")
@MapperScan({"com.autosoft.system.mapper", "com.autosoft.meta.mapper", "com.autosoft.agent.mapper",
        "com.autosoft.flow.mapper", "com.autosoft.workflow.mapper"})
public class AutoSoftApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoSoftApplication.class, args);
    }
}
