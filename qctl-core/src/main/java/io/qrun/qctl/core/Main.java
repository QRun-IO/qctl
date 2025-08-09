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

package io.qrun.qctl.core;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;


@Command(
   name = "qctl",
   mixinStandardHelpOptions = true,
   version = { "qctl 0.1.0" },
   description = "qctl CLI",
   subcommands = {
      HelpCommand.class,
      io.qrun.qctl.core.cli.AuthCommand.class,
      io.qrun.qctl.core.cli.CacheCommand.class
   })
public class Main implements Runnable
{
   private static final Logger log = LoggerFactory.getLogger(Main.class);

   @Spec
   CommandSpec spec;

   @Option(names = "--verbose", description = "Enable verbose output")
   boolean verbose;

   @Option(names = "--debug", description = "Enable debug logging")
   boolean debug;

   @Option(names = "--output", description = "Output format: text|json")
   String outputFormat = "text";

   @Option(names = "--offline", description = "Run without any network access")
   boolean offline;

   @Option(
      names = "--hermetic",
      description = "Resolve strictly from lockfile; allow network only for locked URLs")
   boolean hermetic;

   @Option(names = "--telemetry.enabled", description = "Enable telemetry for this invocation")
   boolean telemetryEnabled;



   public static void main(String[] args)
   {
      CommandLine cmd = new CommandLine(new Main());
      // Discover external subcommands via SPI and register dynamically
      java.util.ServiceLoader<io.qrun.qctl.shared.spi.CommandPlugin> loader =
         java.util.ServiceLoader.load(io.qrun.qctl.shared.spi.CommandPlugin.class);
      for(io.qrun.qctl.shared.spi.CommandPlugin plugin : loader)
      {
         Object      command = plugin.getCommand();
         CommandLine sub     = new CommandLine(command);
         String      name    = sub.getCommandSpec().name();
         cmd.addSubcommand(name, command);
      }
      int code = cmd.execute(args);
      System.exit(code);
   }



   @Override
   public void run()
   {
      try
      {
         // Load config from default locations and env overlay
         java.nio.file.Path configPath =
            io.qrun.qctl.core.sys.SystemPaths.ensureDir(io.qrun.qctl.core.sys.SystemPaths.configDir())
               .resolve("qctl.yaml");
         com.fasterxml.jackson.databind.JsonNode cfg =
            io.qrun.qctl.core.config.ConfigLoader.loadAndValidate(configPath, System.getenv());
         spec.commandLine().getOut().println("qctl: no command specified. Use --help.");
      }
      catch(ExecutionException e)
      {
         log.error("Execution failed", e);
         throw e;
      }
      catch(Exception e)
      {
         log.error("Startup failed", e);
         System.exit(2);
      }
   }
}
