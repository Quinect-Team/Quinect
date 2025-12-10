package com.project.quiz.config;

import java.util.Collections;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.project.quiz.domain.User;
import com.project.quiz.repository.UserRepository;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final UserRepository userRepository;

	public WebSocketConfig(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").withSockJS();
		registry.addEndpoint("/ws/vote").withSockJS();
		registry.addEndpoint("/ws/chat").withSockJS();
		registry.addEndpoint("/ws/friend-chat").withSockJS();
	}
	
	

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/queue");
		registry.setApplicationDestinationPrefixes("/app");
		
		//notification 용 추가
 		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

				if (accessor.getCommand() == StompCommand.CONNECT) {
					System.out.println("🔌 [STOMP CONNECT] 연결 요청");

					try {
						// ⭐ getFirstNativeHeader() 직접 사용 (이건 public)
						String userIdHeader = accessor.getFirstNativeHeader("X-User-ID");
						System.out.println("✅ X-User-ID 헤더: " + userIdHeader);

						if (userIdHeader != null && !userIdHeader.isEmpty()) {
							String userId = userIdHeader;
							System.out.println("✅ 클라이언트에서 전달받은 사용자 ID: " + userId);

							// ⭐ Principal 설정
							UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userId,
									null, Collections.emptyList());

							accessor.setUser(token);
							System.out.println("✅ Principal 설정 완료: " + userId);
						} else {
							System.out.println("❌ X-User-ID 헤더가 없습니다!");
						}
					} catch (Exception e) {
						System.err.println("❌ 에러: " + e.getMessage());
						e.printStackTrace();
					}
				}

				return message;
			}
		});
	}

}
