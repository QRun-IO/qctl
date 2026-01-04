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
import io.qrun.qctl.qqq.template.ConsoleUI;
import io.qrun.qctl.qqq.template.PostGenHookRunner;
import io.qrun.qctl.qqq.template.PromptRunner;
import io.qrun.qctl.qqq.template.TemplateEngine;
import io.qrun.qctl.qqq.template.TemplateManifest;
import io.qrun.qctl.qqq.template.TemplateRenderException;
import io.qrun.qctl.qqq.template.TemplateResolver;
import io.qrun.qctl.qqq.template.TemplatesHub;
import io.qrun.qctl.qqq.template.TransformExecutor;
import io.qrun.qctl.shared.ExitCodes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/*******************************************************************************
 * Initialize a new project from a template.
 *
 * Supports template IDs from hub, local paths, git URLs, or github shorthand.
 * Renders Velocity templates, prompts for variables, and runs post-gen hooks.
 *
 * @since 0.1.0
 *******************************************************************************/
@Command(name = "init", description = "Initialize a new project from a template",
   mixinStandardHelpOptions = true)
public class InitCommand implements Runnable
{
   private static final String DEFAULT_TEMPLATE = "new-qqq-application";
   private static final String DEFAULT_PROJECT_DIR = "my-qqq-app";

   @Parameters(
      index = "0",
      arity = "0..1",
      description = "Template name (default: ${DEFAULT-VALUE})",
      defaultValue = DEFAULT_TEMPLATE)
   String templateName;

   @Option(names = {"-o", "--output"}, description = "Target directory for the new project")
   Path targetDir;

   @Option(names = "--template-version", description = "Template version (semver tag)")
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



   /***************************************************************************
    * Default constructor for picocli.
    *
    * @since 0.1.0
    ***************************************************************************/
   public InitCommand()
   {
   }



   @Override
   public void run()
   {
      ConsoleUI ui = new ConsoleUI();

      try
      {
         ui.header("qctl qqq init");
         ui.subtitle("Initialize a new QQQ project");
         ui.println();

         // Step 1: Template selection
         TemplatesHub hub = new TemplatesHub();
         TemplatesHub.TemplateEntry hubEntry = selectTemplate(hub, ui);
         TemplatesHub.TemplatesIndex index = hub.fetchIndex();
         String gitSource = hubEntry.getGitUrl(index.registry());

         // Step 2: Get target directory
         Path effectiveTargetDir = getTargetDirectory(ui);

         // Check target directory
         if(Files.exists(effectiveTargetDir) && !force)
         {
            ui.error("target directory already exists: " + effectiveTargetDir);
            ui.println("Use --force to overwrite.");
            System.exit(ExitCodes.CONFLICT);
         }

         // Step 3: Resolve template
         ui.println();
         ui.info("Resolving template...");
         TemplateResolver resolver = new TemplateResolver();
         Path templatePath = resolver.resolve(gitSource, version);

         // Get manifest from hub entry
         TemplateManifest manifest = new TemplateManifest(
            hubEntry.schemaVersion(),
            hubEntry.id(),
            hubEntry.name(),
            hubEntry.version(),
            hubEntry.description(),
            hubEntry.minimumQctlVersion(),
            hubEntry.prompts(),
            hubEntry.computed(),
            hubEntry.transforms(),
            hubEntry.postGen(),
            hubEntry.ignore()
         );

         // Check minimum qctl version
         if(manifest.minimumQctlVersion() != null)
         {
            String currentVersion = getClass().getPackage().getImplementationVersion();
            if(currentVersion == null)
            {
               currentVersion = "0.0.0-dev";
            }
            if(!isVersionSatisfied(currentVersion, manifest.minimumQctlVersion()))
            {
               ui.error("Template requires qctl " + manifest.minimumQctlVersion() + " or later");
               ui.println("Current version: " + currentVersion);
               ui.println("Please upgrade qctl to use this template.");
               System.exit(ExitCodes.VALIDATION);
            }
         }

         // Step 4: Collect variables (prompts + CLI overrides)
         Map<String, String> allVars = new LinkedHashMap<>();
         if(!noPrompt && manifest.prompts() != null && !manifest.prompts().isEmpty())
         {
            PromptRunner promptRunner = new PromptRunner();
            allVars.putAll(promptRunner.run(manifest.prompts(), variables));
         }
         allVars.putAll(variables);

         // Step 5: Evaluate computed variables
         TemplateEngine engine = new TemplateEngine();
         allVars = new LinkedHashMap<>(engine.evaluateComputed(manifest.computed(), allVars));
         if(dryRun)
         {
            ui.println();
            ui.println("[DRY-RUN] Would create files:");
            engine.renderDryRun(templatePath, effectiveTargetDir, allVars, manifest);
         }
         else
         {
            if(force && Files.exists(effectiveTargetDir))
            {
               ui.info("Overwriting existing directory...");
            }
            Files.createDirectories(effectiveTargetDir);
            int fileCount = engine.render(templatePath, effectiveTargetDir, allVars, manifest);
            ui.println();
            ui.success("Created " + fileCount + " files in " + effectiveTargetDir);

            // Run transforms (rename/delete)
            if(manifest.transforms() != null && !manifest.transforms().isEmpty())
            {
               ui.info("Applying transforms...");
               TransformExecutor transformExecutor = new TransformExecutor();
               transformExecutor.execute(effectiveTargetDir, manifest.transforms(), allVars);
            }

            // Run post-gen hooks
            if(!skipHooks && manifest.postGen() != null)
            {
               PostGenHookRunner hookRunner = new PostGenHookRunner();
               hookRunner.run(effectiveTargetDir, manifest.postGen());
            }
         }

         ui.println();
         ui.successBold("Project initialized successfully!");
         ui.println();
         ui.println("Next steps:");
         ui.println("  cd " + effectiveTargetDir);
         ui.println("  mvn clean verify");
      }
      catch(TemplateRenderException e)
      {
         ui.error("Template error: " + e.getMessage());
         System.exit(ExitCodes.VALIDATION);
      }
      catch(IOException e)
      {
         ui.error(e.getMessage());
         System.exit(ExitCodes.GENERIC);
      }
      catch(Exception e)
      {
         ui.error(e.getMessage());
         System.exit(ExitCodes.GENERIC);
      }
   }



