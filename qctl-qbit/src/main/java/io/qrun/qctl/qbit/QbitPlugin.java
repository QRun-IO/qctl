package io.qrun.qctl.qbit;

import io.qrun.qctl.shared.spi.CommandPlugin;

public final class QbitPlugin implements CommandPlugin {
  @Override
  public Object getCommand() {
    return new QbitCommand();
  }
}
