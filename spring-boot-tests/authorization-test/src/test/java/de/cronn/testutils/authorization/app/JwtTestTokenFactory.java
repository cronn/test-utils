package de.cronn.testutils.authorization.app;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Component
public class JwtTestTokenFactory {

	private final JwtEncoder encoder;

	public JwtTestTokenFactory() {
		RSAKey rsaKey = new RSAKey.Builder(JwtKeys.publicKey())
			.privateKey(JwtKeys.privateKey())
			.keyID("test-key")
			.build();
		this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
	}

	public String tokenForRoles(Role... roles) {
		List<String> roleNames = Arrays.stream(roles).map(Role::name).toList();
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer("test-issuer")
			.subject("test-subject-" + UUID.randomUUID())
			.issuedAt(now)
			.expiresAt(now.plus(1, ChronoUnit.HOURS))
			.claim("roles", roleNames)
			.build();
		JwsHeader header = JwsHeader.with(() -> JwsAlgorithms.RS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
