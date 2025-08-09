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


import java.io.PrintStream;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import io.qrun.qctl.core.http.ApiClient;
import io.qrun.qctl.core.output.Output;
import io.qrun.qctl.core.sys.SystemPaths;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;


@Command(name = "status", description = "Show app status from qRun API (mock)")
public class StatusCommand implements Runnable
{
   @Option(names = "--app", required = true)
   String app;

   @Option(names = "--env", required = true)
   String env;

   @Spec
   CommandSpec spec;



   /***************************************************************************
    **
    ***************************************************************************/
   static void renderStatus(
      Map<String, Object> status, String app, String env, String outputFormat, PrintStream out)
   {
      if("json".equalsIgnoreCase(outputFormat))
      {
         Map<String, Object> wrapper = new LinkedHashMap<>();
         wrapper.put("app", app);
         wrapper.put("env", env);
         wrapper.put("status", status);
         Output.json(out, wrapper);
         return;
      }
      String st      = String.valueOf(status.getOrDefault("status", "unknown"));
      String ver     = String.valueOf(status.getOrDefault("version", "n/a"));
      String updated = String.valueOf(status.getOrDefault("updatedAt", "n/a"));
      Output.text(
         out,
         "app "
            + app
            + " (env "
            + env
            + "): status="
            + st
            + " version="
            + ver
            + " updatedAt="
            + updated);
   }



   @SuppressWarnings("checkstyle:MagicNumber")
   @Override
   public void run()
   {
      try
      {
         var store  = new io.qrun.qctl.core.auth.TokenStore(SystemPaths.configDir());
         var apiKey = store.readApiKey();
         ApiClient client =
            new ApiClient(
               Duration.ofSeconds(30),
               builder ->
               {
                  builder.header("User-Agent", "qctl/0.1.0");
                  apiKey.ifPresent(k -> builder.header("X-API-Key", k));
               });
         URI uri = URI.create("http://localhost:4010/v1/apps/" + app + "/status?env=" + env);
         @SuppressWarnings("unchecked")
         Map<String, Object> status = client.getJson(uri, Map.class);
         String outputFormat = resolveOutputFormat();
         renderStatus(status, app, env, outputFormat, System.out);
      }
      catch(ApiClient.ApiException e)
      {
         System.err.println("error: " + e.problem.title + ": " + e.problem.detail);
         System.exit(e.exitCode);
      }
      catch(Exception e)
      {
         System.err.println("error: " + e.getMessage());
         System.exit(1);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private String resolveOutputFormat()
   {
      if(spec != null && spec.commandLine() != null && spec.commandLine().getParseResult() != null)
      {
         return spec.commandLine().getParseResult().matchedOptionValue("output", "text");
      }
      return "text";
   }
}
