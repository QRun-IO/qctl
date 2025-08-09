package io.qrun.qctl.core.cli.cache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import picocli.CommandLine.Command;

@Command(name = "ls", description = "List cache entries")
public class CacheLsCommand implements Runnable {
  @Override
  public void run() {
    Path root = io.qrun.qctl.core.sys.SystemPaths.cacheDir();
    try (Stream<Path> s = Files.walk(root, 2)) {
      s.filter(Files::isRegularFile).limit(50).forEach(p -> System.out.println(root.relativize(p)));
    } catch (Exception e) {
      System.err.println("cache ls error: " + e.getMessage());
    }
  }
}
