package com.astrasmp.service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.AuctionLot;
import com.astrasmp.model.PlayerProfile;

public final class AuctionService {
    private final AstraSMPPlugin plugin;
    private final DataStore store;
    private final EconomyService economy;

    public AuctionService(AstraSMPPlugin plugin, DataStore store, EconomyService economy) {
        this.plugin = plugin;
        this.store = store;
        this.economy = economy;
    }

    public int limit() {
        return plugin.getConfig().getInt("server.auction-limit-per-player", 12);
    }

    public int activeLotsFor(String uuid) {
        return (int) store.activeLots().stream().filter(l -> l.getSellerUuid().equals(uuid)).count();
    }

    public AuctionLot createLot(Player seller, ItemStack item, long price) {
        if (item == null || item.getType().isAir() || price <= 0) return null;
        if (activeLotsFor(seller.getUniqueId().toString()) >= limit()) return null;

        int amount = item.getAmount();
        ItemStack clone = item.clone();
        clone.setAmount(amount);
        AuctionLot lot = new AuctionLot(
                store.nextLotId(),
                seller.getUniqueId().toString(),
                clone,
                price,
                System.currentTimeMillis(),
                0L,
                false
        );
        store.lots().put(lot.getId(), lot);
        item.setAmount(0);
        store.requestSave();
        return lot;
    }

    public boolean cancelLot(Player seller, long id) {
        AuctionLot lot = store.lots().get(id);
        if (lot == null || lot.isSold()) return false;
        if (!lot.getSellerUuid().equals(seller.getUniqueId().toString()) && !seller.hasPermission("astrasmp.admin")) return false;
        lot.setSold(true);
        seller.getInventory().addItem(lot.getItem().clone());
        store.requestSave();
        return true;
    }

    public boolean buyLot(Player buyer, long id) {
        AuctionLot lot = store.lots().get(id);
        if (lot == null || lot.isSold()) return false;
        
        PlayerProfile profile = store.profile(buyer.getUniqueId().toString(), buyer.getName());
        if (profile.getCoins() < lot.getPrice()) return false;

        long tax = Math.max(1L, Math.round(lot.getPrice() * plugin.getConfig().getDouble("server.tax-rate", 0.08)));
        long sellerGain = Math.max(0L, lot.getPrice() - tax);

        // Списываем деньги у покупателя
        profile.setCoins(profile.getCoins() - lot.getPrice());
        
        // ПОТОКОБЕЗОПАСНЫЙ ФИКС: Берем профиль продавца напрямую из нашего кэша/БД.
        // Если игрок есть в базе, его реальное имя подтянется автоматически.
        // Никаких обращений к Bukkit API.
        PlayerProfile sellerProfile = store.profile(lot.getSellerUuid(), "Unknown");
        sellerProfile.setCoins(sellerProfile.getCoins() + sellerGain);
        
        lot.setSold(true);
        var leftover = buyer.getInventory().addItem(lot.getItem().clone());
        leftover.values().forEach(stack -> buyer.getWorld().dropItemNaturally(buyer.getLocation(), stack));
        
        store.requestSave();
        return true;
    }

    public List<AuctionLot> search(String term) {
        String q = term == null ? "" : term.toLowerCase(Locale.ROOT);
        return store.activeLots().stream().filter(l -> {
            if (q.isBlank()) return true;
            Material mat = l.getItem().getType();
            String display = l.getItem().hasItemMeta() && l.getItem().getItemMeta().hasDisplayName() ? l.getItem().getItemMeta().getDisplayName().toLowerCase(Locale.ROOT) : "";
            return mat.name().toLowerCase(Locale.ROOT).contains(q) || display.contains(q);
        }).collect(Collectors.toList());
    }

    public Collection<AuctionLot> activeLots() {
        return store.activeLots();
    }
}