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

package io.qrun.qctl.e2e.harness;


import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.qrun.qctl.qqq.registry.TemplateInfo;
import io.qrun.qctl.qqq.registry.TemplateRegistry;
import io.qrun.qctl.qqq.template.TemplateManifest;


/*******************************************************************************
 * Test template registry that loads fixtures from test resources.
 *
 * Provides fully offline, deterministic template loading for E2E tests.
 * Templates are loaded from src/test/resources/fixtures/templates/.
 *
 * @since 0.2.0
 *******************************************************************************/
public class TestTemplateRegistry implements TemplateRegistry
{
   private static final String FIXTURES_DIR = "fixtures/templates";

   private final Map<String, Path> templatePaths = new HashMap<>();
   private final Map<String, TemplateInfo> templateInfos = new HashMap<>();



   /***************************************************************************
    * Register a fixture template by ID.
    *
    * @param id template ID
    * @param fixtureDir path to fixture directory containing template.yaml
    * @return this for fluent chaining
    * @throws IOException if template cannot be loaded
    * @since 0.2.0
    ***************************************************************************/
   public TestTemplateRegistry addFixture(String id, Path fixtureDir) throws IOException
   {
      if(!Files.exists(fixtureDir))
      {
         throw new IOException("Fixture directory does not exist: " + fixtureDir);
      }

      templatePaths.put(id, fixtureDir);

      Path manifestPath = fixtureDir.resolve("template.yaml");
      if(Files.exists(manifestPath))
      {
         TemplateManifest manifest = TemplateManifest.load(fixtureDir);
         templateInfos.put(id, createTemplateInfo(id, manifest, fixtureDir));
      }
      else
      {
         //////////////////////////////////////////////////////////////////
         // Create minimal info for templates without manifests          //
         //////////////////////////////////////////////////////////////////
         templateInfos.put(id, createMinimalInfo(id, fixtureDir));
      }

      return this;
   }



   /***************************************************************************
    * Load a fixture from classpath resources.
    *
    * @param id template ID (also the directory name under fixtures/templates/)
    * @return this for fluent chaining
    * @throws IOException if fixture cannot be loaded
    * @since 0.2.0
    ***************************************************************************/
   public TestTemplateRegistry loadFromResources(String id) throws IOException
   {
      try
      {
         java.net.URL url = getClass().getClassLoader().getResource(FIXTURES_DIR + "/" + id);
         if(url == null)
         {
            throw new IOException("Fixture not found in resources: " + id);
         }
         Path fixtureDir = Path.of(url.toURI());
         return addFixture(id, fixtureDir);
      }
      catch(URISyntaxException e)
      {
         throw new IOException("Invalid fixture path: " + id, e);
      }
   }



   /***************************************************************************
    * Create TemplateInfo from manifest.
    *
    * @param id template ID
    * @param manifest the loaded manifest
    * @param fixtureDir the fixture directory path
    * @return TemplateInfo record
    * @since 0.2.0
    ***************************************************************************/
   private TemplateInfo createTemplateInfo(String id, TemplateManifest manifest, Path fixtureDir)
   {
      return new TemplateInfo(
         id,
         manifest.name() != null ? manifest.name() : id,
         manifest.version() != null ? manifest.version() : "1.0.0",
         manifest.description() != null ? manifest.description() : "Test fixture template",
         fixtureDir.toUri().toString(),
         manifest.getEffectiveSchemaVersion(),
         manifest.minimumQctlVersion(),
         List.of("test", "fixture"),
         "test",
         manifest.prompts() != null ? manifest.prompts() : List.of(),
         manifest.computed() != null ? manifest.computed() : List.of(),
         manifest.transforms() != null ? manifest.transforms() : List.of(),
         manifest.postGen() != null ? manifest.postGen() : List.of(),
         manifest.ignore() != null ? manifest.ignore() : List.of()
      );
   }



   /***************************************************************************
    * Create minimal TemplateInfo for fixtures without manifest.
    *
    * @param id template ID
    * @param fixtureDir the fixture directory path
    * @return TemplateInfo record
    * @since 0.2.0
    ***************************************************************************/
   private TemplateInfo createMinimalInfo(String id, Path fixtureDir)
   {
      return new TemplateInfo(
         id,
         id,
         "1.0.0",
         "Test fixture template (no manifest)",
         fixtureDir.toUri().toString(),
         1,
         null,
         List.of("test"),
         "test",
         List.of(),
         List.of(),
         List.of(),
         List.of(),
         List.of()
      );
   }



   @Override
   public List<TemplateInfo> listTemplates()
   {
      return new ArrayList<>(templateInfos.values());
   }



   @Override
   public Optional<TemplateInfo> getTemplate(String id)
   {
      return Optional.ofNullable(templateInfos.get(id));
   }



   @Override
   public Path downloadTemplate(String id, String version) throws IOException
   {
      Path fixturePath = templatePaths.get(id);
      if(fixturePath == null)
      {
         throw new IOException("Fixture template not found: " + id);
      }
      return fixturePath;
   }



   @Override
   public void refresh()
   {
      // No-op for test registry
   }



   /***************************************************************************
    * Clear all registered fixtures.
    *
    * @return this for fluent chaining
    * @since 0.2.0
    ***************************************************************************/
   public TestTemplateRegistry clear()
   {
      templatePaths.clear();
      templateInfos.clear();
      return this;
   }
}