   /***************************************************************************
    * Select template - use provided name or prompt interactively.
    *
    * @param hub templates hub
    * @param ui console UI
    * @return selected template entry
    * @throws IOException if hub cannot be fetched
    * @since 0.1.0
    ***************************************************************************/
   private TemplatesHub.TemplateEntry selectTemplate(TemplatesHub hub, ConsoleUI ui) throws IOException
   {
      List<TemplatesHub.TemplateEntry> templates = hub.fetchIndex().templates();

      // If template name provided and found, use it
      Optional<TemplatesHub.TemplateEntry> entry = hub.findById(templateName);
      if(entry.isPresent())
      {
         TemplatesHub.TemplateEntry selected = entry.get();
         ui.showValue("Template", selected.name(), selected.id());
         return selected;
      }

      // Template not found - prompt for selection
      ui.error("Template '" + templateName + "' not found.");
      ui.println();

      // Find default index
      int defaultIndex = 0;
      for(int i = 0; i < templates.size(); i++)
      {
         if(DEFAULT_TEMPLATE.equals(templates.get(i).id()))
         {
            defaultIndex = i;
            break;
         }
      }

      return ui.promptSelect(
         "Select a template:",
         templates,
         TemplatesHub.TemplateEntry::name,
         TemplatesHub.TemplateEntry::description,
         defaultIndex
      );
   }



   /***************************************************************************
    * Get target directory - use provided value or prompt interactively.
    *
    * @param ui console UI
    * @return target directory path
    * @since 0.1.0
    ***************************************************************************/
   private Path getTargetDirectory(ConsoleUI ui)
   {
      if(targetDir != null)
      {
         ui.showValue("Project directory", targetDir.toString());
         return targetDir;
      }

      if(noPrompt)
      {
         ui.showValue("Project directory", DEFAULT_PROJECT_DIR);
         return Path.of(DEFAULT_PROJECT_DIR);
      }

      String input = ui.promptText("Project directory", DEFAULT_PROJECT_DIR);
      return Path.of(input);
   }



   /***************************************************************************
    * Check if current version satisfies the minimum version requirement.
    *
    * @param current current version string (e.g., "0.2.0")
    * @param minimum minimum required version (e.g., "0.1.0")
    * @return true if current >= minimum
    * @since 0.2.0
    ***************************************************************************/
   private boolean isVersionSatisfied(String current, String minimum)
   {
      int[] currentParts = parseVersion(current);
      int[] minimumParts = parseVersion(minimum);

      for(int i = 0; i < 3; i++)
      {
         if(currentParts[i] > minimumParts[i])
         {
            return true;
         }
         if(currentParts[i] < minimumParts[i])
         {
            return false;
         }
      }
      return true;
   }



   /***************************************************************************
    * Parse a version string into major, minor, patch components.
    *
    * @param version version string (e.g., "1.2.3" or "1.2.3-dev")
    * @return array of [major, minor, patch]
    * @since 0.2.0
    ***************************************************************************/
   private int[] parseVersion(String version)
   {
      int[] parts = {0, 0, 0};
      if(version == null || version.isEmpty())
      {
         return parts;
      }

      /////////////////////////////////////////////////////////////////////////
      // Strip any suffix after dash (e.g., "1.2.3-dev" -> "1.2.3")         //
      /////////////////////////////////////////////////////////////////////////
      String cleanVersion = version.contains("-") ? version.split("-")[0] : version;
      String[] segments = cleanVersion.split("\\.");

      for(int i = 0; i < Math.min(segments.length, 3); i++)
      {
         try
         {
            parts[i] = Integer.parseInt(segments[i]);
         }
         catch(NumberFormatException e)
         {
            parts[i] = 0;
         }
      }
      return parts;
   }
}
