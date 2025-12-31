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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;


/*******************************************************************************
 * Renders templates using Handlebars.
 *
 * Supports path substitution ({{projectName}}/src) and content templating.
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

   private final Handlebars handlebars;



   /***************************************************************************
    * Create a new template engine.
    *
    * @since 0.1.0
    ***************************************************************************/
   public TemplateEngine()
   {
      this.handlebars = new Handlebars();
      registerHelpers();
   }



   /***************************************************************************
    * Register custom Handlebars helpers.
    *
    * @since 0.1.0
    ***************************************************************************/
   private void registerHelpers()
   {
      // lowercase helper
      handlebars.registerHelper("lowercase", (context, options) ->
         context != null ? context.toString().toLowerCase() : "");

      // uppercase helper
      handlebars.registerHelper("uppercase", (context, options) ->
         context != null ? context.toString().toUpperCase() : "");

      // camelCase helper
      handlebars.registerHelper("camelCase", (context, options) ->
      {
         if(context == null)
         {
            return "";
         }
         String s = context.toString();
         if(s.isEmpty())
         {
            return "";
         }
         return Character.toLowerCase(s.charAt(0)) + s.substring(1);
      });

      // PascalCase helper
      handlebars.registerHelper("pascalCase", (context, options) ->
      {
         if(context == null)
         {
            return "";
         }
         String s = context.toString();
         if(s.isEmpty())
         {
            return "";
         }
         return Character.toUpperCase(s.charAt(0)) + s.substring(1);
      });

      // kebab-case helper
      handlebars.registerHelper("kebabCase", (context, options) ->
      {
         if(context == null)
         {
            return "";
         }
         return context.toString()
            .replaceAll("([a-z])([A-Z])", "$1-$2")
            .toLowerCase();
      });

      // snake_case helper
      handlebars.registerHelper("snakeCase", (context, options) ->
      {
         if(context == null)
         {
            return "";
         }
         return context.toString()
            .replaceAll("([a-z])([A-Z])", "$1_$2")
            .toLowerCase();
      });
   }



   /***************************************************************************
    * Render templates to the target directory.
    *
    * @param templateDir template source directory
    * @param targetDir target output directory
    * @param variables template variables
    * @param manifest template manifest
    * @return number of files created
    * @throws IOException if rendering fails
    * @since 0.1.0
    ***************************************************************************/
   public int render(Path templateDir, Path targetDir, Map<String, String> variables,
                     TemplateManifest manifest) throws IOException
   {
      Path       sourceDir = resolveTemplateSource(templateDir);
      Set<String> ignorePatterns = manifest.ignore() != null
         ? new HashSet<>(manifest.ignore())
         : new HashSet<>();
      ignorePatterns.add(".git");
      ignorePatterns.add("template.yaml");

      AtomicInteger fileCount = new AtomicInteger();

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

            Path relativePath = sourceDir.relativize(dir);
            String renderedPath = renderPath(relativePath.toString(), variables);
            Path   targetPath = targetDir.resolve(renderedPath);
            Files.createDirectories(targetPath);
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

            String renderedPath = renderPath(relativePath.toString(), variables);
            Path   targetPath = targetDir.resolve(renderedPath);

            if(isBinaryFile(file))
            {
               Files.copy(file, targetPath);
            }
            else
            {
               String content = Files.readString(file);
               String rendered = renderContent(content, variables);
               Files.writeString(targetPath, rendered);
            }
            fileCount.incrementAndGet();
            System.out.println("  " + renderedPath);
            return FileVisitResult.CONTINUE;
         }
      });

      return fileCount.get();
   }



   /***************************************************************************
    * Preview template rendering without writing files.
    *
    * @param templateDir template source directory
    * @param targetDir target output directory
    * @param variables template variables
    * @param manifest template manifest
    * @throws IOException if rendering fails
    * @since 0.1.0
    ***************************************************************************/
   public void renderDryRun(Path templateDir, Path targetDir, Map<String, String> variables,
                            TemplateManifest manifest) throws IOException
   {
      Path       sourceDir = resolveTemplateSource(templateDir);
      Set<String> ignorePatterns = manifest.ignore() != null
         ? new HashSet<>(manifest.ignore())
         : new HashSet<>();
      ignorePatterns.add(".git");
      ignorePatterns.add("template.yaml");

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

            String renderedPath = renderPath(relativePath.toString(), variables);
            System.out.println("  " + targetDir.resolve(renderedPath));
            return FileVisitResult.CONTINUE;
         }
      });
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
    * Render a path string with variable substitution.
    *
    * @param path path string with {{var}} placeholders
    * @param variables template variables
    * @return rendered path
    * @since 0.1.0
    ***************************************************************************/
   private String renderPath(String path, Map<String, String> variables)
   {
      String result = path;
      for(Map.Entry<String, String> entry : variables.entrySet())
      {
         result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
      }
      return result;
   }



   /***************************************************************************
    * Render content using Handlebars.
    *
    * @param content template content
    * @param variables template variables
    * @return rendered content
    * @since 0.1.0
    ***************************************************************************/
   private String renderContent(String content, Map<String, String> variables)
   {
      try
      {
         Template template = handlebars.compileInline(content);
         return template.apply(variables);
      }
      catch(IOException e)
      {
         // If template compilation fails, return content as-is
         return content;
      }
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
}
