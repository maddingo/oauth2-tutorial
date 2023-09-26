package no.lyse.plattform.oauth2playground.client.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DelegatingReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationCodeAuthenticationTokenConverter;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {
    @Autowired
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    // @formatter:off
    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http, ServerLogoutSuccessHandler logoutHandler) {
        return http
            .authorizeExchange(authorize ->
                authorize
                    .pathMatchers("/error/**", "/webjars/**", "/", "/index.html", "/favicon.ico", "/_next/**").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/api/auth/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/authorized", "/authorized/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/quote").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/quote11").authenticated()
                    .anyExchange().authenticated()
            )
            .httpBasic(basic -> basic.disable())
            .oauth2Login(withDefaults())
//            .oauth2Login((oauth2Login) ->
//                oauth2Login
//                    .authenticationConverter(authConverter())
//                    .authenticationManager(authManager())
//            )
            .oauth2Client(withDefaults())
            .logout(logout ->
                logout.logoutSuccessHandler(logoutHandler)
            )
            .csrf((csrf) -> csrf.disable())
            .build();
    }

//    private ReactiveAuthenticationManager authManager() {
//        return new DelegatingReactiveAuthenticationManager(
//            new org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeReactiveAuthenticationManager(clientRegistrationRepository),
//            new org.springframework.security.oauth2.client.authentication.OAuth2LoginReactiveAuthenticationManager(clientRegistrationRepository)
//        ));
//    }

//    @Bean
//    ServerAuthenticationConverter authConverter() {
//        return new ServerOAuth2AuthorizationCodeAuthenticationTokenConverter(clientRegistrationRepository);
//    }
    // @formatter:on

    @Bean
    CorsWebFilter corsConfigurationSource(
        @Value("${client-app.cors.allowed-origins}") List<String> allowedOrigins,
        @Value("${client-app.cors.allowed-methods}") List<String> allowedMethods
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(allowedMethods);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    @Bean
    ServerLogoutSuccessHandler oidcLogoutSuccessHandler(
        @Value("${client-app.logout-redirect-uri}") URI logoutRedirectUri
    ) {
        OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler =
            new OidcClientInitiatedServerLogoutSuccessHandler(this.clientRegistrationRepository);

        // Set the location that the End-User's User Agent will be redirected to
        // after the logout has been performed at the Provider
        oidcLogoutSuccessHandler.setLogoutSuccessUrl(logoutRedirectUri);

        return oidcLogoutSuccessHandler;
    }
}
