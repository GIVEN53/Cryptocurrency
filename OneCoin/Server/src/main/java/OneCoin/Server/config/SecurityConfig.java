package OneCoin.Server.config;

import OneCoin.Server.config.auth.filter.JwtAuthenticationFilter;
import OneCoin.Server.config.auth.filter.JwtVerificationFilter;
import OneCoin.Server.config.auth.handler.*;
import OneCoin.Server.config.auth.jwt.JwtTokenizer;
import OneCoin.Server.config.auth.userdetails.Oauth2UserDetailService;
import OneCoin.Server.config.auth.utils.CustomAuthorityUtils;
import OneCoin.Server.user.mapper.UserMapper;
import OneCoin.Server.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity(debug = true)
public class SecurityConfig {
    private final JwtTokenizer jwtTokenizer;
    private final CustomAuthorityUtils customAuthorityUtils;
    private final UserService userService;
    private final UserMapper userMapper;
    @Value("${spring.client.ip}")
    private String clientURL;

    public SecurityConfig(JwtTokenizer jwtTokenizer, CustomAuthorityUtils customAuthorityUtils, UserService userService, UserMapper userMapper) {
        this.jwtTokenizer = jwtTokenizer;
        this.customAuthorityUtils = customAuthorityUtils;
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers().frameOptions().sameOrigin() // ?????? ??????????????? ???????????? request ??? ????????? ???????????? ??????
                .and()
                .csrf().disable()        // ?????? disable
                .cors(Customizer.withDefaults())    // corsConfigurationSource ?????? ??????
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)     // ?????? ???????????? ??????
                .and()
                .formLogin().disable()
                .httpBasic().disable()   // jwt ????????? ????????????
                .exceptionHandling()
                .authenticationEntryPoint(new UserAuthenticationEntryPoint())  // ?????????????????? ?????? ??????
                .accessDeniedHandler(new UserAccessDeniedHandler())     // ?????? ?????? ????????? ??????
                .and()
                .apply(new CustomFilterConfigurer())    // ????????? ?????? ??????
                .and()
                .authorizeHttpRequests(authorize -> authorize
                        .antMatchers(HttpMethod.GET, "/api/order/**").hasRole("USER")
                        .antMatchers(HttpMethod.POST, "/api/order/**").hasRole("USER")
                        .antMatchers(HttpMethod.GET, "/ws/chat/**").permitAll()
                        .antMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .antMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("USER")
                        .antMatchers(HttpMethod.GET, "/api/users").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/users/**").permitAll()
                        .antMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("USER")
                        .antMatchers(HttpMethod.POST, "/api/admin/**").hasRole("ADMIN")
                        .antMatchers(HttpMethod.DELETE, "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .oauth2Login()
                .successHandler(new UserOAuth2SuccessHandler(jwtTokenizer, customAuthorityUtils, userService, userMapper, clientURL))
                .userInfoEndpoint()     // Oauth2 ????????? ?????? ??? userInfo ???????????????(userInfo ?????????)
                .userService(new Oauth2UserDetailService(userService, jwtTokenizer, customAuthorityUtils, userMapper));  // ????????? ???????????? ????????? ????????? ????????? ???????????? ????????? ??????
        return http.build();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // Cors ?????? ??????
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));   // ????????? ??????
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));  // ?????? ????????? ??????
        configuration.setAllowedHeaders(List.of("*", "connection", "upgrade"));     // ?????? ?????? ?????? ??????
        configuration.setAllowCredentials(true);        // ???????????? ?????? ??????
        configuration.setExposedHeaders(List.of("*"));       // ?????? ?????? ?????? ??????

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();   // CorsConfigurationSource ?????? ?????????
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ????????? ?????? ??????
    public class CustomFilterConfigurer extends AbstractHttpConfigurer<CustomFilterConfigurer, HttpSecurity> {
        @Override
        public void configure(HttpSecurity builder) throws Exception {
            AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);

            JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager, jwtTokenizer, userService);  // JwtAuthenticationFilter ??? ??????????????? JwtAuthenticationFilter ?????? ???????????? AuthenticationManager ??? JwtTokenizer ??? DI
            jwtAuthenticationFilter.setFilterProcessesUrl("/api/auth/login");          // ????????? url ??????

            // ??????, ?????? ????????? ??????, ??????????????? ????????? ?????? ???????????? ??????, ?????? ?????? ???????????? ?????? ??????????????? DI ?????? ????????? ??????
            jwtAuthenticationFilter.setAuthenticationSuccessHandler(new UserAuthenticationSuccessHandler());
            jwtAuthenticationFilter.setAuthenticationFailureHandler(new UserAuthenticationFailureHandler());

            JwtVerificationFilter jwtVerificationFilter = new JwtVerificationFilter(jwtTokenizer, customAuthorityUtils, userService);

            // ???????????? ????????? ?????? ????????? ??????
            builder.addFilter(jwtAuthenticationFilter)
                    .addFilterAfter(jwtVerificationFilter, JwtAuthenticationFilter.class)
                    .addFilterAfter(jwtVerificationFilter, OAuth2LoginAuthenticationFilter.class);
        }
    }
}
