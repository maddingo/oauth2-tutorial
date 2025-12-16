package no.lyse.plattform.oauth2playground.authorizationserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties("auth-server")
@Data
public class AuthServerConfigProperties {
    private String issuer;
    private List<UserConfig> users;
    private CorsConfiguration cors;

    @Data
    public static class UserConfig {
        private String username;
        private String password;
        private String[] roles;
    }
}

