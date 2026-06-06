package com.example.demo.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

@RestController
@Profile("local")
@RequestMapping("/dev")
public class DevTokenController {

    private final ECKey localDevEcKey;

    public DevTokenController(@Qualifier("localDevEcKey") ECKey localDevEcKey) {
        this.localDevEcKey = localDevEcKey;
    }

    @GetMapping("/token")
    public Map<String, String> issueToken(
            @RequestParam(defaultValue = "test-student-1") String sub,
            @RequestParam(defaultValue = "student@test.local") String email
    ) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("email", email)
                .expirationTime(new Date(System.currentTimeMillis() + 3_600_000))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(localDevEcKey.getKeyID()).build(),
                claims
        );
        jwt.sign(new ECDSASigner(localDevEcKey));
        String token = jwt.serialize();
        return Map.of(
                "token", token,
                "header", "Authorization: Bearer " + token,
                "sub", sub,
                "email", email
        );
    }
}
