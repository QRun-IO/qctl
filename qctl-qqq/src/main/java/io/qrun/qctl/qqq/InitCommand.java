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
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import io.qrun.qctl.core.doctor.DependencyChecker;
import io.qrun.qctl.core.doctor.DependencyException;
import io.qrun.qctl.qqq.error.ErrorFormatter;
import io.qrun.qctl.qqq.error.SuggestionEngine;
import io.qrun.qctl.qqq.error.TemplateError;
import io.qrun.qctl.qqq.registry.TemplateInfo;
import io.qrun.qctl.qqq.registry.TemplateRegistry;
import io.qrun.qctl.qqq.registry.TemplateRegistryFactory;
import io.qrun.qctl.qqq.template.ConsoleUI;
import io.qrun.qctl.qqq.template.MergeOptions;
import io.qrun.qctl.qqq.template.PostGenHookRunner;
import io.qrun.qctl.qqq.template.PromptRunner;
import io.qrun.qctl.qqq.template.TemplateEngine;
import io.qrun.qctl.qqq.template.TemplateManifest;
import io.qrun.qctl.qqq.template.TemplateRenderException;
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
public class InitCommand implements Callable<Integer>
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

   @Option(names = {"-v", "--verbose"}, description = "Show detailed error information")
   boolean verbose;

   @Option(names = "--merge", description = "Merge with existing directory (skip existing files)")
   boolean merge;

   @Option(names = "--include", description = "Include patterns for merge (overwrite matching files)",
      split = ",")
   List<String> includePatterns = new ArrayList<>();

   @Option(names = "--exclude", description = "Exclude patterns for merge (protect matching files)",
      split = ",")
   List<String> excludePatterns = new ArrayList<>();

   @Option(names = "--interactive", description = "Prompt for each conflicting file in merge mode")
   boolean interactive;

   @Option(names = "--no-backup", description = "Disable backup creation when overwriting files")
   boolean noBackup;



   /***************************************************************************
    * Default constructor for picocli.
    *
    * @since 0.1.0
    ***************************************************************************/
   public InitCommand()
   {
   }



   @Override
   public Integer call()
   {
      ConsoleUI ui = new ConsoleUI();

      try
      {
         ui.header("qctl qqq init");
         ui.subtitle("Initialize a new QQQ project");
         ui.println();

         // Step 1: Template selection from registry
         TemplateRegistry registry = TemplateRegistryFactory.getRegistry();
         TemplateInfo templateInfo = selectTemplate(registry, ui);

         // Step 2: Get target directory
         Path effectiveTargetDir = getTargetDirectory(ui);

         // Check target directory
         if(Files.exists(effectiveTargetDir) && !force && !merge)
         {
            ui.error("target directory already exists: " + effectiveTargetDir);
            ui.println("Use --force to overwrite or --merge to merge with existing.");
            return ExitCodes.CONFLICT;
         }

         // Step 3: Download template from registry
         ui.println();
         ui.info("Downloading template...");
         Path templatePath = registry.downloadTemplate(templateInfo.id(), version);

         // Get manifest from template info
         TemplateManifest manifest = templateInfo.toManifest();

         // Check minimum qctl version
         if(manifest.minimumQctlVersion() != null)
         {
            String currentVersion = getVersionFromProperties();
            if(!isVersionSatisfied(currentVersion, manifest.minimumQctlVersion()))
            {
               ui.error("Template requires qctl " + manifest.minimumQctlVersion() + " or later");
               ui.println("Current version: " + currentVersion);
               ui.println("Please upgrade qctl to use this template.");
               return ExitCodes.VALIDATION;
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

         ///////////////////////////////////////////////////////////////////////
         // Build merge options                                               //
         ///////////////////////////////////////////////////////////////////////
         MergeOptions mergeOptions = merge
            ? new MergeOptions(true, includePatterns, excludePatterns, interactive, !noBackup)
            : MergeOptions.disabled();

         if(dryRun)
         {
            ui.println();
            ui.println("[DRY-RUN] Would create files:");
            engine.renderDryRun(templatePath, effectiveTargetDir, allVars, manifest, mergeOptions);
         }
         else
         {
            if(force && Files.exists(effectiveTargetDir))
            {
               ui.info("Overwriting existing directory...");
            }
            if(merge && Files.exists(effectiveTargetDir))
            {
               ui.info("Merging with existing directory...");
            }
            Files.createDirectories(effectiveTargetDir);
            int fileCount = engine.render(templatePath, effectiveTargetDir, allVars, manifest,
               mergeOptions, ui);
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
               //////////////////////////////////////////////////////////////////
               // Pre-flight dependency checks before running hooks            //
               //////////////////////////////////////////////////////////////////
               try
               {
                  checkHookDependencies(manifest.postGen(), ui);
               }
               catch(DependencyException e)
               {
                  ui.warning("Skipping post-gen hooks: " + e.getMessage());
                  ui.println("Run 'qctl doctor' for installation help.");
                  skipHooks = true;
               }

               if(!skipHooks)
               {
                  PostGenHookRunner hookRunner = new PostGenHookRunner();
                  hookRunner.run(effectiveTargetDir, manifest.postGen());
               }
            }
         }

         ui.println();
         ui.successBold("Project initialized successfully!");
         ui.println();
         ui.println("Next steps:");
         ui.println("  cd " + effectiveTargetDir);
         ui.println("  mvn clean verify");

         return ExitCodes.SUCCESS;
      }
      catch(TemplateRenderException e)
      {
         ErrorFormatter formatter = new ErrorFormatter(verbose);
         TemplateError error = new TemplateError(
            TemplateError.ErrorCategory.RENDER_ERROR,
            e.getMessage(),
            "Check template syntax and ensure all variables are defined"
         );
         System.err.print(formatter.format(error));
         return error.getExitCode();
      }
      catch(IOException e)
      {
         ErrorFormatter formatter = new ErrorFormatter(verbose);
         String hint = e instanceof UnknownHostException
            ? "Check your network connection and try again"
            : null;
         System.err.print(formatter.format(e, hint));
         return ExitCodes.GENERIC;
      }
      catch(TemplateError e)
      {
         ErrorFormatter formatter = new ErrorFormatter(verbose);
         System.err.print(formatter.format(e));
         return e.getExitCode();
      }
      catch(Exception e)
      {
         ErrorFormatter formatter = new ErrorFormatter(verbose);
         System.err.print(formatter.format(e, null));
         return ExitCodes.GENERIC;
      }
   }



   /***************************************************************************
    * Select template - use provided name or prompt interactively.
    *
    * @param registry template registry
    * @param ui console UI
    * @return selected template info
    * @throws IOException if registry cannot be fetched
    * @since 0.1.0
    ***************************************************************************/
   private TemplateInfo selectTemplate(TemplateRegistry registry, ConsoleUI ui) throws IOException
   {
      List<TemplateInfo> templates = registry.listTemplates();

      // If template name provided and found, use it
      Optional<TemplateInfo> entry = registry.getTemplate(templateName);
      if(entry.isPresent())
      {
         TemplateInfo selected = entry.get();
         ui.showValue("Template", selected.name(), selected.id());
         return selected;
      }

      /////////////////////////////////////////////////////////////////////////
      // Template not found - show error with suggestions                    //
      /////////////////////////////////////////////////////////////////////////
      List<String> templateIds = templates.stream().map(TemplateInfo::id).toList();
      SuggestionEngine suggestionEngine = new SuggestionEngine();
      List<String> suggestions = suggestionEngine.findSimilar(templateName, templateIds);

      ui.error("Template '" + templateName + "' not found.");
      if(!suggestions.isEmpty())
      {
         ui.println();
         ui.println("Did you mean?");
         for(String suggestion : suggestions)
         {
            ui.println("  " + suggestion);
         }
      }
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
         TemplateInfo::name,
         TemplateInfo::description,
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
    * Get version from version.properties resource file.
    *
    * @return version string from properties or "0.0.0-dev" if not found
    * @since 0.2.0
    ***************************************************************************/
   private String getVersionFromProperties()
   {
      Properties props = new Properties();
      try(InputStream is = getClass().getResourceAsStream("/version.properties"))
      {
         if(is != null)
         {
            props.load(is);
         }
      }
      catch(IOException e)
      {
         // Fall back to default
      }
      return props.getProperty("version", "0.0.0-dev");
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



   /***************************************************************************
    * Check dependencies required by post-gen hooks before running them.
    *
    * Examines hook commands and verifies required tools are installed:
    * - git commands require Git
    * - mvn commands require Java 21+ and Maven
    *
    * @param hooks list of post-gen hooks to check
    * @param ui console UI for status messages
    * @throws DependencyException if a required dependency is missing
    * @since 0.2.0
    ***************************************************************************/
   private void checkHookDependencies(List<TemplateManifest.PostGenHook> hooks, ConsoleUI ui)
         throws DependencyException
   {
      boolean needsGit = false;
      boolean needsMaven = false;

      for(TemplateManifest.PostGenHook hook : hooks)
      {
         if(hook.command() == null)
         {
            continue;
         }

         String cmd = hook.command().trim().toLowerCase();
         if(cmd.startsWith("git "))
         {
            needsGit = true;
         }
         if(cmd.startsWith("mvn ") || cmd.startsWith("./mvnw ") || cmd.contains("maven"))
         {
            needsMaven = true;
         }
      }

      if(needsGit)
      {
         DependencyChecker.requireGit();
      }

      if(needsMaven)
      {
         DependencyChecker.requireJava(21);
         DependencyChecker.requireMaven();
      }
   }
}
