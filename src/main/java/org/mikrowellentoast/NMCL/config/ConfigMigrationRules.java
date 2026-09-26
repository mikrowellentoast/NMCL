package org.mikrowellentoast.NMCL.config;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

public final class ConfigMigrationRules {
    private ConfigMigrationRules() {}
    public static Map<String, String> legacyMappings() {
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("enabled", "plugin.enabled");
        mappings.put("combat-tag-duration", "combat.duration");
        mappings.put("enable-in-creative", "combat.creative-mode");
        mappings.put("retaliation-attack", "combat.retaliation.enabled");
        mappings.put("retaliationattack", "combat.retaliation.enabled");
        mappings.put("retaliation-attack-duration", "combat.retaliation.window");
        mappings.put("retaliation-window", "combat.retaliation.window");
        mappings.put("set-attacker-on-combat", "combat.retaliation.tag-attacker-immediately");
        mappings.put("allow-portal-teleport", "teleport.portals");
        mappings.put("allow-enderpearl-teleport", "teleport.ender-pearls");
        mappings.put("blocked-commands", "commands.list");
        mappings.put("disabled-worlds", "worlds.disabled");
        mappings.put("enable-safe-zone", "safe-zones.enabled");
        mappings.put("remove-tag-when-entering-safe-zone", "safe-zones.remove-combat-on-entry");
        return Collections.unmodifiableMap(mappings);
    }
}
