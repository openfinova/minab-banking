package com.openfinova.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "bankingAuditorAware")
public class OpenfinovaBankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenfinovaBankingApplication.class, args);
    }

}
