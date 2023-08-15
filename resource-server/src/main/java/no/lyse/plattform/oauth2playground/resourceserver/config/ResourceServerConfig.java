package no.lyse.plattform.oauth2playground.resourceserver.config;

import org.springframework.boot.actuate.autoconfigure.web.exchanges.HttpExchangesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
@Configuration(proxyBeanMethods = false)
@Import(HttpExchangesAutoConfiguration.class)
public class ResourceServerConfig {
    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        // @formatter:off
        http
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/quote/**").hasAuthority("SCOPE_message.read")
                .pathMatchers("/quotes").hasAuthority("SCOPE_message.read")
                .anyExchange().authenticated())
            .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
                .jwt(Customizer.withDefaults()));

            return http.build();
        // @formatter:on
    }
}
