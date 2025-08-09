package io.qrun.qctl.qqq;

import io.qrun.qctl.shared.spi.CommandPlugin;

public final class QqqPlugin implements CommandPlugin {
  @Override
  public Object getCommand() {
    return new QqqCommand();
  }
}
