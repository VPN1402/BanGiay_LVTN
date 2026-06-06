package com.example.LVTN;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LvtnApplication {

	public static void main(String[] args) {
		SpringApplication.run(LvtnApplication.class, args);
	}

}
