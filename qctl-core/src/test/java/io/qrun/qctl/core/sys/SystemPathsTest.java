package io.qrun.qctl.core.sys;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SystemPathsTest {
  @Test
  void configDir_resolves_some_path() {
    Path p = SystemPaths.configDir();
    assertThat(p).isNotNull();
  }

  @Test
  void cacheDir_resolves_some_path() {
    Path p = SystemPaths.cacheDir();
    assertThat(p).isNotNull();
  }
}
