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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


/*******************************************************************************
 * Fetches and manages the templates index from templates-hub.
 *
 * @since 0.1.0
 *******************************************************************************/
public class TemplatesHub
{
   private static final String HUB_URL =
      "https://raw.githubusercontent.com/QRun-IO/templates-hub/main/templates.yaml";
   private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
   private static final int TIMEOUT_SECONDS = 30;

   private TemplatesIndex cachedIndex;



   /***************************************************************************
    * Fetch the templates index from the hub.
    *
    * @return templates index
    * @throws IOException if fetch fails
    * @since 0.1.0
    ***************************************************************************/
   public TemplatesIndex fetchIndex() throws IOException
   {
      if(cachedIndex != null)
      {
         return cachedIndex;
      }

      try
      {
         HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

         HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(HUB_URL))
            .GET()
            .build();

         HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());

         if(response.statusCode() != 200)
         {
            throw new IOException("Failed to fetch templates index: HTTP " + response.statusCode());
         }

         cachedIndex = YAML.readValue(response.body(), TemplatesIndex.class);
         return cachedIndex;
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw new IOException("Interrupted while fetching templates index", e);
      }
   }



   /***************************************************************************
    * Find a template by ID.
    *
    * @param templateId template ID
    * @return template entry if found
    * @throws IOException if fetch fails
    * @since 0.1.0
    ***************************************************************************/
   public Optional<TemplateEntry> findById(String templateId) throws IOException
   {
      TemplatesIndex index = fetchIndex();
      if(index.templates() == null)
      {
         return Optional.empty();
      }
      return index.templates().stream()
         .filter(t -> t.id().equals(templateId))
         .findFirst();
   }



   /***************************************************************************
    * Templates index from templates-hub.
    *
    * @since 0.1.0
    ***************************************************************************/
   @JsonIgnoreProperties(ignoreUnknown = true)
   public record TemplatesIndex(
      String version,
      String registry,
      List<TemplateEntry> templates
   )
   {
   }



   /***************************************************************************
    * A template entry from the index.
    *
    * @since 0.1.0
    ***************************************************************************/
   @JsonIgnoreProperties(ignoreUnknown = true)
   public record TemplateEntry(
      String id,
      String name,
      String description,
      String repo,
      String version,
      List<String> tags,
      String maintainer,
      List<TemplateManifest.Prompt> prompts,
      List<TemplateManifest.PostGenHook> postGen,
      List<String> ignore
   )
   {
      /*************************************************************************
       * Get the git clone URL for this template.
       *
       * @param registry base registry URL
       * @return git clone URL
       * @since 0.1.0
       *************************************************************************/
      public String getGitUrl(String registry)
      {
         return registry + "/" + repo + ".git";
      }
   }
}
