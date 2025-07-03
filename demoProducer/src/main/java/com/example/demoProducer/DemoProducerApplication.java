package com.example.demoProducer;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.newrelic.api.agent.Trace;

@SpringBootApplication
public class DemoProducerApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DemoProducerApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);
	}
}
