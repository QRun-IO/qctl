package io.qrun.qctl.core.sys;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SystemPaths {
  private SystemPaths() {}

  public static Path configDir() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      String appData = System.getenv("APPDATA");
      if (appData != null && !appData.isEmpty()) {
        return Paths.get(appData, "qctl");
      }
      return Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "qctl");
    } else if (os.contains("mac")) {
      return Paths.get(System.getProperty("user.home"), "Library", "Application Support", "qctl");
    } else {
      String xdg = System.getenv("XDG_CONFIG_HOME");
      if (xdg != null && !xdg.isEmpty()) {
        return Paths.get(xdg, "qctl");
      }
      return Paths.get(System.getProperty("user.home"), ".config", "qctl");
    }
  }

  public static Path cacheDir() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      String local = System.getenv("LOCALAPPDATA");
      if (local != null && !local.isEmpty()) {
        return Paths.get(local, "qctl", "cache");
      }
      return Paths.get(System.getProperty("user.home"), "AppData", "Local", "qctl", "cache");
    } else if (os.contains("mac")) {
      return Paths.get(System.getProperty("user.home"), "Library", "Caches", "qctl");
    } else {
      String xdg = System.getenv("XDG_CACHE_HOME");
      if (xdg != null && !xdg.isEmpty()) {
        return Paths.get(xdg, "qctl");
      }
      return Paths.get(System.getProperty("user.home"), ".cache", "qctl");
    }
  }

  public static Path ensureDir(Path p) {
    try {
      Files.createDirectories(p);
    } catch (Exception ignored) {
    }
    return p;
  }
}
