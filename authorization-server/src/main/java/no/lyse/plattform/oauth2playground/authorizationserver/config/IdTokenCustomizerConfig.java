package no.lyse.plattform.oauth2playground.authorizationserver.config;

import no.lyse.plattform.oauth2playground.authorizationserver.service.OidcUserInfoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * See <a href="https://docs.spring.io/spring-authorization-server/reference/guides/how-to-userinfo.html">Spring Authorization Server Documentation</a>
 */
@Configuration
public class IdTokenCustomizerConfig {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(OidcUserInfoService userInfoService) {
        return (context) -> {
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                OidcUserInfo userInfo = userInfoService.loadUser(
                    context.getPrincipal().getName());
                context.getClaims().claims(claims ->
                    claims.putAll(userInfo.getClaims()));
            }
        };
    }

}
