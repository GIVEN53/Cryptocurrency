package OneCoin.Server.chat.service;

import OneCoin.Server.chat.entity.ChatMessage;
import OneCoin.Server.chat.entity.ChatRoom;
import OneCoin.Server.chat.repository.ChatMessageRdbRepository;
import OneCoin.Server.chat.repository.ChatMessageRepository;
import OneCoin.Server.chat.constant.MessageType;
import OneCoin.Server.chat.repository.LastSavedRepository;
import OneCoin.Server.config.auth.utils.UserUtilsForWebSocket;
import OneCoin.Server.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final UserUtilsForWebSocket userInfoUtils;
    private final ChatMessageRdbRepository chatMessageRdbRepository;
    private final LastSavedRepository lastSavedRepository;
    private final ChatRoomService chatRoomService;

    public ChatMessage makeEnterOrLeaveChatMessage(MessageType messageType, Integer chatRoomId, User user) {
        ChatMessage chatMessage = ChatMessage.builder()
                .type(messageType)
                .chatRoomId(chatRoomId)
                .userId(user.getUserId())
                .userDisplayName(user.getDisplayName())
                .build();
        if (messageType.equals(MessageType.ENTER)) {
            setEnterMessage(chatMessage);
        } else if (messageType.equals(MessageType.LEAVE)) {
            setLeaveMessage(chatMessage);
        }
        setCurrentTime(chatMessage);
        return chatMessage;
    }

    public ChatMessage setInfoAndSaveMessage(ChatMessage chatMessage, Principal user) {
        chatMessage.setType(MessageType.TALK);
        setUserInfo(chatMessage, user);
        setCurrentTime(chatMessage);
        chatMessageRepository.save(chatMessage);
        return chatMessage;
    }

    private void setUserInfo(ChatMessage chatMessage, Principal user) {
        Map<String, Object> claims = userInfoUtils.extractClaims(user);
        Long userId = ((Integer) claims.get("id")).longValue();
        String displayName = (String) claims.get("displayName");
        chatMessage.setUserId(userId);
        chatMessage.setUserDisplayName(displayName);
    }

    private ChatMessage setCurrentTime(ChatMessage chatMessage) {
        chatMessage.setChatAt(LocalDateTime.now().toString());
        return chatMessage;
    }

    private ChatMessage setEnterMessage(ChatMessage chatMessage) {
        chatMessage.setMessage("[알림] " + chatMessage.getUserDisplayName() + "이 입장하셨습니다.");
        return chatMessage;
    }

    private ChatMessage setLeaveMessage(ChatMessage chatMessage) {
        chatMessage.setMessage("[알림] " + chatMessage.getUserDisplayName() + "이 퇴장하셨습니다.");
        return chatMessage;
    }

    public List<ChatMessage> getChatMessages(Integer chatRoomId) {
        return chatMessageRepository.getMessageFromRoom(chatRoomId);
    }

    public void saveInMemoryChatMessagesToRdb() {
        List<ChatRoom> chatRooms = chatRoomService.findAllChatRooms();
        for(ChatRoom chatRoom : chatRooms) {
            Integer chatRoomId = chatRoom.getChatRoomId();
            ChatMessage lastSaved = lastSavedRepository.get(chatRoomId);
            List<ChatMessage> messages;
            if(lastSaved == null) {
                messages = chatMessageRepository.findAll(chatRoomId);
            } else {
                Long index = chatMessageRepository.getIndex(chatRoomId, lastSaved);
                messages = chatMessageRepository.findAllAfter(chatRoomId, index);
            }
            if(messages.size() == 0) return;
            ChatMessage latestMessage = messages.get(messages.size() - 1);
            lastSavedRepository.save(chatRoomId, latestMessage);
            chatMessageRdbRepository.saveAll(messages);
        }
    }
}
