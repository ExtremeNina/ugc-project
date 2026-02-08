package com.example.onlyone.Configuration;

import com.example.onlyone.Entity.CustomOAuth2User;
import com.example.onlyone.Exception.ExceptionHandlingFilter;
import com.example.onlyone.Service.ServiceImpl.OAuth2UserServiceImpl;
import com.example.onlyone.Utils.JwtAuthenticationFilter;
import com.example.onlyone.Utils.OAuth2AuthenticationFailureHandler;
import com.example.onlyone.Utils.OAuth2SuccessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;


@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "login",
                                "/api/auth/**",
                                "/ws",
                                "/api/community/Recommend",
                                "user/api/Interaction/is-liked",
                                "user/api/Interaction/count",
                                "/api/comment/{articleId}",
                                "/oauth2/**",
                                "/error",
                                "/api/articles/{articleId}",
                                "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/dynamic/**").hasRole("USER")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN","SUPER_ADMIN")
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(new OAuth2SuccessHandler())
                        .failureHandler(new OAuth2AuthenticationFailureHandler())
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService()) // 使用自定义的OAuth2UserService
                        )
                )
                //先使用jwtAuthenticationFilter检验是否用户是否处于登录状态，不在登录状态则执行登录请求
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 预检请求缓存时间

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ExceptionHandlingFilter exceptionHandlingFilter() {
        return new ExceptionHandlingFilter();
    }

    @Bean
    public OAuth2AuthenticationFailureHandler authenticationFailureHandler() {return new OAuth2AuthenticationFailureHandler();}

    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {return new OAuth2SuccessHandler();}

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        return new OAuth2UserServiceImpl();
    }
}
