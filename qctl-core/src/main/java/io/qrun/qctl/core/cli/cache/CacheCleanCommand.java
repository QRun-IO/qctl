package io.qrun.qctl.core.cli.cache;

import java.io.IOException;
import java.nio.file.Path;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "clean", description = "Clean cache")
public class CacheCleanCommand implements Runnable {
  @Option(names = "--all", description = "Delete entire cache root")
  boolean all;

  @Override
  public void run() {
    Path root = io.qrun.qctl.core.sys.SystemPaths.cacheDir();
    if (!all) {
      System.out.println("Nothing to do. Use --all to remove entire cache.");
      return;
    }
    try {
      java.nio.file.Files.walk(root)
          .sorted(java.util.Comparator.reverseOrder())
          .forEach(p -> {
            try { java.nio.file.Files.deleteIfExists(p); } catch (IOException ignored) {}
          });
      System.out.println("Cache cleared: " + root);
    } catch (IOException e) {
      System.err.println("cache clean error: " + e.getMessage());
    }
  }
}
