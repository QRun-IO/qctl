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
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Simple deep-merge utility for JSON objects.
 */
final class JsonMerge
{
   /**
    * Deep-merges {@code override} into {@code base}. Objects are merged recursively; other types
    * replace the base value.
    *
    * @param base base JSON (may be null)
    * @param override override JSON
    * @return merged JSON node
    */
   static JsonNode merge(JsonNode base, JsonNode override)
   {
      if(base == null)
      {
         return override;
      }
      if(override == null || override.isNull())
      {
         return base;
      }
      if(base.isObject() && override.isObject())
      {
         ObjectNode result = base.deepCopy();
         override.fields().forEachRemaining(e -> {
            JsonNode existing = result.get(e.getKey());
            JsonNode merged   = merge(existing, e.getValue());
            result.set(e.getKey(), merged);
         });
         return result;
      }
      return override;
   }
}
