package io.qrun.qctl.qstudio;

import io.qrun.qctl.shared.spi.CommandPlugin;

public final class QstudioPlugin implements CommandPlugin {
  @Override
  public Object getCommand() {
    return new QstudioCommand();
  }
}
