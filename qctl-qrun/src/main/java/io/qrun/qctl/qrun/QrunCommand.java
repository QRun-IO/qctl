package io.qrun.qctl.qrun;

import picocli.CommandLine.Command;

@Command(name = "qrun", description = "qRun lifecycle commands", mixinStandardHelpOptions = true,
    subcommands = { StatusCommand.class })
public class QrunCommand implements Runnable {
  @Override
  public void run() {
    System.out.println("qrun: run a subcommand. Try --help.");
  }
}
