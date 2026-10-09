package common.model;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String contentType; // "TEXT" or "IMAGE"
    private String content;     // text or base64 / metadata
    private String timestamp;

    public ChatMessage() {}
    public ChatMessage(String messageId, String senderId, String receiverId, String contentType, String content) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.contentType = contentType;
        this.content = content;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
