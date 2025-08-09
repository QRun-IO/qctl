package io.qrun.qctl.core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class JsonMerge {
  static JsonNode merge(JsonNode base, JsonNode override) {
    if (base == null) return override;
    if (override == null || override.isNull()) return base;
    if (base.isObject() && override.isObject()) {
      ObjectNode result = ((ObjectNode) base).deepCopy();
      override.fields().forEachRemaining(e -> {
        JsonNode existing = result.get(e.getKey());
        JsonNode merged = merge(existing, e.getValue());
        result.set(e.getKey(), merged);
      });
      return result;
    }
    return override;
  }
}
