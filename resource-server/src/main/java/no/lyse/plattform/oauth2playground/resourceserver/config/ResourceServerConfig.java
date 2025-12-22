package no.lyse.plattform.oauth2playground.resourceserver.config;

import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.actuate.autoconfigure.web.exchanges.HttpExchangesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@EnableWebFluxSecurity
@Configuration(proxyBeanMethods = false)
public class ResourceServerConfig {
    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        // @formatter:off
        http
            .authorizeExchange(exchange -> exchange
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/quote", "/quote/**", "/quotes").hasAuthority("SCOPE_message.read")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
                .jwt(Customizer.withDefaults())
            )
            .authenticationManager(authentication -> Mono.just(authentication).log())
        ;

        http.cors(Customizer.withDefaults());
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

        converter.setJwtGrantedAuthoritiesConverter((jwt) -> {

                Flux<GrantedAuthority> scopes = Flux.fromIterable(jwt.getClaimAsStringList("scope"))
                    .map((role) -> "SCOPE_" + role)
                    .map(SimpleGrantedAuthority::new);

                Flux<GrantedAuthority> roles = Flux.fromIterable(jwt.getAudience())
                    .map((role) -> "AUD_" + role)
                    .map(SimpleGrantedAuthority::new);

                return Flux.concat(scopes, roles);
            }
        );

        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", new org.springframework.web.cors.CorsConfiguration().applyPermitDefaultValues());
        return source;
    }

}
