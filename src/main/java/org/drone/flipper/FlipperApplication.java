package org.drone.flipper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class FlipperApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlipperApplication.class, args);
    }
}
