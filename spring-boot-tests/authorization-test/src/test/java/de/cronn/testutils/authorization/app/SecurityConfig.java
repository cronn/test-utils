package de.cronn.testutils.authorization.app;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.addFilterBefore(new SimulatedStatusFilter(), SecurityContextHolderFilter.class)
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/public").permitAll()
				.requestMatchers("/actuator/**").permitAll()
				// see spring's org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
				.requestMatchers("/error").permitAll()
				.requestMatchers("/authenticated").authenticated()
				.requestMatchers("/any-role").hasAnyRole(Stream.of(Role.values()).map(Role::name).toArray(String[]::new))
				.requestMatchers("/admin").hasRole(Role.ADMIN.name())
				.requestMatchers("/user").hasAnyRole(Role.ADMIN.name(), Role.USER.name())
				.requestMatchers("/guest-only").hasRole(Role.GUEST.name())
				.requestMatchers("/locked").denyAll()
				.requestMatchers("/items/**").hasRole(Role.ADMIN.name())
				.requestMatchers("/teapot").hasRole(Role.USER.name())
				.requestMatchers("/not-found").hasRole(Role.USER.name())
				.requestMatchers("/server-error").hasRole(Role.USER.name())
				.requestMatchers("/any-method").hasRole(Role.ADMIN.name())
				.anyRequest().denyAll())
			.httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint()))
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				.dPoP(Customizer.withDefaults()))
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(authenticationEntryPoint())
				.accessDeniedHandler(accessDeniedHandler()));
		return http.build();
	}

	@Bean
	public InMemoryUserDetailsManager userDetailsService() {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		return new InMemoryUserDetailsManager(
			User.withUsername("admin-user").password(encoder.encode("admin-password")).roles(Role.ADMIN.name()).build(),
			User.withUsername("regular-user").password(encoder.encode("user-password")).roles(Role.USER.name()).build(),
			User.withUsername("guest-user").password(encoder.encode("guest-password")).roles(Role.GUEST.name()).build(),
			User.withUsername("no-role-user").password(encoder.encode("no-role-password")).roles().build()
		);
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withPublicKey(JwtKeys.publicKey()).build();
	}

	private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::extractAuthorities);
		return jwt -> (JwtAuthenticationToken) converter.convert(jwt);
	}

	private static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
		List<String> roles = jwt.getClaimAsStringList("roles");
		if (roles == null) {
			return List.of();
		}
		return roles.stream()
			.map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
			.toList();
	}

	private AuthenticationEntryPoint authenticationEntryPoint() {
		return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
	}

	private AccessDeniedHandler accessDeniedHandler() {
		return (request, response, accessDeniedException) ->
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}
}
