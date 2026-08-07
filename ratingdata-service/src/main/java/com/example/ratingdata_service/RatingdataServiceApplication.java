package com.example.ratingdata_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.ratingdata_service")
public class RatingdataServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(RatingdataServiceApplication.class, args);
	}
}