package com.astrasmp.rituals;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RitualRecipe {

    private final String id;
    private final List<ItemStack> requiredItems;
    private final EntityType requiredSacrifice;
    private final int minimumCircleTier;
    private final ItemStack result;
    private final int corruptionReward;

    public RitualRecipe(String id, List<ItemStack> requiredItems, EntityType requiredSacrifice, int minimumCircleTier, ItemStack result, int corruptionReward) {
        this.id = id;
        this.requiredItems = requiredItems;
        this.requiredSacrifice = requiredSacrifice;
        this.minimumCircleTier = minimumCircleTier;
        this.result = result;
        this.corruptionReward = corruptionReward;
    }

    public String getId() { return id; }
    public List<ItemStack> getRequiredItems() { return requiredItems; }
    public EntityType getRequiredSacrifice() { return requiredSacrifice; }
    public int getMinimumCircleTier() { return minimumCircleTier; }
    public ItemStack getResult() { return result; }
    public int getCorruptionReward() { return corruptionReward; }

    public boolean matches(List<ItemStack> groundItems, EntityType victim, int tier) {
        if (tier < minimumCircleTier) return false;
        
        // С жертвой: если requiredSacrifice == null, значит жертва не нужна (или любая).
        // Дополнительно: Игрока (PLAYER) можно принести в жертву в ЛЮБОМ ритуале вместо требуемого моба.
        if (requiredSacrifice != null && requiredSacrifice != victim && victim != EntityType.PLAYER) return false;

        // Проверка предметов
        // Простой алгоритм: каждый требуемый предмет должен быть найден на земле (с нужным материалом, и в нужном или большем количестве).
        // Для точной проверки лучше использовать ItemRegistry.is или проверять кастомдату, но пока проверяем тип и количество.
        for (ItemStack req : requiredItems) {
            int currentAmount = 0;
            for (ItemStack ground : groundItems) {
                if (ground.getType() == req.getType()) {
                    currentAmount += ground.getAmount();
                }
            }
            if (currentAmount < req.getAmount()) {
                return false;
            }
        }

        return true;
    }
}
