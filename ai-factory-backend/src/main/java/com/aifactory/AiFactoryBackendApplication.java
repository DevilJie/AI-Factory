package com.aifactory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Factory 后端服务启动类
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@SpringBootApplication
@MapperScan("com.aifactory.mapper")
public class AiFactoryBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiFactoryBackendApplication.class, args);
        System.out.println("""

                ======================================
                 AI Factory Backend Started Successfully!
                 Port: 1024
                ======================================
                """);
    }
}
