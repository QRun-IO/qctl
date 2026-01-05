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


import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 * Unit tests for MockTemplateRegistry.
 *
 * @since 0.2.0
 *******************************************************************************/
class MockTemplateRegistryTest
{
   private MockTemplateRegistry registry;



   @BeforeEach
   void setUp()
   {
      registry = new MockTemplateRegistry();
   }



   @Test
   void testListTemplates_returnsEmbeddedTemplates()
   {
      List<TemplateInfo> templates = registry.listTemplates();

      assertThat(templates).isNotEmpty();
      assertThat(templates).hasSizeGreaterThanOrEqualTo(3);
   }



   @Test
   void testListTemplates_containsExpectedTemplates()
   {
      List<TemplateInfo> templates = registry.listTemplates();

      List<String> ids = templates.stream()
         .map(TemplateInfo::id)
         .toList();

      assertThat(ids).contains("new-qqq-application", "qqq-web-api", "qqq-cli-tool");
   }



   @Test
   void testGetTemplate_existingTemplate_returnsTemplate()
   {
      Optional<TemplateInfo> result = registry.getTemplate("new-qqq-application");

      assertThat(result).isPresent();
      assertThat(result.get().id()).isEqualTo("new-qqq-application");
      assertThat(result.get().name()).isEqualTo("QQQ Application Starter");
      assertThat(result.get().version()).isEqualTo("1.0.0");
   }



   @Test
   void testGetTemplate_nonExistingTemplate_returnsEmpty()
   {
      Optional<TemplateInfo> result = registry.getTemplate("non-existent");

      assertThat(result).isEmpty();
   }



   @Test
   void testTemplateInfo_hasExpectedMetadata()
   {
      Optional<TemplateInfo> result = registry.getTemplate("new-qqq-application");
      assertThat(result).isPresent();

      TemplateInfo info = result.get();
      assertThat(info.schemaVersion()).isEqualTo(2);
      assertThat(info.minimumQctlVersion()).isEqualTo("0.2.0");
      assertThat(info.tags()).contains("qqq", "java", "starter");
      assertThat(info.maintainer()).isEqualTo("QRun-IO");
   }



   @Test
   void testTemplateInfo_hasPrompts()
   {
      Optional<TemplateInfo> result = registry.getTemplate("new-qqq-application");
      assertThat(result).isPresent();

      TemplateInfo info = result.get();
      assertThat(info.prompts()).isNotEmpty();
      assertThat(info.prompts()).hasSize(4);

      List<String> promptNames = info.prompts().stream()
         .map(p -> p.name())
         .toList();
      assertThat(promptNames).containsExactly("projectName", "groupId", "artifactId", "packageName");
   }



   @Test
   void testTemplateInfo_hasComputedVariables()
   {
      Optional<TemplateInfo> result = registry.getTemplate("new-qqq-application");
      assertThat(result).isPresent();

      TemplateInfo info = result.get();
      assertThat(info.computed()).isNotEmpty();
      assertThat(info.computed().get(0).name()).isEqualTo("packagePath");
   }



   @Test
   void testTemplateInfo_hasTransforms()
   {
      Optional<TemplateInfo> result = registry.getTemplate("new-qqq-application");
      assertThat(result).isPresent();

      TemplateInfo info = result.get();
      assertThat(info.transforms()).isNotEmpty();
      assertThat(info.transforms().get(0).type()).isEqualTo("delete");
   }



   @Test
   void testTemplateInfo_toManifest_convertsCorrectly()
   {
      Optional<TemplateInfo> result = registry.getTemplate("new-qqq-application");
      assertThat(result).isPresent();

      var manifest = result.get().toManifest();
      assertThat(manifest.id()).isEqualTo("new-qqq-application");
      assertThat(manifest.name()).isEqualTo("QQQ Application Starter");
      assertThat(manifest.schemaVersion()).isEqualTo(2);
      assertThat(manifest.prompts()).hasSize(4);
   }



   @Test
   void testRefresh_doesNothing()
   {
      // Refresh is a no-op for mock registry - just ensure it doesn't throw
      registry.refresh();

      // Templates should still be available
      assertThat(registry.listTemplates()).isNotEmpty();
   }
}
