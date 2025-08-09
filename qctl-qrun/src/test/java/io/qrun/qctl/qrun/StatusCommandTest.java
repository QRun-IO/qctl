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

package io.qrun.qctl.qrun;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


class StatusCommandTest
{

   @Test
   void render_text_output()
   {
      Map<String, Object> status = new HashMap<>();
      status.put("status", "healthy");
      status.put("version", "0.1.0");
      status.put("updatedAt", "2025-01-15T12:05:00Z");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream           out  = new PrintStream(baos);
      StatusCommand.renderStatus(status, "demo-app", "dev", "text", out);
      String s = baos.toString().trim();
      assertThat(s).contains("demo-app").contains("env dev").contains("status=healthy");
   }



   @Test
   void render_json_output()
   {
      Map<String, Object> status = new HashMap<>();
      status.put("status", "healthy");
      status.put("version", "0.1.0");
      status.put("updatedAt", "2025-01-15T12:05:00Z");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      PrintStream           out  = new PrintStream(baos);
      StatusCommand.renderStatus(status, "demo-app", "dev", "json", out);
      String json = baos.toString().trim();
      assertThat(json).startsWith("{").contains("\"app\":\"demo-app\"").contains("\"status\":");
   }
}
