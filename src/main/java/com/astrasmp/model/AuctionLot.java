package com.astrasmp.model;

import org.bukkit.inventory.ItemStack;

public final class AuctionLot {
    private final long id;
    private final String sellerUuid;
    private final ItemStack item;
    private final long price;
    private final long createdAt;
    private final long expiresAt;
    private boolean sold;

    public AuctionLot(long id, String sellerUuid, ItemStack item, long price, long createdAt, long expiresAt, boolean sold) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.item = item;
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.sold = sold;
    }

    public long getId() { return id; }
    public String getSellerUuid() { return sellerUuid; }
    public ItemStack getItem() { return item; }
    public long getPrice() { return price; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isSold() { return sold; }
    public void setSold(boolean sold) { this.sold = sold; }
}
