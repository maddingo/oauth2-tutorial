package no.lyse.plattform.oauth2playground.authorizationserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.lyse.plattform.oauth2playground.authorizationserver.config.AuthServerConfigProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserDetailsService userDetailsService;
    private final AuthServerConfigProperties authServerConfigProperties;
    private final OidcUserInfoService oidcUserInfoService;
    
    @Override
    public void run(ApplicationArguments args) {
        CompletableFuture<Void> userDetails = CompletableFuture.runAsync(this::initializeUserDetails);
        CompletableFuture<Void> userInfos = CompletableFuture.runAsync(this::initializeUserInfos);

        try {

            CompletableFuture.allOf(userDetails, userInfos)
                .exceptionally(ex -> {
                    throw new RuntimeException("Failed to initialize data", ex);
                })
                .join();
        } catch (Exception ex) {
            log.error("Failed to initialize data", ex);
        }
    }

    private void initializeUserInfos() {
        authServerConfigProperties.getUsers().forEach(userConfig -> {
            String username = userConfig.getUsername();
            oidcUserInfoService.addUser(username, createUser(userConfig));
        });
    }

    private void initializeUserDetails() {
        if (userDetailsService instanceof InMemoryUserDetailsManager userManager) {
            authServerConfigProperties.getUsers()
                .forEach(userConfig ->
                    userManager.createUser(
                        User.builder()
                            .username(userConfig.getUsername())
                            .password(userConfig.getPassword())
                            .roles(userConfig.getRoles())
                            .build()
                    )
                );
        }
    }

    private static Map<String, Object> createUser(AuthServerConfigProperties.UserConfig userConfig) {
        return OidcUserInfo.builder()
            .subject(userConfig.getUsername())
            .name("First Last")
            .givenName("First")
            .familyName("Last")
            .middleName("Middle")
            .nickname("User")
            .preferredUsername(userConfig.getUsername())
            .profile("https://example.com/" + userConfig.getUsername())
            .picture("https://example.com/" + userConfig.getUsername() + ".jpg")
            .website("https://example.com")
            .email(userConfig.getUsername() + "@example.com")
            .emailVerified(false)
            .gender("female")
            .birthdate("1989-11-09")
            .zoneinfo("Europe/Berlin")
            .locale("en-US")
            .phoneNumber("+49 (033) 123-456-78")
            .phoneNumberVerified(false)
            .claim("address", Map.of("formatted", "Brandenburger Tor\nPariser Platz\n10117 Berlin\nGermany"))
            .updatedAt(ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
            .build()
            .getClaims();
    }

}
