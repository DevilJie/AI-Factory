package com.aifactory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 数据库初始化配置
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
// 已禁用 - MySQL数据库已手动初始化
//@Configuration
public class DatabaseInitConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    // @PostConstruct
    public void initDatabase() {
        try {
            // 确保db目录存在
            String dbPath = dbUrl.replace("jdbc:sqlite:", "");
            java.io.File dbFile = new java.io.File(dbPath);
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            // 读取并执行初始化SQL
            // 从jar包运行时，使用相对路径
            java.io.File sqlFile = new java.io.File("db/init.sql");
            if (!sqlFile.exists()) {
                // 如果相对路径不存在，尝试绝对路径
                sqlFile = new java.io.File("../db/init.sql");
            }

            if (sqlFile.exists()) {
                String sql = new String(Files.readAllBytes(sqlFile.toPath()));

                try (Connection conn = DriverManager.getConnection(dbUrl);
                     Statement stmt = conn.createStatement()) {

                    // 分割并执行SQL语句
                    String[] statements = sql.split(";");
                    for (String statement : statements) {
                        String trimmed = statement.trim();
                        if (!trimmed.isEmpty()) {
                            stmt.execute(trimmed);
                        }
                    }
                }

                System.out.println("数据库初始化成功！");
            } else {
                System.err.println("找不到初始化SQL文件：" + sqlFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("数据库初始化失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
