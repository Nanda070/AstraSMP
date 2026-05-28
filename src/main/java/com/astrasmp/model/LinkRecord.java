package com.astrasmp.model;

public final class LinkRecord {
    private final String uuid;
    private String discordId;
    private String code;
    private boolean verified;

    public LinkRecord(String uuid, String discordId, String code, boolean verified) {
        this.uuid = uuid;
        this.discordId = discordId;
        this.code = code;
        this.verified = verified;
    }

    public String getUuid() { return uuid; }
    public String getDiscordId() { return discordId; }
    public void setDiscordId(String discordId) { this.discordId = discordId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
