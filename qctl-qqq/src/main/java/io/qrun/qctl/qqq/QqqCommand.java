package io.qrun.qctl.qqq;

import picocli.CommandLine.Command;

@Command(name = "qqq", description = "Scaffolding commands", mixinStandardHelpOptions = true)
public class QqqCommand implements Runnable {
  @Override
  public void run() {
    System.out.println("qqq: run a subcommand. Try --help.");
  }
}
