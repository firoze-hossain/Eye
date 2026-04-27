package com.roze.trackeyecentral;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrackeyeCentralApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackeyeCentralApplication.class, args);
    }

}
