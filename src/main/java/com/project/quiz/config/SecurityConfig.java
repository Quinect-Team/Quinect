package com.project.quiz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.project.quiz.service.CustomOAuth2UserService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final OAuth2SuccessHandler oAuth2SuccessHandler;
	private final CustomLoginSuccessHandler customLoginSuccessHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService)
			throws Exception {
		http.authorizeHttpRequests((auth) -> auth
				// 1. 허용할 URL: 인덱스, 로그인, 회원가입, 정적 리소스(css, js, 이미지)
				.requestMatchers("/", "/index", "/login", "/signup/**", "/register", "/api/user/check-email",
						"/guest/**", "/css/**", "/js/**", "/images/**", "/vendor/**", "/joinroom", "/waitroom/**",
						"/ws/**", "/forgot/**", "/send", "/verify", "/check", "/reset", "/success", "/img/**",
						"/quiz/**", "/quiz-result/**")
				.permitAll().requestMatchers("/api/**").authenticated()

				.requestMatchers("/notice/write").hasRole("ADMIN")
				// [추가] 공지사항 목록/상세는 누구나(혹은 로그인 유저) 허용
				.requestMatchers("/notice/**").permitAll()

				// 2. 그 외 요청은 인증(로그인) 필요
				.anyRequest().authenticated()).formLogin((form) -> form.loginPage("/login") // 로그인 페이지 URL
						.usernameParameter("email") // HTML 폼의 input name (이메일로 로그인 시)
						.passwordParameter("password") // HTML 폼의 input name
						.successHandler(customLoginSuccessHandler).failureUrl("/login?error").permitAll())

				.oauth2Login(oauth2 -> oauth2.loginPage("/login")
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))

						// ⭐ [수정] 성공 시 핸들러 연결 (기존 defaultSuccessUrl 대신 사용)
						.successHandler(oAuth2SuccessHandler))
				.logout((logout) -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
						.logoutSuccessUrl("/login?logout") // 로그아웃 성공 시 이동할 페이지
						.invalidateHttpSession(true).deleteCookies("JSESSIONID"))
				.sessionManagement((session) -> session
						// 세션이 끊긴 상태로 접근하면 이 주소로 보냄 (?expired 파라미터 추가)
						.invalidSessionUrl("/login?expired"));
		return http.build();
	}

	// 비밀번호 암호화 빈 등록 (DB에 비밀번호 저장 시 필수)
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}