package edu.northeastern.numad26sp_team4_sena.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Message {

    private String messageId;
    private String senderId;
    private String text;

    @ServerTimestamp
    private Date timestamp;

    public Message() {}

    public Message(String senderId, String text) {
        this.senderId = senderId;
        this.text = text;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}

