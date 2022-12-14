package OneCoin.Server.config;

import OneCoin.Server.chat.controller.MessageInterceptor;
import OneCoin.Server.chat.controller.RegisterController;
import OneCoin.Server.config.auth.utils.UserUtilsForWebSocket;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

//웹소켓 핸들러를 특정 url과 매핑시킴
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final UserUtilsForWebSocket userUtilsForWebSocket;
    private final RegisterController registerController;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // (/ws/chat)엔드포인트로 들어온 http 을 웹소켓 통신으로 전환한다.
        //  요때 들어온 요청을 dispatcherServlet에서 같이 처리함.
        //  그래서 Spring mvc랑 함께 쓰기 좋다는 것.
        registry.addEndpoint("/ws/chat", "/ws/upbit-info")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // destination header(STOMP에서 사용하는 Message format)이 아래 /app으로 시작하면
        // 발행하는 거고
        registry.setApplicationDestinationPrefixes("/app");
        // /topic으로 시작하면 구독하는 것
        // 일단 여기서는 스프링에서 기본적으로 제공하는 simple broker를 사용
        registry.enableSimpleBroker("/topic", "/info");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new MessageInterceptor(
                userUtilsForWebSocket, registerController));
    }


}
