package com.example.demo.config;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalDevJwtConfig {

    @Bean("localDevEcKey")
    ECKey localDevEcKey() throws Exception {
        return new ECKeyGenerator(Curve.P_256).keyID("local-dev-key").generate();
    }
}
