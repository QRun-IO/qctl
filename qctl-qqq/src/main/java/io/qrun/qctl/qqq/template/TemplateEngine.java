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

package io.qrun.qctl.qqq.template;


import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.qrun.qctl.qqq.error.SuggestionEngine;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;


/*******************************************************************************
 * Renders templates using Apache Velocity.
 *
 * Supports path substitution ($packagePath/src) and content templating with
 * full Velocity syntax including control flow and macros.
 *
 * @since 0.1.0
 *******************************************************************************/
public class TemplateEngine
{
   private static final String TEMPLATE_DIR = "template";
   private static final Set<String> BINARY_EXTENSIONS = Set.of(
      ".jar", ".class", ".png", ".jpg", ".jpeg", ".gif", ".ico",
      ".zip", ".tar", ".gz", ".war", ".ear", ".pdf"
   );

   /////////////////////////////////////////////////////////////////////////////
   // Files that should be copied verbatim without template processing        //
   /////////////////////////////////////////////////////////////////////////////
   private static final Set<String> VERBATIM_FILENAMES = Set.of(
      "mvnw", "mvnw.cmd", "gradlew", "gradlew.bat"
   );

   /////////////////////////////////////////////////////////////////////////////
   // Pattern to detect undefined Velocity references ($var or ${var})       //
   /////////////////////////////////////////////////////////////////////////////
   private static final Pattern UNDEFINED_REF_PATTERN =
      Pattern.compile("\\$(!)?\\{?([a-zA-Z][a-zA-Z0-9_]*)\\}?");

   private final VelocityEngine velocityEngine;
   private final StringTool stringTool;
   private final SuggestionEngine suggestionEngine;



   /***************************************************************************
    * Create a new template engine.
    *
    * @since 0.1.0
    ***************************************************************************/
   public TemplateEngine()
   {
      this.velocityEngine = createVelocityEngine();
      this.stringTool = new StringTool();
      this.suggestionEngine = new SuggestionEngine();
   }



   /***************************************************************************
    * Create and configure the Velocity engine.
    *
    * @return configured VelocityEngine
    * @since 0.2.0
    ***************************************************************************/
   private VelocityEngine createVelocityEngine()
   {
      Properties props = new Properties();
      props.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
      props.setProperty(RuntimeConstants.RESOURCE_LOADERS, "string");
      props.setProperty("resource.loader.string.class",
         "org.apache.velocity.runtime.resource.loader.StringResourceLoader");

      VelocityEngine engine = new VelocityEngine();
      engine.init(props);
      return engine;
   }



   /***************************************************************************
    * Render templates to the target directory.
    *
    * @param templateDir template source directory
    * @param targetDir target output directory
    * @param variables template variables
    * @param manifest template manifest
    * @return number of files created
    * @throws IOException if file operations fail
    * @throws TemplateRenderException if template rendering fails
    * @since 0.1.0
    ***************************************************************************/
   public int render(Path templateDir, Path targetDir, Map<String, String> variables,
                     TemplateManifest manifest) throws IOException, TemplateRenderException
   {
      return render(templateDir, targetDir, variables, manifest, MergeOptions.disabled(), null);
   }



