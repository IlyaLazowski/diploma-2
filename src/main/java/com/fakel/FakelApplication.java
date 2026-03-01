package com.fakel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FakelApplication {
    public static void main(String[] args) {
        SpringApplication.run(FakelApplication.class, args);
        System.out.println("======================");
        System.out.println("   FAKEL APP STARTED  ");
        System.out.println("======================");
    }
}