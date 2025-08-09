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
    assertThrows(IllegalArgumentException.class, () ->
        ConfigLoader.loadAndValidate(tmp, new HashMap<>()));
  }
}
