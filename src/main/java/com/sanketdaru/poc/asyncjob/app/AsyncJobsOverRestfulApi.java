package com.sanketdaru.poc.asyncjob.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;


@SpringBootApplication(scanBasePackages = { "com.sanketdaru.poc.asyncjob" }, 
	exclude = { SecurityAutoConfiguration.class })
public class AsyncJobsOverRestfulApi {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(AsyncJobsOverRestfulApi.class);
		application.run(args);
	  }
}
