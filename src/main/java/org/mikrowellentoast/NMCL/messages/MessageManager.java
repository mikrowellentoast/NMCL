package org.mikrowellentoast.NMCL.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class MessageManager {
    private final NoMoreCombatLog plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages;

    public MessageManager(NoMoreCombatLog plugin) {
        this.plugin = plugin;
        if (!new File(plugin.getDataFolder(), "messages.yml").exists()) plugin.saveResource("messages.yml", false);
        reload();
    }

    public void reload() {
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    public void send(CommandSender sender, String key) { send(sender, key, Map.of()); }

    public void send(CommandSender sender, String key, Map<String, ?> placeholders) {
        Component component = component(key, placeholders, true);
        if (!component.equals(Component.empty())) sender.sendMessage(component);
    }

    public Component component(String key, Map<String, ?> placeholders, boolean prefix) {
        String value = messages.getString(key, "");
        if (value == null || value.isBlank()) return Component.empty();
        String raw = (prefix ? messages.getString("prefix", "") : "") + value;
        Map<String, Object> safe = new HashMap<>(placeholders);
        for (var entry : safe.entrySet()) {
            raw = raw.replace("<" + entry.getKey() + ">", MiniMessage.miniMessage().escapeTags(String.valueOf(entry.getValue())));
        }
        try {
            return miniMessage.deserialize(raw);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Invalid MiniMessage at messages.yml key '" + key + "': " + exception.getMessage());
            return Component.text(raw);
        }
    }

    public String raw(String key, String fallback) { return messages.getString(key, fallback); }
}
