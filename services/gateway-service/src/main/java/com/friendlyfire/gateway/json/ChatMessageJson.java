package com.friendlyfire.gateway.json;

import com.friendlyfire.proto.friendlyfire.room.v1.ChatMessage;
import java.util.List;

/**
 * One room chat message (FF-315): in a CHAT_MESSAGE event's {@code chat}, and in
 * GET /api/rooms/{code}/chat. {@code displayName} is the name when it was sent
 * ("Deleted player" once they delete their account).
 */
public record ChatMessageJson(String messageId, String userId, String displayName, String text, String sentAt) {

    public static ChatMessageJson from(ChatMessage message) {
        return new ChatMessageJson(
                message.getMessageId(),
                message.getUserId(),
                message.getDisplayName(),
                message.getText(),
                ProtoJson.timestamp(message.hasSentAt(), message.getSentAt()));
    }

    /** GET /api/rooms/{code}/chat: oldest first, at most 200. */
    public record History(List<ChatMessageJson> messages) {
    }
}
