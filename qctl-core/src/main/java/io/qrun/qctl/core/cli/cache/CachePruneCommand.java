/*
 * All Rights Reserved
 *
 * Copyright (c) 2025. QRunIO.   Contact: contact@qrun.io
 *
 * THE CONTENTS OF THIS PROJECT ARE PROPRIETARY AND CONFIDENTIAL.
 * UNAUTHORIZED COPYING, TRANSFERRING, OR REPRODUCTION OF ANY PART OF THIS PROJECT, VIA ANY MEDIUM, IS STRICTLY PROHIBITED.
 *
 * The receipt or possession of the source code and/or any parts thereof does not convey or imply any right to use them
 * for any purpose other than the purpose for which they were provided to you.
 */
package io.qrun.qctl.core.cli.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "prune", description = "Prune cache to target size")
public class CachePruneCommand implements Runnable {
  @Option(names = "--max-size", required = true, description = "Target size, e.g., 500MB")
  String maxSize;

  // For tests, allow overriding cache root
  final java.nio.file.Path rootOverride;

  public CachePruneCommand() {
    this.rootOverride = null;
  }

  CachePruneCommand(java.nio.file.Path rootOverride) {
    this.rootOverride = rootOverride;
  }

  @Override
  public void run() {
    long target = parseSize(maxSize);
    Path root = rootOverride != null ? rootOverride : io.qrun.qctl.core.sys.SystemPaths.cacheDir();
    try {
      long total =
          Files.walk(root).filter(Files::isRegularFile).mapToLong(CachePruneCommand::size).sum();
      if (total <= target) {
        System.out.println("No prune needed.");
        return;
      }
      // naive LRU: sort by lastModified
      try (Stream<Path> s =
          Files.walk(root)
              .filter(Files::isRegularFile)
              .sorted(Comparator.comparingLong(CachePruneCommand::lastModified))) {
        for (Path p : (Iterable<Path>) s::iterator) {
          long sz = size(p);
          try {
            Files.deleteIfExists(p);
          } catch (IOException ignored) {
          }
          total -= sz;
          if (total <= target) break;
        }
      }
      System.out.println("Pruned to <= target.");
    } catch (IOException e) {
      System.err.println("cache prune error: " + e.getMessage());
    }
  }

  static long parseSize(String s) {
    String t = s.trim().toUpperCase();
    long mul = 1;
    if (t.endsWith("KB")) {
      mul = 1024;
      t = t.substring(0, t.length() - 2);
    } else if (t.endsWith("MB")) {
      mul = 1024 * 1024;
      t = t.substring(0, t.length() - 2);
    } else if (t.endsWith("GB")) {
      mul = 1024L * 1024L * 1024L;
      t = t.substring(0, t.length() - 2);
    } else if (t.endsWith("B")) {
      mul = 1;
      t = t.substring(0, t.length() - 1);
    }
    long val = Long.parseLong(t.trim());
    return val * mul;
  }

  static long size(Path p) {
    try {
      return Files.size(p);
    } catch (IOException e) {
      return 0L;
    }
  }

  static long lastModified(Path p) {
    try {
      return Files.getLastModifiedTime(p).toMillis();
    } catch (IOException e) {
      return 0L;
    }
  }
}
