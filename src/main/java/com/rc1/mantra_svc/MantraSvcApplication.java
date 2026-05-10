package com.rc1.mantra_svc;

import com.rc1.mantra_svc.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mantra Application - Your All-in-One Day to Day Life Assistant.
 * Powered by Spring Boot WebFlux (Reactive) + MongoDB Reactive.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class MantraSvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(MantraSvcApplication.class, args);
	}

}
