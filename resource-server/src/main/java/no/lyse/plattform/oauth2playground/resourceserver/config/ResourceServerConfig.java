package no.lyse.plattform.oauth2playground.resourceserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.web.exchanges.HttpExchangesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
                .jwt(jwtSpec-> {
                    //jwtSpec.jwkSetUri("https://login.microsoftonline.com/22ca942f-06c2-4f38-9407-0e447dedbb67/discovery/v2.0/keys?appid=5474f7d9-4282-4083-bfcd-90301246ffb8");
                }
                )
            )
        ;

            return http.build();
        // @formatter:on
    }

    @Bean
    @Profile("azure")
    public ReactiveJwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri, @Value("${spring.security.oauth2.resourceserver.jwt.client-id:null}") String clientId) {
        NimbusReactiveJwtDecoder jwtDecoder = (NimbusReactiveJwtDecoder)
            ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
        OAuth2TokenValidator<Jwt> tokenValidator = new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(), // equivalent of JwtValidators.createDefaultWithIssuer(issuerUri)
//            new JwtIssuerValidator(issuerUri), // the issuer in the token is wrong
            new JwtClaimValidator<String>("appid", (appid) -> Objects.equals(appid, clientId)));
            //new JwtClaimValidator<List<String>>("appid", (aud) -> aud != null && aud.contains(clientId)));
        jwtDecoder.setJwtValidator(tokenValidator);
        return jwtDecoder;
    }

    @Bean
    public ReactiveJwtAuthenticationConverter getJwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter((jwt) -> Flux.fromStream(
            Optional.ofNullable(jwt.getClaimAsStringList("scopes"))
                .orElse(List.of("message.read"))// roles is missing
                .stream()
                .map((role) -> "SCOPE_" + role)
                .map(SimpleGrantedAuthority::new)
        ));

        converter.setPrincipalClaimName("sub");
        return converter;
    }
}
