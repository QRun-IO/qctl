package io.qrun.qctl.qrun;

import io.qrun.qctl.shared.spi.CommandPlugin;

public final class QrunPlugin implements CommandPlugin {
  @Override
  public Object getCommand() {
    return new QrunCommand();
  }
}
