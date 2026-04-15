package com.muebleria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class FurnitureStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(FurnitureStoreApplication.class, args);
    }
}
