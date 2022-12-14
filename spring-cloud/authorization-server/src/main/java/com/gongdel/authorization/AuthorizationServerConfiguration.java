package com.gongdel.authorization;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpoint;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@EnableAuthorizationServer
@Configuration
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

	AuthenticationManager authenticationManager;
	KeyPair keyPair;
	boolean jwtEnabled;

	public AuthorizationServerConfiguration(
			AuthenticationConfiguration authenticationConfiguration,
			KeyPair keyPair,
			@Value("${security.oauth2.authorizationserver.jwt.enabled:true}") boolean jwtEnabled) throws Exception {

		this.authenticationManager = authenticationConfiguration.getAuthenticationManager();
		this.keyPair = keyPair;
		this.jwtEnabled = jwtEnabled;
	}

	@Override
	public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
		super.configure(security);
	}

	@Override
	// API??? ?????? ??????????????? ????????? ??????
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.inMemory()
				.withClient("reader")
				.authorizedGrantTypes("code", "authorization_code", "implicit", "password")
				.redirectUris("http://my.redirect.uri")
				.secret("{noop}secret") // {noop} ??? password ????????? ????????????, ??????????????? ??????????????? ??????( NoOpPasswordEncoder)
				.scopes("product:read") // https://cheese10yun.github.io/spring-oauth2-provider/
				.accessTokenValiditySeconds(600_000_000)
				.and()
				.withClient("writer")
				.authorizedGrantTypes("code", "authorization_code", "implicit", "password")
				.redirectUris("http://my.redirect.uri")
				.secret("{noop}secret")
				.scopes("product:read", "product:write")
				.accessTokenValiditySeconds(600_000_000)
				.and()
				.withClient("noscopes")
				.authorizedGrantTypes("code", "authorization_code", "implicit", "password")
				.redirectUris("http://my.redirect.uri")
				.secret("{noop}secret")
				.scopes("none")
				.accessTokenValiditySeconds(600_000_000);
	}

	// ?????? ????????? ??????
	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		endpoints
				.authenticationManager(this.authenticationManager)
				.tokenStore(tokenStore());

		if (this.jwtEnabled) {
			endpoints
					.accessTokenConverter(accessTokenConverter());
		}
	}

	@Bean
	public TokenStore tokenStore() {
		if (this.jwtEnabled) {
			return new JwtTokenStore(accessTokenConverter());
		} else {
			return new InMemoryTokenStore();
		}
	}

	@Bean
	public JwtAccessTokenConverter accessTokenConverter() {
		JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
		converter.setKeyPair(this.keyPair);

		DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
		accessTokenConverter.setUserTokenConverter(new SubjectAttributeUserTokenConverter());
		converter.setAccessTokenConverter(accessTokenConverter);

		return converter;
	}
}

@Configuration
class UserConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.authorizeRequests()
					.antMatchers("/actuator/**").permitAll() // url ??????
					.mvcMatchers("/.well-known/jwks.json").permitAll() // mvc ??????
					.anyRequest().authenticated()
				.and()
				.httpBasic()
				.and()
				.csrf().ignoringRequestMatchers(request -> "/introspect".equals(request.getRequestURI()));
	}

	@Bean
	@Override
	public UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager(
				User.withDefaultPasswordEncoder()
						.username("gongdel")
						.password("password")
						.roles("USER")
						.build());
	}
}

@FrameworkEndpoint
class IntrospectEndpoint {
	TokenStore tokenStore;

	public IntrospectEndpoint(TokenStore tokenStore) {
		this.tokenStore = tokenStore;
	}

	@PostMapping("/introspect")
	@ResponseBody
	public Map<String, Object> introspect(@RequestParam("token") String token) {
		OAuth2AccessToken accessToken = this.tokenStore.readAccessToken(token);
		Map<String, Object> attributes = new HashMap<>();
		if (accessToken == null || accessToken.isExpired()) {
			attributes.put("active", false);
			return attributes;
		}

		OAuth2Authentication authentication = this.tokenStore.readAuthentication(token);

		attributes.put("active", true);
		attributes.put("exp", accessToken.getExpiration().getTime());
		attributes.put("scope", accessToken.getScope().stream().collect(Collectors.joining(" ")));
		attributes.put("sub", authentication.getName());

		return attributes;
	}
}

@FrameworkEndpoint
class JwkSetEndpoint {
	KeyPair keyPair;

	public JwkSetEndpoint(KeyPair keyPair) {
		this.keyPair = keyPair;
	}

	@GetMapping("/.well-known/jwks.json")
	@ResponseBody
	public Map<String, Object> getKey() {
		RSAPublicKey publicKey = (RSAPublicKey) this.keyPair.getPublic();
		RSAKey key = new RSAKey.Builder(publicKey).build();
		return new JWKSet(key).toJSONObject();
	}
}

@Configuration
class KeyConfig {

	@Bean
	KeyPair keyPair() {
		try {
			String modulus =
					"18044398961479537755088511127417480155072543594514852056908450877656126120801808993616738273349107491806340290040410660515399239279742407357192875363433659810851147557504389760192273458065587503508596714389889971758652047927503525007076910925306186421971180013159326306810174367375596043267660331677530921991343349336096643043840224352451615452251387611820750171352353189973315443889352557807329336576421211370350554195530374360110583327093711721857129170040527236951522127488980970085401773781530555922385755722534685479501240842392531455355164896023070459024737908929308707435474197069199421373363801477026083786683";
			String exponent = "65537";
			// https://zamcom.tistory.com/168
			RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(new BigInteger(modulus), new BigInteger(exponent));

			String privateExponent =
					"3851612021791312596791631935569878540203393691253311342052463788814433805390794604753109719790052408607029530149004451377846406736413270923596916756321977922303381344613407820854322190592787335193581632323728135479679928871596911841005827348430783250026013354350760878678723915119966019947072651782000702927096735228356171563532131162414366310012554312756036441054404004920678199077822575051043273088621405687950081861819700809912238863867947415641838115425624808671834312114785499017269379478439158796130804789241476050832773822038351367878951389438751088021113551495469440016698505614123035099067172660197922333993";
			RSAPrivateKeySpec privateKeySpec = new RSAPrivateKeySpec(new BigInteger(modulus),
					new BigInteger(privateExponent));
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return new KeyPair(keyFactory.generatePublic(publicSpec), keyFactory.generatePrivate(privateKeySpec));
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}

/**
 * ????????? ?????? ????????? user parameter ??? ??????  custom name ??? ???????????? ?????? ????????? ????????? ????????? ????????????.
 * ??????????????? user_name ????????? ???????????????,  jwt ????????? ????????? ?????? ??????(sub)??? ???????????? ??? ??????
 */
class SubjectAttributeUserTokenConverter extends DefaultUserAuthenticationConverter {

	@Override
	public Map<String, ?> convertUserAuthentication(Authentication authentication) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("sub", authentication.getName());

		if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
			response.put(AUTHORITIES, AuthorityUtils.authorityListToSet(authentication.getAuthorities()));
		}

		return response;
	}
}