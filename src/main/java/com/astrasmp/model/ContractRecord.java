package com.astrasmp.model;

public final class ContractRecord {
    private final long id;
    private final String creatorUuid;
    private final String targetUuid;
    private final long reward;
    private final String type;
    private final String note;
    private boolean active;
    private final long createdAt;

    public ContractRecord(long id, String creatorUuid, String targetUuid, long reward, String type, String note, boolean active, long createdAt) {
        this.id = id;
        this.creatorUuid = creatorUuid;
        this.targetUuid = targetUuid;
        this.reward = reward;
        this.type = type;
        this.note = note;
        this.active = active;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getCreatorUuid() { return creatorUuid; }
    public String getTargetUuid() { return targetUuid; }
    public long getReward() { return reward; }
    public String getType() { return type; }
    public String getNote() { return note; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getCreatedAt() { return createdAt; }
}
