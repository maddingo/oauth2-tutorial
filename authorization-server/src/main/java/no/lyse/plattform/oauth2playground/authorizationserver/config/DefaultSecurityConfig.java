package no.lyse.plattform.oauth2playground.authorizationserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class DefaultSecurityConfig {

    private final AuthServerConfigProperties authServerConfigProperties;

    public DefaultSecurityConfig(AuthServerConfigProperties authServerConfigProperties) {
        this.authServerConfigProperties = authServerConfigProperties;
    }

    // @formatter:off
    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            )
            .formLogin(withDefaults());
        return http.build();
    }
    // @formatter:on

    // @formatter:off
    @Bean
    UserDetailsService users() {
        InMemoryUserDetailsManager inMemoryUserDetailsManager = new InMemoryUserDetailsManager();

        authServerConfigProperties.getUsers()
            .forEach(userConfig ->
                inMemoryUserDetailsManager.createUser(
                    User.builder()
                        .username(userConfig.getUsername())
                        .password(userConfig.getPassword())
                        .roles(userConfig.getRoles())
                        .build()
                )
            );
        return inMemoryUserDetailsManager;
    }
    // @formatter:on

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
