package no.lyse.plattform.oauth2playground.authorizationserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OidcUserInfoService {

    private final Map<String, Map<String, Object>> userInfo = new HashMap<>();

    public OidcUserInfo loadUser(String username) {
        // TODO we can probably store the OidcUserInfo directly in the userInfoRepository
        return new OidcUserInfo(this.userInfo.get(username));
    }

    /**
     * Iser by the DataInitializer.
     */
    void addUser(String id, Map<String, Object> userInfo) {
       this.userInfo.put(id, userInfo);
    }
}
