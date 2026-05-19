package com.baomidou.mybatisplus.extension.it;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 集成测试用最小 Spring Boot 启动类。
 */
@SpringBootApplication
@MapperScan("com.baomidou.mybatisplus.extension.it")
public class ItApplication {
}
