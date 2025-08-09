package io.qrun.qctl.core.cli;

import picocli.CommandLine.Command;

@Command(name = "cache", description = "Cache maintenance commands (V1)", mixinStandardHelpOptions = true,
    subcommands = {
      io.qrun.qctl.core.cli.cache.CacheLsCommand.class,
      io.qrun.qctl.core.cli.cache.CachePruneCommand.class,
      io.qrun.qctl.core.cli.cache.CacheCleanCommand.class
    })
public class CacheCommand implements Runnable {
  @Override
  public void run() {
    System.out.println("cache: use subcommands (ls|prune|clean)");
  }
}
