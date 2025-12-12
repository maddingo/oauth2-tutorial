package no.lyse.plattform.oauth2playground.authorizationserver.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class UserConfig {

    private final AuthServerConfigProperties authServerConfigProperties;

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

//    @Bean
//    public SessionRegistry sessionRegistry() {
//        return new SessionRegistryImpl();
//    }
//
//    @Bean
//    public HttpSessionEventPublisher httpSessionEventPublisher() {
//        return new HttpSessionEventPublisher();
//    }
}
