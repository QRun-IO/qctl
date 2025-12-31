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

package io.qrun.qctl.qqq;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.qrun.qctl.qqq.template.PostGenHookRunner;
import io.qrun.qctl.qqq.template.PromptRunner;
import io.qrun.qctl.qqq.template.TemplateEngine;
import io.qrun.qctl.qqq.template.TemplateManifest;
import io.qrun.qctl.qqq.template.TemplateResolver;
import io.qrun.qctl.qqq.template.TemplatesHub;
import io.qrun.qctl.shared.ExitCodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/*******************************************************************************
 * Initialize a new project from a template.
 *
 * Supports template IDs from hub, local paths, git URLs, or github shorthand.
 * Renders Handlebars templates, prompts for variables, and runs post-gen hooks.
 *
 * @since 0.1.0
 *******************************************************************************/
@Command(name = "init", description = "Initialize a new project from a template")
public class InitCommand implements Runnable
{
   /***************************************************************************
    * Default constructor for picocli.
    *
    * @since 0.1.0
    ***************************************************************************/
   public InitCommand()
   {
   }


   @Parameters(index = "0", description = "Template ID or source (path, git URL, github.com/org/repo)")
   String templateSource;

   @Parameters(index = "1", description = "Target directory for the new project")
   Path targetDir;

   @Option(names = "--version", description = "Template version (semver tag, e.g., 1.0.0)")
   String version;

   @Option(names = "--var", description = "Set a template variable (key=value)")
   Map<String, String> variables = new LinkedHashMap<>();

   @Option(names = "--force", description = "Overwrite target directory if it exists")
   boolean force;

   @Option(names = "--dry-run", description = "Preview changes without writing files")
   boolean dryRun;

   @Option(names = "--skip-hooks", description = "Skip post-generation hooks")
   boolean skipHooks;

   @Option(names = "--no-prompt", description = "Disable interactive prompts (use defaults)")
   boolean noPrompt;



   @Override
   public void run()
   {
      try
      {
         // Check target directory
         if(Files.exists(targetDir) && !force)
         {
            System.err.println("error: target directory already exists: " + targetDir);
            System.err.println("Use --force to overwrite.");
            System.exit(ExitCodes.CONFLICT);
         }

         // Try to find template in hub first (if source looks like an ID)
         TemplatesHub.TemplateEntry hubEntry = null;
         String                     gitSource = templateSource;

         if(isTemplateId(templateSource))
         {
            System.out.println("Looking up template: " + templateSource);
            TemplatesHub                         hub   = new TemplatesHub();
            Optional<TemplatesHub.TemplateEntry> entry = hub.findById(templateSource);

            if(entry.isPresent())
            {
               hubEntry = entry.get();
               TemplatesHub.TemplatesIndex index = hub.fetchIndex();
               gitSource = hubEntry.getGitUrl(index.registry());
               System.out.println("Found: " + hubEntry.name() + " v" + hubEntry.version());
            }
            else
            {
               System.err.println("error: template not found in hub: " + templateSource);
               System.err.println("Use 'qctl qqq list' to see available templates.");
               System.exit(ExitCodes.NOT_FOUND);
            }
         }

         // Resolve template source
         System.out.println("Resolving template: " + gitSource);
         TemplateResolver resolver     = new TemplateResolver();
         Path             templatePath = resolver.resolve(gitSource, version);

         // Get manifest - from hub entry or file
         TemplateManifest manifest = getManifest(templatePath, hubEntry);
         System.out.println("Template: " + manifest.name() + " v" + manifest.version());

         // Collect variables (prompts + CLI overrides)
         Map<String, String> allVars = new LinkedHashMap<>();
         if(!noPrompt && manifest.prompts() != null)
         {
            PromptRunner promptRunner = new PromptRunner();
            allVars.putAll(promptRunner.run(manifest.prompts(), variables));
         }
         allVars.putAll(variables);

         // Render templates
         TemplateEngine engine = new TemplateEngine();
         if(dryRun)
         {
            System.out.println("\n[DRY-RUN] Would create files:");
            engine.renderDryRun(templatePath, targetDir, allVars, manifest);
         }
         else
         {
            if(force && Files.exists(targetDir))
            {
               System.out.println("Overwriting existing directory...");
            }
            Files.createDirectories(targetDir);
            int fileCount = engine.render(templatePath, targetDir, allVars, manifest);
            System.out.println("\nCreated " + fileCount + " files in " + targetDir);

            // Run post-gen hooks
            if(!skipHooks && manifest.postGen() != null)
            {
               PostGenHookRunner hookRunner = new PostGenHookRunner();
               hookRunner.run(targetDir, manifest.postGen());
            }
         }

         System.out.println("\nProject initialized successfully!");
      }
      catch(IOException e)
      {
         System.err.println("error: " + e.getMessage());
         System.exit(ExitCodes.GENERIC);
      }
      catch(Exception e)
      {
         System.err.println("error: " + e.getMessage());
         System.exit(ExitCodes.GENERIC);
      }
   }



   /***************************************************************************
    * Check if the source looks like a template ID (no slashes, dots, or colons).
    *
    * @param source template source string
    * @return true if it looks like a hub template ID
    * @since 0.1.0
    ***************************************************************************/
   private boolean isTemplateId(String source)
   {
      return !source.contains("/") && !source.contains(".") && !source.contains(":");
   }



   /***************************************************************************
    * Get manifest from hub entry or file.
    *
    * @param templatePath path to template
    * @param hubEntry hub entry (may be null)
    * @return template manifest
    * @throws IOException if manifest cannot be loaded
    * @since 0.1.0
    ***************************************************************************/
   private TemplateManifest getManifest(Path templatePath, TemplatesHub.TemplateEntry hubEntry)
         throws IOException
   {
      if(hubEntry != null)
      {
         // Build manifest from hub entry
         return new TemplateManifest(
            hubEntry.id(),
            hubEntry.name(),
            hubEntry.version(),
            hubEntry.description(),
            hubEntry.prompts(),
            hubEntry.postGen(),
            hubEntry.ignore()
         );
      }
      else
      {
         // Load from file
         return TemplateManifest.load(templatePath);
      }
   }
}
