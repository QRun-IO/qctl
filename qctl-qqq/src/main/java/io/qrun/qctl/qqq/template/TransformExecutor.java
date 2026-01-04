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
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*******************************************************************************
 * Executes post-render transforms on generated files.
 *
 * Supports rename and delete operations using glob patterns.
 *
 * @since 0.2.0
 *******************************************************************************/
public class TransformExecutor
{
   /////////////////////////////////////////////////////////////////////////////
   // Pattern for variable substitution in replacement strings: ${varName}   //
   /////////////////////////////////////////////////////////////////////////////
   private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([a-zA-Z][a-zA-Z0-9_]*)}");



   /***************************************************************************
    * Execute a list of transforms on the target directory.
    *
    * @param targetDir target directory containing rendered files
    * @param transforms list of transforms to execute (may be null)
    * @param variables template variables for replacement substitution
    * @throws IOException if file operations fail
    * @throws TemplateRenderException if transform is invalid
    * @since 0.2.0
    ***************************************************************************/
   public void execute(Path targetDir, List<Transform> transforms, Map<String, String> variables)
         throws IOException, TemplateRenderException
   {
      if(transforms == null || transforms.isEmpty())
      {
         return;
      }

      for(Transform transform : transforms)
      {
         executeTransform(targetDir, transform, variables);
      }
   }



   /***************************************************************************
    * Execute a single transform.
    *
    * @param targetDir target directory
    * @param transform transform to execute
    * @param variables template variables
    * @throws IOException if file operations fail
    * @throws TemplateRenderException if transform is invalid
    * @since 0.2.0
    ***************************************************************************/
   private void executeTransform(Path targetDir, Transform transform, Map<String, String> variables)
         throws IOException, TemplateRenderException
   {
      String type = transform.type();

      if(Transform.TYPE_DELETE.equals(type))
      {
         executeDelete(targetDir, transform.pattern());
      }
      else if(Transform.TYPE_RENAME.equals(type))
      {
         executeRename(targetDir, transform.pattern(), transform.replacement(), variables);
      }
      else
      {
         throw new TemplateRenderException("Invalid transform type: " + type);
      }
   }



   /***************************************************************************
    * Execute a delete transform.
    *
    * @param targetDir target directory
    * @param pattern glob pattern to match files
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   private void executeDelete(Path targetDir, String pattern) throws IOException
   {
      List<Path> matchingFiles = findMatchingFiles(targetDir, pattern);

      for(Path file : matchingFiles)
      {
         Files.deleteIfExists(file);
      }
   }



   /***************************************************************************
    * Execute a rename transform.
    *
    * @param targetDir target directory
    * @param pattern glob pattern to match files
    * @param replacement replacement filename (may contain ${var} placeholders)
    * @param variables template variables for substitution
    * @throws IOException if file operations fail
    * @since 0.2.0
    ***************************************************************************/
   private void executeRename(Path targetDir, String pattern, String replacement,
                               Map<String, String> variables) throws IOException
   {
      List<Path> matchingFiles = findMatchingFiles(targetDir, pattern);
      String resolvedReplacement = substituteVariables(replacement, variables);

      for(Path file : matchingFiles)
      {
         Path newPath = file.getParent().resolve(resolvedReplacement);
         Files.move(file, newPath);
      }
   }



   /***************************************************************************
    * Find all files matching a glob pattern.
    *
    * @param targetDir target directory to search
    * @param pattern glob pattern
    * @return list of matching file paths
    * @throws IOException if traversal fails
    * @since 0.2.0
    ***************************************************************************/
   private List<Path> findMatchingFiles(Path targetDir, String pattern) throws IOException
   {
      List<Path>   matches     = new ArrayList<>();
      PathMatcher  matcher     = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

      Files.walkFileTree(targetDir, new SimpleFileVisitor<>()
      {
         @Override
         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
         {
            Path relativePath = targetDir.relativize(file);
            if(matcher.matches(relativePath))
            {
               matches.add(file);
            }
            return FileVisitResult.CONTINUE;
         }
      });

      return matches;
   }



   /***************************************************************************
    * Substitute ${varName} placeholders in a string.
    *
    * @param template string with placeholders
    * @param variables variable values
    * @return string with placeholders replaced
    * @since 0.2.0
    ***************************************************************************/
   private String substituteVariables(String template, Map<String, String> variables)
   {
      if(template == null || variables == null || variables.isEmpty())
      {
         return template;
      }

      Matcher      matcher = VARIABLE_PATTERN.matcher(template);
      StringBuffer result  = new StringBuffer();

      while(matcher.find())
      {
         String varName = matcher.group(1);
         String value   = variables.getOrDefault(varName, matcher.group(0));
         matcher.appendReplacement(result, Matcher.quoteReplacement(value));
      }
      matcher.appendTail(result);

      return result.toString();
   }
}
