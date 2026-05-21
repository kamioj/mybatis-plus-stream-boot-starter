package io.github.kamioj.quickstart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MyBatis-Plus Stream Boot Starter — Quickstart 示例入口。
 *
 * <p>运行方式：
 * <pre>
 *   mvn spring-boot:run
 * </pre>
 * 或在 IDE 中直接运行此类。启动后查看控制台输出即可看到各 API 的执行结果与对应 SQL。
 */
@SpringBootApplication
@MapperScan("io.github.kamioj.quickstart.mapper")
public class QuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickstartApplication.class, args);
    }
}
