package io.qrun.qctl.qbit;

import picocli.CommandLine.Command;

@Command(name = "qbit", description = "qBit package operations", mixinStandardHelpOptions = true,
    subcommands = { ResolveCommand.class })
public class QbitCommand implements Runnable {
  @Override
  public void run() {
    System.out.println("qbit: run a subcommand. Try --help.");
  }
}
