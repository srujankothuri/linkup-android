package edu.northeastern.numad26sp_team4_sena.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class LinkUpHistory {

    private String sessionId;
    private String partnerUid;
    private String partnerName;
    private String partnerUsername;
    private String partnerImage;

    @ServerTimestamp
    private Date linkedUpAt;

    private Date endedAt;
    private long durationSeconds;
    private int messageCount;

    public LinkUpHistory() {}

    public LinkUpHistory(String sessionId, String partnerUid, String partnerName,
                        String partnerUsername, String partnerImage) {
        this.sessionId = sessionId;
        this.partnerUid = partnerUid;
        this.partnerName = partnerName;
        this.partnerUsername = partnerUsername;
        this.partnerImage = partnerImage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPartnerUid() {
        return partnerUid;
    }

    public void setPartnerUid(String partnerUid) {
        this.partnerUid = partnerUid;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    public String getPartnerUsername() {
        return partnerUsername;
    }

    public void setPartnerUsername(String partnerUsername) {
        this.partnerUsername = partnerUsername;
    }

    public String getPartnerImage() {
        return partnerImage;
    }

    public void setPartnerImage(String partnerImage) {
        this.partnerImage = partnerImage;
    }

    public Date getLinkedUpAt() {
        return linkedUpAt;
    }

    public void setLinkedUpAt(Date linkedUpAt) {
        this.linkedUpAt = linkedUpAt;
    }

    public Date getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Date endedAt) {
        this.endedAt = endedAt;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
}

