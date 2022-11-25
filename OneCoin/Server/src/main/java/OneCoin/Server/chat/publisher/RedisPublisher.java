package OneCoin.Server.chat.publisher;

import OneCoin.Server.chat.chatMessage.dto.ChatResponseDto;
import OneCoin.Server.chat.chatMessage.entity.ChatMessage;
import OneCoin.Server.chat.chatMessage.mapper.ChatMapper;
import OneCoin.Server.chat.chatMessage.service.ChatService;
import OneCoin.Server.chat.constant.MessageType;
import OneCoin.Server.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {
    private final RedisTemplate<Object, Object> redisTemplate;
    private final ChatService chatService;
    private final ChatMapper chatMapper;
    private final ChannelTopic channelTopic;

    public void publishEnterOrLeaveMessage(MessageType type, Long chatRoomId, User user) {
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .userId(user.getUserId())
                .userDisplayName(user.getDisplayName())
                .build();
        ChatMessage messageToUse = chatService.delegate(type, chatMessage);
        ChatResponseDto chatResponseDto = chatMapper.chatMessageToResponseDto(messageToUse);
        log.info("[PUBLISHER] message is ready for sending");
        log.info("[MESSAGE] {}", chatResponseDto);
        redisTemplate.convertAndSend(channelTopic.getTopic(), chatResponseDto);
    }
}
