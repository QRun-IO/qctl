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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


/*******************************************************************************
 * Represents a template manifest (template.yaml).
 *
 * Schema v2 adds: schemaVersion, minimumQctlVersion, prompts[].required,
 * postGen[].phase. Backward compatible with v1 manifests.
 *
 * @since 0.1.0
 *******************************************************************************/
@JsonIgnoreProperties(ignoreUnknown = true)
public record TemplateManifest(
   Integer schemaVersion,
   String id,
   String name,
   String version,
   String description,
   String minimumQctlVersion,
   List<Prompt> prompts,
   List<ComputedVariable> computed,
   List<Transform> transforms,
   List<PostGenHook> postGen,
   List<String> ignore
)
{
   private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
   private static final String MANIFEST_FILE = "template.yaml";
   private static final int DEFAULT_SCHEMA_VERSION = 1;



   /***************************************************************************
    * Get effective schema version (defaults to 1 for backward compatibility).
    *
    * @return schema version
    * @since 0.2.0
    ***************************************************************************/
   public int getEffectiveSchemaVersion()
   {
      return schemaVersion != null ? schemaVersion : DEFAULT_SCHEMA_VERSION;
   }



   /***************************************************************************
    * Load a template manifest from a directory.
    *
    * @param templateDir directory containing template.yaml
    * @return parsed manifest
    * @throws IOException if manifest cannot be read
    * @since 0.1.0
    ***************************************************************************/
   public static TemplateManifest load(Path templateDir) throws IOException
   {
      Path manifestPath = templateDir.resolve(MANIFEST_FILE);
      if(!Files.exists(manifestPath))
      {
         throw new IOException("template.yaml not found in " + templateDir);
      }

      try
      {
         return YAML.readValue(manifestPath.toFile(), TemplateManifest.class);
      }
      catch(JsonProcessingException e)
      {
         throw new IOException(formatYamlError(e, manifestPath), e);
      }
   }



   /***************************************************************************
    * Format a YAML parsing error with line and column information.
    *
    * @param e the JSON processing exception
    * @param manifestPath path to the manifest file
    * @return formatted error message
    * @since 0.2.0
    ***************************************************************************/
   private static String formatYamlError(JsonProcessingException e, Path manifestPath)
   {
      JsonLocation location = e.getLocation();
      StringBuilder sb = new StringBuilder();
      sb.append("YAML parse error in ").append(manifestPath.getFileName());

      if(location != null)
      {
         int line = location.getLineNr();
         int col = location.getColumnNr();
         sb.append(" (line ").append(line);
         if(col >= 0)
         {
            sb.append(", column ").append(col);
         }
         sb.append(")");
      }

      //////////////////////////////////////////////////////////////////////////
      // Extract the original error message without the location suffix       //
      //////////////////////////////////////////////////////////////////////////
      String originalMessage = e.getOriginalMessage();
      if(originalMessage != null && !originalMessage.isEmpty())
      {
         sb.append(": ").append(originalMessage);
      }

      return sb.toString();
   }



   /***************************************************************************
    * A prompt definition for collecting user input.
    *
    * @since 0.1.0
    ***************************************************************************/
   @JsonIgnoreProperties(ignoreUnknown = true)
   public record Prompt(
      String name,
      String message,
      String type,
      String defaultValue,
      List<String> choices,
      PromptValidation validation,
      Boolean required
   )
   {
      /*************************************************************************
       * Check if this prompt is required.
       *
       * @return true if required (defaults to true if not specified)
       * @since 0.2.0
       *************************************************************************/
      public boolean isRequired()
      {
         return required == null || required;
      }
   }



   /***************************************************************************
    * A post-generation hook definition.
    *
    * @since 0.1.0
    ***************************************************************************/
   @JsonIgnoreProperties(ignoreUnknown = true)
   public record PostGenHook(
      String name,
      String command,
      String workDir,
      Boolean optional,
      String phase
   )
   {
   }
}
