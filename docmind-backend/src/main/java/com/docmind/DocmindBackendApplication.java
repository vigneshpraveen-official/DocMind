package com.docmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DocmindBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocmindBackendApplication.class, args);
	}

}
