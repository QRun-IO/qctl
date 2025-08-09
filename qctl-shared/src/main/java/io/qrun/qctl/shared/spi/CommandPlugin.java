package io.qrun.qctl.shared.spi;

/** Service-provider interface for dynamically contributed Picocli subcommands. */
public interface CommandPlugin {
  /**
   * Returns an instance of a Picocli @Command-annotated class to register as a subcommand.
   * The returned object should be thread-safe or stateless.
   */
  Object getCommand();
}
