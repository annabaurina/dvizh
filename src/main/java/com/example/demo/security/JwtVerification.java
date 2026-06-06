package com.example.demo.security;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtVerification {
    private static final String projectURL = "https://yobtiqmzqbtjmhkkotja.supabase.co/auth/v1/.well-known/jwks.json";
    private JWKSet jwkSet;
    private final HttpClient httpClient;

    public JwtVerification() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build();
    }

    private JWKSet getJwkSet() throws Exception {
        if (jwkSet == null){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(projectURL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new Exception("Ошибка загрузки ключа" + response.statusCode());
            }
            jwkSet = JWKSet.parse(response.body());
        }
        return jwkSet;
    }

    public SignedJWT verification(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWKSet jwkSet = getJwkSet();
        String keyId = signedJWT.getHeader().getKeyID();

        if (keyId == null) {
            throw new SecurityException("Key ID нет");
        }

        JWK jwk =  jwkSet.getKeyByKeyId(keyId);
        if (jwk == null) {
            throw new SecurityException("Нет ключа верификации");
        }

        JWSVerifier verifier = switch (jwk) {
            case RSAKey rsaKey -> new RSASSAVerifier(rsaKey);
            case ECKey ecKey -> new ECDSAVerifier(ecKey);
            default -> throw new SecurityException("Неподдерживаемый тип ключа JWK: " + jwk.getKeyType());
        };
        if (!signedJWT.verify(verifier)) {
            throw new SecurityException("Неверная подпись");
        }

        Date time = signedJWT.getJWTClaimsSet().getExpirationTime();
        if (time != null && time.before(new Date())) {
            throw new SecurityException("Просрочен JWT");
        }
        return signedJWT;
    }

    public String getAuthId(String token) throws Exception {
        SignedJWT signedJWT = verification(token);
        return signedJWT.getJWTClaimsSet().getSubject();
    }
}
