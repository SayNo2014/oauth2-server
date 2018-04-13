## 前言
关于OAuth2.0的理论知识可以参考[理解OAuth 2.0](http://www.ruanyifeng.com/blog/2014/05/oauth_2_0.html "理解OAuth 2.0")，本文不再赘述。

## 简介
本文主要讲解Spring Boot框架下使用（Spring Security + Spring Security OAuth2 + JWT）整合实现OAuth2。

## 授权码模式处理流程图
![](https://www.zhaidehui.com/usr/uploads/2018/04/3200315174.png)

## 项目准备

1.添加依赖
````xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-oauth2</artifactId>
</dependency>
<dependency>
	<groupId>org.mybatis.spring.boot</groupId>
	<artifactId>mybatis-spring-boot-starter</artifactId>
	<version>1.1.1</version>
</dependency>
<dependency>
	<groupId>mysql</groupId>
	<artifactId>mysql-connector-java</artifactId>
</dependency>
<!-- 便于调试 -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-devtools</artifactId>
	<optional>true</optional> <!-- 表示依赖不会传递 -->
</dependency>
````
既然是授权中心必然有需要保护的资源，我们将UserInfo作为被保护的资源。
URL：[http://localhost:8080/user/get_user_info]

## Oauth 2.0 Provider
** Authorization Server **
	1) AuthorizationEndpoint:进行授权的服务，Default URL: /oauth/authorize
	2) TokenEndpoint：获取token的服务，Default URL: /oauth/token
** Resource Server **
	1) 受保护资源UserController：URL: /user/get_user_info
	2) OAuth2AuthenticationProcessingFilter：给带有访问令牌的请求加载认证

## Authorization Server 配置
1、授权服务器需要创建两个配置类，分别继承自`AuthorizationServerConfigurerAdapter`和`WebSecurityConfigurerAdapter`，并重写configure方法，完成自定义授权服务器配置。
2、添加注解`@EnableAuthorizationServer`声明一个认证服务器。
````java
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {
	// 省略...
}
````
2.1、认证服务器配置类`AuthorizationServerConfig`，继承自`AuthorizationServerConfigurerAdapter`。
	a. authorizationCodeServices#AuthorizationCodeServices：用于定义授权码模式下认证服务器生成的授权码
		保存在数据库还是内存中，本例中采用保存在内存中。
	b. authenticationManager#AuthenticationManager ：注解注入AuthenticationManager用于开启密码授权。
	c. dataSource#DataSource : 注解注入数据源信息，用于从指定的数据源中获取授权客户端信息。
	d. tokenServices#DefaultTokenServices：定义默认的Token生成类。
	e. accessTokenConverter#JwtAccessTokenConverter ：自定义JWT#Token携带的信息和签名秘钥。
	f. tokenStore#JwtTokenStore：定义token的存储方式，JWT模式下可以不保存token。
````java
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
		public OAuth2AccessToken enhance(
			OAuth2AccessToken accessToken, 
			OAuth2Authentication authentication) {
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
````

2.2、重写`AuthorizationServerConfig#configure(AuthorizationServerEndpointsConfigurer endpoints)`方法，配置授权码保存方式（AuthorizationCodeServices）以及自定义的token生成配置（JwtAccessTokenConverter）。
** xhyg.oauth.config.AuthorizationServerConfig#configure(AuthorizationServerEndpointsConfigurer)**
````java
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
````

2.3、重写`AuthorizationServerConfig#configure(AuthorizationServerSecurityConfigurer security)`方法，配置自定义安全约束。
**xhyg.oauth.config.AuthorizationServerConfig.configure(AuthorizationServerSecurityConfigurer)**
````java
@Override
public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
	security.tokenKeyAccess("permitAll()").checkTokenAccess("isAuthenticated");
	// 让/oauth/token支持client_id以及client_secret作登录认证
	security.allowFormAuthenticationForClients();
}
````

2.4、重写`AuthorizationServerConfig#configure(ClientDetailsServiceConfigurer clients)`方法，配置客户端保存的数据源信息。
** xhyg.oauth.config.AuthorizationServerConfig.configure(ClientDetailsServiceConfigurer)**
````java
@Override
public void configure(
		ClientDetailsServiceConfigurer clients) throws Exception {
	clients.jdbc(dataSource);
}
````
3、认证服务器安全配置类`SecurityConfig`，继承自`WebSecurityConfigurerAdapter`,用于配置认证用户加密规则，认证用户获取规则（数据库或者内存）。
** xhyg.oauth.config.SecurityConfig**
````java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private UserDetailsService userDetailsService;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
		auth.authenticationProvider(authenticationProvider());
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}

	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}
}
````
4、添加注解`@EnableResourceServer`声明资源服务器（本例中认证服务器和资源服务器使用同一个项目，生产环境下可拆分），资源服务器配置类`ResourceServerConfig`继承自`ResourceServerConfigurerAdapter`可配置对于资源的安全访问策略。
**xhyg.oauth.config.ResourceServerConfig**
````java
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.requestMatcher(new OAuthRequestedMatcher()).authorizeRequests().antMatchers(HttpMethod.OPTIONS).permitAll()
				.anyRequest().authenticated();
	}

	private static class OAuthRequestedMatcher implements RequestMatcher {
		public boolean matches(HttpServletRequest request) {
			String auth = request.getHeader("Authorization");
			// 自定义token验证
			boolean haveOauth2Token = (auth != null);
			boolean haveAccessToken = request.getParameter("access_token") != null;
			return haveOauth2Token || haveAccessToken;
		}
	}
}
````

