package com.chat.client.model;

import java.io.*;
import java.util.Properties;

/** Defaults from ./config.properties or the bundled file; command-line args override them. */
public record AppConfig(String host, int port) {
    public static AppConfig load() {
        Properties p = new Properties();
        File external = new File("config.properties");
        try (InputStream in = external.exists() ? new FileInputStream(external)
                : AppConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) p.load(in);
        } catch (IOException ignored) { }
        return new AppConfig(p.getProperty("server.host", "localhost"),
                Integer.parseInt(p.getProperty("server.port", "3000").trim()));
    }
}
