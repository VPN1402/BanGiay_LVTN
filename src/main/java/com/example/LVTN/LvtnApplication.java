package com.example.LVTN;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling // kích hoạt hẹn giờ
public class LvtnApplication {

	public static void main(String[] args) {
		SpringApplication.run(LvtnApplication.class, args);
	}

}
