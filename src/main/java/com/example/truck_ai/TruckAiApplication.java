package com.example.truck_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;



@SpringBootApplication
@EnableAsync
public class TruckAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TruckAiApplication.class, args);
	}

}
