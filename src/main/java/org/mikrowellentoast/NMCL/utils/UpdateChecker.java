package org.mikrowellentoast.NMCL.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mikrowellentoast.NMCL.NoMoreCombatLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

public class UpdateChecker {
    private final NoMoreCombatLog plugin;

    public UpdateChecker(NoMoreCombatLog plugin) {
        this.plugin = plugin;
    }

    /** @deprecated Pass the plugin instance explicitly. */
    @Deprecated
    public UpdateChecker() {
        this(NoMoreCombatLog.getInstance());
    }


    public void checkForUpdates() {
        try {
            URI url = URI.create("https://api.modrinth.com/v2/project/nomorecombatlog/version");
            HttpURLConnection conn = (HttpURLConnection) url.toURL().openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            conn.setRequestProperty("User-Agent", "NoMoreCombatLog/" + plugin.getPluginMeta().getVersion());

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while((line = reader.readLine()) != null) response.append(line);
            } finally {
                conn.disconnect();
            }

            JsonArray versions = (JsonArray) JsonParser.parseString(response.toString());

            if (versions.isEmpty()) return;

            JsonObject latest = (JsonObject) versions.get(0);
            String latestVersion = String.valueOf(latest.get("version_number"));
            String currentVersion = plugin.getPluginMeta().getVersion();

            latestVersion = latestVersion.trim().replaceAll("^\"|\"$", "");


            if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                plugin.getLogger().info("Found newer version: " + latestVersion);
                plugin.setUpdate_available(latestVersion);
            }


        } catch (Exception e) {
            plugin.getLogger().warning("[NoMoreCombatLog] Update check failed: " + e.getMessage());
        }
    }
}