5、`application.properties`中配置了jwt签名秘钥和数据库连接信息。
**application.properties**
````text
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
spring.application.name=auth-server

spring.datasource.url=jdbc:mysql://localhost:3306/auth?characterEncoding=utf8&useSSL=true
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.jdbc.Driver

server.port=8080

jwt-single-key=b77b9f08
````

6、定义自定义`DetailsService`实现类`Oauth2UserDetailsService`，用于获取用户信息。
** xhyg.oauth.user.Oauth2UserDetailsService**
````java
@Service("userDetailsService")
public class Oauth2UserDetailsService implements UserDetailsService {

	@Autowired
	private UserMapper userMapper;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		SysUser user = userMapper.findByUserName(username);
		if (user == null) {
			throw new UsernameNotFoundException(username);
		}
		return User
				.withUsername(user.getUsername())
				.password(user.getPassword())
				.authorities(user.getAuthority())
				.roles("USER")
				.build();
	}
}
````

7、定义受保护资源Controller。
**xhyg.oauth.user.UserController**
````java
@Controller
@RequestMapping("/user")
public class UserController {
	
	@RequestMapping("/get_user_info")
	public @ResponseBody UserInfo getUserInfo(@RequestHeader("Authorization") String token) {
		Jwt jwt = JwtHelper.decode(token);
		System.out.println(jwt.getClaims());
		UserInfo userInfo = new UserInfo();
		userInfo.setUserName("xhyg");
		userInfo.setPhone("18973221341");
		return userInfo;
	}
	
}
````
8、关于Mybatis以及Mapper配置文件、SQL和源码此处省略，参考源码。

> 本例中的配置仅供演示用，实际开发中参考Spring Doc进行详细配置。

## 认证流程
1、直接访问受保护资源，直接跳转到登录页面。
![](https://www.zhaidehui.com/usr/uploads/2018/04/3587522014.png)

2、访问URL[http://localhost:8080/oauth/authorize?client_id=client&response_type=code] 获取授权码，服务器接收请求后自动跳转到授权服务器的登录页面，提示用户登录并授权。
![](https://www.zhaidehui.com/usr/uploads/2018/04/2629763151.png)
![](https://www.zhaidehui.com/usr/uploads/2018/04/216832657.png)
用户授权后，会自动跳转到客户端定义好的重定向地址。
![](https://www.zhaidehui.com/usr/uploads/2018/04/1015698353.png)
![](https://www.zhaidehui.com/usr/uploads/2018/04/2646469859.png)

3、通过授权码向授权服务器申请access_token。
![](https://www.zhaidehui.com/usr/uploads/2018/04/3351976985.png)

4、通过access_token访问受保护资源。
![](https://www.zhaidehui.com/usr/uploads/2018/04/4013630658.png)

## Spring Security OAth2登录源码流程图
![](https://www.zhaidehui.com/usr/uploads/2018/04/894266923.png)https://raw.githubusercontent.com/longfeizheng/longfeizheng.github.io/master/images/security/spring-security-oatuh202.pn

## 源码

-- 

## 参考文献
- [理解OAuth 2.0](http://www.ruanyifeng.com/blog/2014/05/oauth_2_0.html "理解OAuth 2.0")
- [从零开始的Spring Security Oauth2](http://blog.didispace.com/spring-security-oauth2-xjf-1/ "从零开始的Spring Security Oauth2")
- [Spring cloud oauth2.0学习总结](https://blog.csdn.net/j754379117/article/details/70175198 "Spring cloud oauth2.0学习总结")
- [Spring Security源码分析十：初识Spring Security OAuth2](https://longfeizheng.github.io/2018/01/20/Spring-Security%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90%E5%8D%81-%E5%88%9D%E8%AF%86Spring-Security-OAuth2/#tokenendpoint "Spring Security源码分析十：初识Spring Security OAuth2")
- [Spring cloud oauth2.0的源码解析与实践Demo](https://blog.csdn.net/j754379117/article/details/70176974 "Spring cloud oauth2.0的源码解析与实践Demo")


