package com.stockalert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockAlertApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockAlertApiApplication.class, args);
	}

}