   /***************************************************************************
    * Render templates with merge support.
    *
    * @param templateDir template source directory
    * @param targetDir target output directory
    * @param variables template variables
    * @param manifest template manifest
    * @param mergeOptions merge configuration
    * @param ui console UI for interactive prompts (may be null)
    * @return number of files created/modified
    * @throws IOException if file operations fail
    * @throws TemplateRenderException if template rendering fails
    * @since 0.2.0
    ***************************************************************************/
   public int render(Path templateDir, Path targetDir, Map<String, String> variables,
                     TemplateManifest manifest, MergeOptions mergeOptions, ConsoleUI ui)
         throws IOException, TemplateRenderException
   {
      Path        sourceDir      = resolveTemplateSource(templateDir);
      Set<String> ignorePatterns = manifest.ignore() != null
         ? new HashSet<>(manifest.ignore())
         : new HashSet<>();
      ignorePatterns.add(".git");
      ignorePatterns.add("template.yaml");

      ConsoleUI effectiveUi = ui != null ? ui : new ConsoleUI();
      MergeStrategy mergeStrategy = new MergeStrategy(mergeOptions, effectiveUi);

      AtomicInteger              fileCount   = new AtomicInteger();
      AtomicReference<Exception> renderError = new AtomicReference<>();

      Files.walkFileTree(sourceDir, new SimpleFileVisitor<>()
      {
         @Override
         public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
               throws IOException
         {
            if(shouldIgnore(sourceDir.relativize(dir).toString(), ignorePatterns))
            {
               return FileVisitResult.SKIP_SUBTREE;
            }

            try
            {
               Path   relativePath = sourceDir.relativize(dir);
               String renderedPath = renderPath(relativePath.toString(), variables);
               Path   targetPath   = targetDir.resolve(renderedPath);
               Files.createDirectories(targetPath);
            }
            catch(TemplateRenderException e)
            {
               renderError.set(e);
               return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
         }


         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
               throws IOException
         {
            Path relativePath = sourceDir.relativize(file);
            if(shouldIgnore(relativePath.toString(), ignorePatterns))
            {
               return FileVisitResult.CONTINUE;
            }

            try
            {
               String renderedPath = renderPath(relativePath.toString(), variables);
               Path   targetPath   = targetDir.resolve(renderedPath);

               //////////////////////////////////////////////////////////////////
               // Prepare content for merge decision                           //
               //////////////////////////////////////////////////////////////////
               String rendered;
               if(isBinaryFile(file) || isVerbatimFile(file))
               {
                  rendered = null;
               }
               else
               {
                  String content = Files.readString(file);
                  rendered = renderContent(content, variables);
               }

               //////////////////////////////////////////////////////////////////
               // Check merge strategy                                         //
               //////////////////////////////////////////////////////////////////
               MergeStrategy.MergeAction action = mergeStrategy.determineAction(
                  renderedPath, targetPath, rendered);

               switch(action)
               {
                  case SKIP:
                     effectiveUi.println("  [skip] " + renderedPath);
                     return FileVisitResult.CONTINUE;

                  case OVERWRITE:
                     mergeStrategy.createBackup(targetPath);
                     effectiveUi.println("  [overwrite] " + renderedPath);
                     break;

                  case WRITE:
                  default:
                     effectiveUi.println("  " + renderedPath);
                     break;
               }

               //////////////////////////////////////////////////////////////////
               // Write the file                                               //
               //////////////////////////////////////////////////////////////////
               if(isBinaryFile(file) || isVerbatimFile(file))
               {
                  Files.copy(file, targetPath,
                     java.nio.file.StandardCopyOption.REPLACE_EXISTING);
               }
               else
               {
                  Files.writeString(targetPath, rendered);
               }
               fileCount.incrementAndGet();
            }
            catch(TemplateRenderException e)
            {
               renderError.set(e);
               return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
         }
      });

      if(renderError.get() != null)
      {
         Exception e = renderError.get();
         if(e instanceof TemplateRenderException tre)
         {
            throw tre;
         }
         throw new TemplateRenderException("Template rendering failed", e);
      }

      return fileCount.get();
   }



   /***************************************************************************
    * Preview template rendering without writing files.
    *
    * @param templateDir template source directory
    * @param targetDir target output directory
    * @param variables template variables
    * @param manifest template manifest
    * @throws IOException if file operations fail
    * @throws TemplateRenderException if path rendering fails
    * @since 0.1.0
    ***************************************************************************/
   public void renderDryRun(Path templateDir, Path targetDir, Map<String, String> variables,
                            TemplateManifest manifest) throws IOException, TemplateRenderException
   {
      renderDryRun(templateDir, targetDir, variables, manifest, MergeOptions.disabled());
   }



   /***************************************************************************
    * Preview template rendering with merge support.
    *
    * @param templateDir template source directory
    * @param targetDir target output directory
    * @param variables template variables
    * @param manifest template manifest
    * @param mergeOptions merge configuration
    * @throws IOException if file operations fail
    * @throws TemplateRenderException if path rendering fails
    * @since 0.2.0
    ***************************************************************************/
   public void renderDryRun(Path templateDir, Path targetDir, Map<String, String> variables,
                            TemplateManifest manifest, MergeOptions mergeOptions)
         throws IOException, TemplateRenderException
   {
      Path        sourceDir      = resolveTemplateSource(templateDir);
      Set<String> ignorePatterns = manifest.ignore() != null
         ? new HashSet<>(manifest.ignore())
         : new HashSet<>();
      ignorePatterns.add(".git");
      ignorePatterns.add("template.yaml");

      AtomicReference<TemplateRenderException> renderError = new AtomicReference<>();

      Files.walkFileTree(sourceDir, new SimpleFileVisitor<>()
      {
         @Override
         public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
         {
            if(shouldIgnore(sourceDir.relativize(dir).toString(), ignorePatterns))
            {
               return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
         }


         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
         {
            Path relativePath = sourceDir.relativize(file);
            if(shouldIgnore(relativePath.toString(), ignorePatterns))
            {
               return FileVisitResult.CONTINUE;
            }

            try
            {
               String renderedPath = renderPath(relativePath.toString(), variables);
               Path   targetPath   = targetDir.resolve(renderedPath);

               //////////////////////////////////////////////////////////////////
               // Check merge status for preview                               //
               //////////////////////////////////////////////////////////////////
               String status = "";
               if(mergeOptions.enabled())
               {
                  if(mergeOptions.matchesExclude(renderedPath))
                  {
                     status = "[skip] ";
                  }
                  else if(Files.exists(targetPath))
                  {
                     boolean hasIncludes = mergeOptions.includePatterns() != null
                        && !mergeOptions.includePatterns().isEmpty();
                     if(hasIncludes && mergeOptions.matchesInclude(renderedPath))
                     {
                        status = "[overwrite] ";
                     }
                     else
                     {
                        status = "[skip] ";
                     }
                  }
               }

               System.out.println("  " + status + targetPath);
            }
            catch(TemplateRenderException e)
            {
               renderError.set(e);
               return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
         }
      });

      if(renderError.get() != null)
      {
         throw renderError.get();
      }
   }



   /***************************************************************************
    * Render a path string with Velocity variable substitution.
    *
    * @param path path string with $var placeholders
    * @param variables template variables
    * @return rendered path
    * @throws TemplateRenderException if rendering fails
    * @since 0.1.0
    ***************************************************************************/
   public String renderPath(String path, Map<String, String> variables)
         throws TemplateRenderException
   {
      /////////////////////////////////////////////////////////////////////////
      // Convert __VARNAME__ placeholders to $VARNAME for Velocity.          //
      // This allows templates to use __VARNAME__ in paths, which is safer   //
      // for shells and build tools that might interpret $ as a variable.    //
      /////////////////////////////////////////////////////////////////////////
      String normalizedPath = path.replaceAll("__([a-zA-Z][a-zA-Z0-9]*)__", "\\$$1");
      return renderContent(normalizedPath, variables);
   }



   /***************************************************************************
    * Render content using Velocity with string variables.
    *
    * @param content template content
    * @param variables template variables (String values)
    * @return rendered content
    * @throws TemplateRenderException if rendering fails
    * @since 0.1.0
    ***************************************************************************/
   public String renderContent(String content, Map<String, String> variables)
         throws TemplateRenderException
   {
      VelocityContext context = createContext(variables);
      return evaluateTemplate(content, context);
   }



   /***************************************************************************
    * Render content using Velocity with object variables.
    *
    * Supports complex objects like lists for #foreach directives.
    *
    * @param content template content
    * @param variables template variables (Object values)
    * @return rendered content
    * @throws TemplateRenderException if rendering fails
    * @since 0.2.0
    ***************************************************************************/
   public String renderContentWithObjects(String content, Map<String, Object> variables)
         throws TemplateRenderException
   {
      VelocityContext context = new VelocityContext();
      context.put("str", stringTool);
      for(Map.Entry<String, Object> entry : variables.entrySet())
      {
         context.put(entry.getKey(), entry.getValue());
      }
      return evaluateTemplate(content, context);
   }



   /***************************************************************************
    * Evaluate computed variables and merge with input variables.
    *
    * Computed variables are evaluated in order. Each computed variable
    * can reference previously defined variables (prompts or earlier computed).
    * If a computed variable references an undefined variable, rendering fails.
    *
    * @param computed list of computed variable definitions (may be null)
    * @param variables input variables from prompts
    * @return merged map containing original and computed variables
    * @throws TemplateRenderException if evaluation fails
    * @since 0.2.0
    ***************************************************************************/
   public Map<String, String> evaluateComputed(List<ComputedVariable> computed,
                                                Map<String, String> variables)
         throws TemplateRenderException
   {
      if(computed == null || computed.isEmpty())
      {
         return variables;
      }

      Map<String, String> result = new HashMap<>(variables);

      for(ComputedVariable cv : computed)
      {
         String value = renderContent(cv.expression(), result);
         result.put(cv.name(), value);
      }

      return result;
   }



   /***************************************************************************
    * Create a Velocity context with string variables and tools.
    *
    * @param variables template variables
    * @return configured VelocityContext
    * @since 0.2.0
    ***************************************************************************/
   private VelocityContext createContext(Map<String, String> variables)
   {
      VelocityContext context = new VelocityContext();
      context.put("str", stringTool);
      for(Map.Entry<String, String> entry : variables.entrySet())
      {
         context.put(entry.getKey(), entry.getValue());
      }
      return context;
   }



   /***************************************************************************
    * Evaluate a template string with the given context.
    *
    * @param template template string
    * @param context Velocity context
    * @return rendered string
    * @throws TemplateRenderException if rendering fails
    * @since 0.2.0
    ***************************************************************************/
   private String evaluateTemplate(String template, VelocityContext context)
         throws TemplateRenderException
   {
      try
      {
         StringWriter writer = new StringWriter();
         boolean      result = velocityEngine.evaluate(context, writer, "template", template);

         if(!result)
         {
            throw new TemplateRenderException("Template evaluation failed");
         }

         String output = writer.toString();

         /////////////////////////////////////////////////////////////////////
         // Check for undefined references that weren't resolved           //
         /////////////////////////////////////////////////////////////////////
         checkForUndefinedReferences(template, output, context);

         return output;
      }
      catch(ParseErrorException e)
      {
         throw new TemplateRenderException("Template syntax error: " + e.getMessage(), e);
      }
      catch(MethodInvocationException e)
      {
         throw new TemplateRenderException("Template method error: " + e.getMessage(), e);
      }
      catch(ResourceNotFoundException e)
      {
         throw new TemplateRenderException("Template resource not found: " + e.getMessage(), e);
      }
   }



   /***************************************************************************
    * Check for undefined variable references in the output.
    *
    * If a variable reference like $name appears in both input and output
    * unchanged, it means the variable was not defined.
    *
    * @param template original template
    * @param output rendered output
    * @param context Velocity context
    * @throws TemplateRenderException if undefined references found
    * @since 0.2.0
    ***************************************************************************/
   private void checkForUndefinedReferences(String template, String output,
                                             VelocityContext context)
         throws TemplateRenderException
   {
      Matcher matcher = UNDEFINED_REF_PATTERN.matcher(output);
      while(matcher.find())
      {
         String varName = matcher.group(2);

         /////////////////////////////////////////////////////////////////////
         // Skip if it's a tool reference (like $str)                       //
         /////////////////////////////////////////////////////////////////////
         if("str".equals(varName))
         {
            continue;
         }

         /////////////////////////////////////////////////////////////////////
         // Skip if it looks like a Maven property (${name.something})      //
         // These are intentionally left as literals in templates           //
         /////////////////////////////////////////////////////////////////////
         int endPos = matcher.end();
         if(endPos < output.length() && output.charAt(endPos) == '.')
         {
            continue;
         }

         /////////////////////////////////////////////////////////////////////
         // If the variable is not in context, it's undefined               //
         /////////////////////////////////////////////////////////////////////
         if(context.get(varName) == null)
         {
            /////////////////////////////////////////////////////////////////
            // Check if this exact reference was in the original template  //
            /////////////////////////////////////////////////////////////////
            String fullRef = matcher.group(0);
            if(template.contains(fullRef))
            {
               //////////////////////////////////////////////////////////////
               // Collect available variables and find similar ones        //
               //////////////////////////////////////////////////////////////
               List<String> available = collectAvailableVariables(context);
               List<String> suggestions = suggestionEngine.findSimilar(varName, available);
               throw new TemplateRenderException(varName, suggestions, available);
            }
         }
      }
   }



   /***************************************************************************
    * Collect all available variable names from the context.
    *
    * @param context Velocity context
    * @return list of variable names
    * @since 0.2.0
    ***************************************************************************/
   private List<String> collectAvailableVariables(VelocityContext context)
   {
      List<String> variables = new ArrayList<>();
      for(Object key : context.getKeys())
      {
         String name = key.toString();
         /////////////////////////////////////////////////////////////////////
         // Skip internal tools like $str                                   //
         /////////////////////////////////////////////////////////////////////
         if(!"str".equals(name))
         {
            variables.add(name);
         }
      }
      return variables;
   }



   /***************************************************************************
    * Resolve the template source directory.
    *
    * @param templateDir template root directory
    * @return source directory for templates
    * @since 0.1.0
    ***************************************************************************/
   private Path resolveTemplateSource(Path templateDir)
   {
      Path templateSubdir = templateDir.resolve(TEMPLATE_DIR);
      if(Files.isDirectory(templateSubdir))
      {
         return templateSubdir;
      }
      return templateDir;
   }



   /***************************************************************************
    * Check if a file should be ignored.
    *
    * @param path relative path
    * @param patterns ignore patterns
    * @return true if file should be ignored
    * @since 0.1.0
    ***************************************************************************/
   private boolean shouldIgnore(String path, Set<String> patterns)
   {
      for(String pattern : patterns)
      {
         if(path.equals(pattern) || path.startsWith(pattern + "/"))
         {
            return true;
         }
         if(pattern.startsWith("*."))
         {
            String ext = pattern.substring(1);
            if(path.endsWith(ext))
            {
               return true;
            }
         }
      }
      return false;
   }



   /***************************************************************************
    * Check if a file is binary.
    *
    * @param file file path
    * @return true if file is binary
    * @since 0.1.0
    ***************************************************************************/
   private boolean isBinaryFile(Path file)
   {
      String name = file.getFileName().toString().toLowerCase();
      for(String ext : BINARY_EXTENSIONS)
      {
         if(name.endsWith(ext))
         {
            return true;
         }
      }
      return false;
   }



   /***************************************************************************
    * Check if a file should be copied verbatim without template processing.
    *
    * @param file file path
    * @return true if file should be copied verbatim
    * @since 0.2.0
    ***************************************************************************/
   private boolean isVerbatimFile(Path file)
   {
      String name = file.getFileName().toString();
      return VERBATIM_FILENAMES.contains(name);
   }



   /***************************************************************************
    * Inner class to hold exception reference for use in visitor.
    *
    * @param <T> exception type
    * @since 0.2.0
    ***************************************************************************/
   private static class AtomicReference<T>
   {
      private T value;



      /*************************************************************************
       * Set the value.
       *
       * @param value value to set
       * @since 0.2.0
       *************************************************************************/
      public void set(T value)
      {
         this.value = value;
      }



      /*************************************************************************
       * Get the value.
       *
       * @return stored value
       * @since 0.2.0
       *************************************************************************/
      public T get()
      {
         return value;
      }
   }
}
