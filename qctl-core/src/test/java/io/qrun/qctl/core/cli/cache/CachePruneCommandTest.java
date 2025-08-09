/*
 * All Rights Reserved
 *
 * Copyright (c) 2025. QRunIO.   Contact: contact@qrun.io
 *
 * THE CONTENTS OF THIS PROJECT ARE PROPRIETARY AND CONFIDENTIAL.
 * UNAUTHORIZED COPYING, TRANSFERRING, OR REPRODUCTION OF ANY PART OF THIS PROJECT, VIA ANY MEDIUM, IS STRICTLY PROHIBITED.
 *
 * The receipt or possession of the source code and/or any parts thereof does not convey or imply any right to use them
 * for any purpose other than the purpose for which they were provided to you.
 */
package io.qrun.qctl.core.cli.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CachePruneCommandTest {

  @Test
  void parse_size_units() {
    assertThat(CachePruneCommand.parseSize("1KB")).isEqualTo(1024);
    assertThat(CachePruneCommand.parseSize("2MB")).isEqualTo(2L * 1024 * 1024);
    assertThat(CachePruneCommand.parseSize("3GB")).isEqualTo(3L * 1024 * 1024 * 1024);
    assertThat(CachePruneCommand.parseSize("42B")).isEqualTo(42);
    assertThat(CachePruneCommand.parseSize("100")).isEqualTo(100);
  }

  @Test
  void prune_noop_when_total_below_target() throws Exception {
    Path tmp = Files.createTempDirectory("qctl-cache");
    Path f = tmp.resolve("a.bin");
    Files.writeString(f, "12345");
    long before = Files.size(f);

    CachePruneCommand cmd = new CachePruneCommand(tmp);
    cmd.maxSize = "10KB";
    cmd.run();

    long after = Files.exists(f) ? Files.size(f) : 0;
    assertThat(after).isEqualTo(before);
  }

  @Test
  void parse_size_invalid_throws_number_format() {
    assertThatThrownBy(() -> CachePruneCommand.parseSize("abcMB"))
        .isInstanceOf(NumberFormatException.class);
  }
}
