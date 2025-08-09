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
package io.qrun.qctl.core.output;


import java.io.PrintStream;
import com.fasterxml.jackson.databind.ObjectMapper;


public final class Output
{
   private static final ObjectMapper MAPPER = new ObjectMapper();



   public static void text(PrintStream out, String s)
   {
      out.println(s);
   }



   public static void json(PrintStream out, Object obj)
   {
      try
      {
         out.println(MAPPER.writeValueAsString(obj));
      }
      catch(Exception e)
      {
         out.println("{\"error\":\"serialization failure\"}");
      }
   }
}
