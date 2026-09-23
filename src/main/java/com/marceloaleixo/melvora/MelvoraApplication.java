package com.marceloaleixo.melvora;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MelvoraApplication {

	public static void main(String[] args) {
		SpringApplication.run(MelvoraApplication.class, args);
	}

}
