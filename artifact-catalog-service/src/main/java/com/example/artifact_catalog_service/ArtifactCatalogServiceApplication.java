package com.example.artifact_catalog_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class ArtifactCatalogServiceApplication {

	@Bean
	public RestTemplate getRestTemplate() {
	return new RestTemplate();
	}
	
	@Bean
	public WebClient.Builder getCliBuilder(){
		return WebClient.builder();
	};
	public static void main(String[] args) {
		SpringApplication.run(ArtifactCatalogServiceApplication.class, args);
	}

}
