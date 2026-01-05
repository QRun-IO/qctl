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

package io.qrun.qctl.qqq.registry;


import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import io.qrun.qctl.qqq.template.TemplateResolver;
import io.qrun.qctl.qqq.template.TemplatesHub;


/*******************************************************************************
 * Template registry backed by GitHub templates-hub repository.
 *
 * Fetches template index from raw.githubusercontent.com and clones templates
 * from GitHub repositories. Includes caching with configurable TTL.
 *
 * @since 0.2.0
 *******************************************************************************/
public class GitHubTemplateRegistry implements TemplateRegistry
{
   private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(15);

   private final TemplatesHub hub;
   private final TemplateResolver resolver;
   private final Duration cacheTtl;

   private List<TemplateInfo> cachedTemplates;
   private Instant cacheExpiry;



   /***************************************************************************
    * Construct with default cache TTL.
    *
    * @since 0.2.0
    ***************************************************************************/
   public GitHubTemplateRegistry()
   {
      this(DEFAULT_CACHE_TTL);
   }



   /***************************************************************************
    * Construct with custom cache TTL.
    *
    * @param cacheTtl cache duration
    * @since 0.2.0
    ***************************************************************************/
   public GitHubTemplateRegistry(Duration cacheTtl)
   {
      this.hub = new TemplatesHub();
      this.resolver = new TemplateResolver();
      this.cacheTtl = cacheTtl;
   }



   @Override
   public List<TemplateInfo> listTemplates() throws IOException
   {
      if(cachedTemplates != null && cacheExpiry != null && Instant.now().isBefore(cacheExpiry))
      {
         return cachedTemplates;
      }

      TemplatesHub.TemplatesIndex index = hub.fetchIndex();
      if(index.templates() == null)
      {
         cachedTemplates = List.of();
      }
      else
      {
         cachedTemplates = index.templates().stream()
            .map(entry -> convertToTemplateInfo(entry, index.registry()))
            .toList();
      }

      cacheExpiry = Instant.now().plus(cacheTtl);
      return cachedTemplates;
   }



   @Override
   public Optional<TemplateInfo> getTemplate(String id) throws IOException
   {
      return listTemplates().stream()
         .filter(t -> t.id().equals(id))
         .findFirst();
   }



   @Override
   public Path downloadTemplate(String id, String version) throws IOException
   {
      Optional<TemplateInfo> template = getTemplate(id);
      if(template.isEmpty())
      {
         throw new IOException("Template not found: " + id);
      }

      return resolver.resolve(template.get().downloadUrl(), version);
   }



   @Override
   public void refresh()
   {
      cachedTemplates = null;
      cacheExpiry = null;
   }



   /***************************************************************************
    * Convert hub entry to TemplateInfo.
    *
    * @param entry hub template entry
    * @param registry base registry URL
    * @return template info
    * @since 0.2.0
    ***************************************************************************/
   private TemplateInfo convertToTemplateInfo(TemplatesHub.TemplateEntry entry, String registry)
   {
      return new TemplateInfo(
         entry.id(),
         entry.name(),
         entry.version(),
         entry.description(),
         entry.getGitUrl(registry),
         entry.schemaVersion(),
         entry.minimumQctlVersion(),
         entry.tags(),
         entry.maintainer(),
         entry.prompts(),
         entry.computed(),
         entry.transforms(),
         entry.postGen(),
         entry.ignore()
      );
   }
}
