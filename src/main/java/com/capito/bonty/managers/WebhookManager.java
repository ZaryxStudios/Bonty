package com.capito.bonty.managers;

import com.capito.bonty.Bonty;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class WebhookManager {
    private final Bonty plugin;

    public WebhookManager(Bonty plugin) {
        this.plugin = plugin;
    }

    public void sendEvent(String eventKey, Map<String, String> placeholders) {
        if (!plugin.getConfig().getBoolean("features.discord-webhooks.enabled", false)) return;

        String base = "features.discord-webhooks.events." + eventKey;
        if (!plugin.getConfig().getBoolean(base + ".enabled", false)) return;

        String webhookUrl = plugin.getConfig().getString(base + ".url", "");
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) return;

        String title = apply(plugin.getConfig().getString(base + ".title", "Bonty Event"), placeholders);
        String description = apply(plugin.getConfig().getString(base + ".description", ""), placeholders);
        int color = plugin.getConfig().getInt(base + ".color", 16776960);

        String username = plugin.getConfig().getString("features.discord-webhooks.username", "Bonty");
        String avatarUrl = plugin.getConfig().getString("features.discord-webhooks.avatar-url", "");

        sendRawWebhook(webhookUrl, username, avatarUrl, title, description, color);
    }

    private String apply(String text, Map<String, String> placeholders) {
        String output = text == null ? "" : text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            output = output.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return output;
    }

    private void sendRawWebhook(String webhookUrl, String username, String avatarUrl, String title, String description, int color) {
        new Thread(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String escapedTitle = escape(title);
                String escapedDescription = escape(description);
                String escapedUsername = escape(username == null ? "Bonty" : username);

                StringBuilder json = new StringBuilder();
                json.append("{")
                    .append("\"username\":\"").append(escapedUsername).append("\",")
                    .append("\"embeds\":[{")
                    .append("\"title\":\"").append(escapedTitle).append("\",")
                    .append("\"description\":\"").append(escapedDescription).append("\",")
                    .append("\"color\":").append(color)
                    .append("}]");

                if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                    json.append(",\"avatar_url\":\"").append(escape(avatarUrl)).append("\"");
                }
                json.append("}");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes(StandardCharsets.UTF_8));
                }

                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send webhook: " + e.getMessage());
            }
        }, "Bonty-Webhook-" + System.nanoTime()).start();
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
