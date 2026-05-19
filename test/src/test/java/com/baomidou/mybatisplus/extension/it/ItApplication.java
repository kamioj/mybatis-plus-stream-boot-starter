package com.baomidou.mybatisplus.extension.it;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 集成测试用最小 Spring Boot 启动类。
 *
 * <p>annotationClass=Mapper.class 让 MapperScan 只扫描带 @Mapper 注解的接口，
 * 避免 UserService（IStreamService 子接口）被误识别为 mapper。
 */
@SpringBootApplication
@MapperScan(basePackages = "com.baomidou.mybatisplus.extension.it", annotationClass = Mapper.class)
public class ItApplication {
}
