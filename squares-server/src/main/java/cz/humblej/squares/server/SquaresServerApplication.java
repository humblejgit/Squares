package cz.humblej.squares.server;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import cz.humblej.squares.server.metadata.MetadataProperties;
import cz.humblej.identity.server.security.OidcProperties;

@SpringBootApplication(scanBasePackages = {"cz.humblej.squares.server", "cz.humblej.identity.server"})
@EnableConfigurationProperties({MetadataProperties.class, OidcProperties.class})
public class SquaresServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SquaresServerApplication.class, args);
    }

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }
}
