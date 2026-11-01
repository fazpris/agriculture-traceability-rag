package com.tracegrid;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 农产品溯源系统启动类
 */
@SpringBootApplication
@MapperScan("com.tracegrid.**.mapper")
public class TracegridApplication {
    public static void main(String[] args) {
        SpringApplication.run(TracegridApplication.class, args);
    }
}
