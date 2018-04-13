package xhyg.oauth.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

	/**
	 * 身份认证
	 */
	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
    private DataSource dataSource;
    
	@Value("${jwt-single-key}")
	private String signingKey;
	
	@Bean
	public AuthorizationCodeServices authorizationCodeServices() {
		return new InMemoryAuthorizationCodeServices();
	}
	
	@Bean
	public JwtTokenStore tokenStore() {
		return new JwtTokenStore(accessTokenConverter());
	}

	 @Bean
	 public JwtAccessTokenConverter accessTokenConverter() {
		 JwtAccessTokenConverter converter = new JwtAccessTokenConverter() {
			 @Override
			public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
				 String userName = authentication.getName();
				 Map<String, Object> additionalInformation = new HashMap<>();
				 additionalInformation.put("userName", userName);
				 additionalInformation.put("token_info_test", "test");
				 ((DefaultOAuth2AccessToken)accessToken).setAdditionalInformation(
						 additionalInformation);
				 return super.enhance(accessToken, authentication);
			}
		 };
		 converter.setSigningKey(signingKey);
		 return converter;
	 }

	// 默认token产生实现类
	@Bean
	public DefaultTokenServices tokenServices() {
		DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
		defaultTokenServices.setTokenEnhancer(accessTokenConverter());
		defaultTokenServices.setTokenStore(tokenStore());
		defaultTokenServices.setSupportRefreshToken(true);
		return defaultTokenServices;
	}
		
	@Override
	public void configure(
			AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		endpoints.authorizationCodeServices(
				authorizationCodeServices());
		endpoints.tokenStore(tokenStore()).authenticationManager(authenticationManager)
				// 允许 GET、POST 请求获取 token，即访问端点：oauth/token
				.allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST);
		// jwt
		endpoints.tokenEnhancer(accessTokenConverter());
	}

	@Override
	public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
		security.tokenKeyAccess("permitAll()").checkTokenAccess("isAuthenticated");
		// 让/oauth/token支持client_id以及client_secret作登录认证
		security.allowFormAuthenticationForClients();
	}

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.jdbc(dataSource);
	}
	
}
