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
            hubEntry.id(),
            hubEntry.name(),
            hubEntry.version(),
            hubEntry.description(),
            hubEntry.prompts(),
            hubEntry.postGen(),
            hubEntry.ignore()
         );

         // Step 4: Collect variables (prompts + CLI overrides)
         Map<String, String> allVars = new LinkedHashMap<>();
         if(!noPrompt && manifest.prompts() != null && !manifest.prompts().isEmpty())
         {
            PromptRunner promptRunner = new PromptRunner();
            allVars.putAll(promptRunner.run(manifest.prompts(), variables));
         }
         allVars.putAll(variables);

         // Step 5: Render templates
         TemplateEngine engine = new TemplateEngine();
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
}
