package io.qrun.qctl.core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public final class ConfigLoader {
  private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
  private static final ObjectMapper JSON = new ObjectMapper();

  public static JsonNode loadAndValidate(Path projectConfig, Map<String, String> env)
      throws IOException {
    JsonNode merged = mergeDefaults(projectConfig, env);
    validateAgainstSchema(merged);
    return merged;
  }

  private static JsonNode mergeDefaults(Path projectConfig, Map<String, String> env)
      throws IOException {
    JsonNode defaults = JSON.readTree("{\"output\":\"text\"}");
    JsonNode file = Files.exists(projectConfig) ? YAML.readTree(Files.newBufferedReader(projectConfig)) : JSON.nullNode();
    JsonNode envNode = EnvOverlay.toNode(env);
    return JsonMerge.merge(JsonMerge.merge(defaults, file), envNode);
  }

  private static void validateAgainstSchema(JsonNode node) throws IOException {
    try (InputStream in =
        ConfigLoader.class.getResourceAsStream("/schema/qctl-config-v1.json")) {
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
      JsonSchema schema = factory.getSchema(in);
      Set<ValidationMessage> errors = schema.validate(node);
      if (!errors.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        errors.forEach(e -> sb.append(e.getInstanceLocation()).append(" ").append(e.getMessage()).append("\n"));
        throw new IllegalArgumentException("Config validation failed:\n" + sb);
      }
    }
  }
}
