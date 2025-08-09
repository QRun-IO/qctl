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
package io.qrun.qctl.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ConfigLoaderTest {
  @Test
  void loads_defaults_when_no_file() throws Exception {
    Path tmp = Files.createTempDirectory("qctl-test");
    JsonNode n = ConfigLoader.loadAndValidate(tmp.resolve("missing.yaml"), new HashMap<>());
    assertThat(n.get("output").asText()).isEqualTo("text");
  }

  @Test
  void validates_schema_and_throws_on_bad_fields() throws Exception {
    Path tmp = Files.createTempFile("qctl-bad", ".yaml");
    Files.writeString(tmp, "log:\n  level: wrong\n");
    assertThrows(
        IllegalArgumentException.class, () -> ConfigLoader.loadAndValidate(tmp, new HashMap<>()));
  }
}
