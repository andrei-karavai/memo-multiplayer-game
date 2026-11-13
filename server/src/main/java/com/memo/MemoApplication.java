package com.memo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot entry point and root component-scanning anchor.
 *
 */
@SpringBootApplication
public class MemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MemoApplication.class, args);
    }
}
