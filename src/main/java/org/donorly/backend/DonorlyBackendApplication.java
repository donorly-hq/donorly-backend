package org.donorly.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DonorlyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DonorlyBackendApplication.class, args);
	}

}
