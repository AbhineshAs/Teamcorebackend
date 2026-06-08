package com.example.crm;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class CrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrmApplication.class, args);
	}
	@PostConstruct
	public void init() {
	    // Setting to IST (India Standard Time) which is UTC+5:30
	    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
	    System.out.println("--- APPLICATION SYNCED TO LOCAL TIME: " + java.time.LocalDateTime.now() + " ---");
	}

}
