package io.qrun.qctl.core.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintStream;

public final class Output {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static void text(PrintStream out, String s) {
    out.println(s);
  }

  public static void json(PrintStream out, Object obj) {
    try {
      out.println(MAPPER.writeValueAsString(obj));
    } catch (Exception e) {
      out.println("{\"error\":\"serialization failure\"}");
    }
  }
}
