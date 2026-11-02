package com.axel.doublesneak;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DoubleSneakBackpack extends JavaPlugin implements Listener {
    private final Map<UUID, Long> lastSneak = new HashMap<>();
    private Object modularPacksPlugin;
    private Object useListener;
    private Method equippedBackpack;
    private Method openBackpackFromItem;
    private Method isBackpack;
    private Object openCause;

    @Override public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("ModularPacks") == null) {
            getLogger().severe("ModularPacks is required. Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        try {
            ClassLoader loader = Bukkit.getPluginManager().getPlugin("ModularPacks").getClass().getClassLoader();
            Class<?> pluginClass = loader.loadClass("io.github.tootertutor.ModularPacks.ModularPacksPlugin");
            modularPacksPlugin = Bukkit.getPluginManager().getPlugin("ModularPacks");
            Class<?> listenerClass = loader.loadClass("io.github.tootertutor.ModularPacks.listeners.backpack.BackpackUseListener");
            Constructor<?> ctor = listenerClass.getConstructor(pluginClass);
            useListener = ctor.newInstance(modularPacksPlugin);
            equippedBackpack = listenerClass.getDeclaredMethod("equippedBackpack", Player.class);
            equippedBackpack.setAccessible(true);
            openBackpackFromItem = listenerClass.getDeclaredMethod("openBackpackFromItem", Player.class, Runnable.class,
                    org.bukkit.inventory.ItemStack.class,
                    loader.loadClass("io.github.tootertutor.ModularPacks.api.events.backpack.BackpackOpenCause"));
            openBackpackFromItem.setAccessible(true);
            Class<?> itemsClass = loader.loadClass("io.github.tootertutor.ModularPacks.item.BackpackItems");
            Object items = pluginClass.getMethod("getBackpackMenuRenderer").invoke(modularPacksPlugin);
            // The item validity check is performed by ModularPacks' equippedBackpack/open path.
            Class<?> causeClass = loader.loadClass("io.github.tootertutor.ModularPacks.api.events.backpack.BackpackOpenCause");
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object cause = Enum.valueOf((Class<? extends Enum>) causeClass, "EQUIPPED_HOTKEY");
            openCause = cause;
            getLogger().info("Hooked into ModularPacks equipped-backpack opening.");
        } catch (ReflectiveOperationException ex) {
            getLogger().severe("Could not hook ModularPacks 2.7.7: " + ex);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        Long previous = lastSneak.put(player.getUniqueId(), now);
        if (previous == null || now - previous > 450) return;
        lastSneak.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(this, () -> openEquipped(player));
    }

    private void openEquipped(Player player) {
        try {
            Object item = equippedBackpack.invoke(useListener, player);
            if (item == null) return;
            openBackpackFromItem.invoke(useListener, player, (Runnable) () -> {}, item, openCause);
        } catch (ReflectiveOperationException ex) {
            getLogger().warning("Could not open equipped backpack for " + player.getName() + ": " + ex.getMessage());
        }
    }
}
