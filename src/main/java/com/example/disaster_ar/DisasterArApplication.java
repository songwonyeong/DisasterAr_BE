package com.example.disaster_ar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.example.disaster_ar.domain.v4")
public class DisasterArApplication {

	public static void main(String[] args) {
		SpringApplication.run(DisasterArApplication.class, args);
	}

}
