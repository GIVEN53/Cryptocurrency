package OneCoin.Server.chat.utils;

import org.springframework.stereotype.Component;

@Component
public class ChatRoomUtils {
    public final String KEY_FOR_CHAT_ROOMS = "ChatRooms";
    private final String PREFIX_OF_KEY = "ChatRoom";
    private final String SUFFIX_OF_KEY = "Session";
    private final String SUFFIX_OF_LAST_CHAT_KEY = "LastSavedKey";

    public Integer parseChatRoomId(String key) {
        String chatRoomIdAsString = key.replace(PREFIX_OF_KEY, "");
        chatRoomIdAsString = chatRoomIdAsString.replace(SUFFIX_OF_KEY, "");
        return Integer.parseInt(chatRoomIdAsString);
    }

    public String makeKey(Integer chatRoomId) {
        return PREFIX_OF_KEY + String.valueOf(chatRoomId) + SUFFIX_OF_KEY;
    }

    public String makeLastChatMessageKey(Integer chatRoomId) {
        return PREFIX_OF_KEY + String.valueOf(chatRoomId) + SUFFIX_OF_LAST_CHAT_KEY;
    }
}
