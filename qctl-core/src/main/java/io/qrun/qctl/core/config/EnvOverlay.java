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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Map;

final class EnvOverlay {
  static JsonNode toNode(Map<String, String> env) {
    ObjectNode root = JsonNodeFactory.instance.objectNode();
    for (Map.Entry<String, String> e : env.entrySet()) {
      if (!e.getKey().startsWith("QCTL_")) continue;
      String[] parts = e.getKey().substring(5).toLowerCase(Locale.ROOT).split("_");
      ObjectNode cur = root;
      for (int i = 0; i < parts.length; i++) {
        String key = parts[i];
        if (i == parts.length - 1) {
          cur.put(key, e.getValue());
        } else {
          ObjectNode next = (ObjectNode) cur.get(key);
          if (next == null) {
            next = JsonNodeFactory.instance.objectNode();
            cur.set(key, next);
          }
          cur = next;
        }
      }
    }
    return root;
  }
}
