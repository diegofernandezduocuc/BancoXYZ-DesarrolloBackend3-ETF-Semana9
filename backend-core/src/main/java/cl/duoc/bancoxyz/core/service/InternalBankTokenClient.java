package cl.duoc.bancoxyz.core.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InternalBankTokenClient {

    private final RestClient authClient;
    private final String tokenUri;
    private final String clientSecret;

    public InternalBankTokenClient(
            @Value("${bancoxyz.oauth.client-id:${OAUTH_CLIENT_ID:bancoxyz-client}}") String clientId,
            @Value("${bancoxyz.oauth.client-secret:${OAUTH_CLIENT_SECRET:}}") String clientSecret,
            @Value("${bancoxyz.oauth.token-uri:${AUTH_TOKEN_URI:http://localhost:9000/oauth2/token}}") String tokenUri) {
        this.clientSecret = clientSecret;
        this.tokenUri = tokenUri;
        this.authClient = RestClient.builder()
                .defaultHeaders(headers -> headers.setBasicAuth(clientId, clientSecret))
                .build();
    }

    public String token(String scope) {
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Credenciales internas OAuth2 no configuradas");
        }
        var body = new LinkedMultiValueMap<String, String>();
        body.add("grant_type", "client_credentials");
        body.add("scope", scope);
        TokenResponse response = authClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authorization Server no entrego token interno");
        }
        return response.accessToken();
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {}
}
