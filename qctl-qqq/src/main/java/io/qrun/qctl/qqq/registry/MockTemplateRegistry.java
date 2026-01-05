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
import java.util.List;
import java.util.Optional;
import io.qrun.qctl.qqq.template.ComputedVariable;
import io.qrun.qctl.qqq.template.TemplateManifest;
import io.qrun.qctl.qqq.template.TemplateResolver;
import io.qrun.qctl.qqq.template.Transform;


/*******************************************************************************
 * Mock template registry with embedded template data.
 *
 * Provides stable template listing without network dependencies.
 * Used for development, testing, and as a fallback when Voyage is unavailable.
 *
 * @since 0.2.0
 *******************************************************************************/
public class MockTemplateRegistry implements TemplateRegistry
{
   private static final String GITHUB_REGISTRY = "https://github.com/QRun-IO";
   private static final List<TemplateInfo> MOCK_TEMPLATES = List.of(
      new TemplateInfo(
         "new-qqq-application",
         "QQQ Application Starter",
         "1.0.0",
         "Complete QQQ application with entities, database, and dashboard",
         GITHUB_REGISTRY + "/new-qqq-application-template.git",
         2,
         "0.2.0",
         List.of("qqq", "java", "starter"),
         "QRun-IO",
         List.of(
            new TemplateManifest.Prompt("projectName", "Project name", "text", "my-qqq-app", null, null, true),
            new TemplateManifest.Prompt("groupId", "Maven group ID", "text", "com.example", null, null, true),
            new TemplateManifest.Prompt("artifactId", "Maven artifact ID", "text", "my-qqq-app", null, null, true),
            new TemplateManifest.Prompt("packageName", "Java package name", "text", "com.example.myapp", null, null, true)
         ),
         List.of(
            new ComputedVariable("packagePath", "$str.replace($packageName, '.', '/')")
         ),
         List.of(
            new Transform("delete", "**/.gitkeep", null)
         ),
         List.of(
            new TemplateManifest.PostGenHook("Initialize git repository", "git init", null, true, "init"),
            new TemplateManifest.PostGenHook("Build project", "mvn clean compile -DskipTests", null, true, "build")
         ),
         List.of(".git", ".github", ".idea", "target", "*.iml", "LICENSE", "README.md")
      ),
      new TemplateInfo(
         "qqq-web-api",
         "QQQ Web API",
         "1.0.0",
         "REST API application with Javalin middleware and OpenAPI docs",
         GITHUB_REGISTRY + "/qqq-web-api-template.git",
         2,
         "0.2.0",
         List.of("qqq", "java", "api", "rest"),
         "QRun-IO",
         List.of(
            new TemplateManifest.Prompt("projectName", "Project name", "text", "my-api", null, null, true),
            new TemplateManifest.Prompt("groupId", "Maven group ID", "text", "com.example", null, null, true),
            new TemplateManifest.Prompt("artifactId", "Maven artifact ID", "text", "my-api", null, null, true),
            new TemplateManifest.Prompt("packageName", "Java package name", "text", "com.example.api", null, null, true)
         ),
         List.of(new ComputedVariable("packagePath", "$str.replace($packageName, '.', '/')")),
         List.of(new Transform("delete", "**/.gitkeep", null)),
         List.of(),
         List.of(".git", ".github", "target")
      ),
      new TemplateInfo(
         "qqq-cli-tool",
         "QQQ CLI Tool",
         "1.0.0",
         "Command-line tool with Picocli and native image support",
         GITHUB_REGISTRY + "/qqq-cli-tool-template.git",
         2,
         "0.2.0",
         List.of("qqq", "java", "cli", "native"),
         "QRun-IO",
         List.of(
            new TemplateManifest.Prompt("projectName", "Project name", "text", "my-cli", null, null, true),
            new TemplateManifest.Prompt("groupId", "Maven group ID", "text", "com.example", null, null, true),
            new TemplateManifest.Prompt("artifactId", "Maven artifact ID", "text", "my-cli", null, null, true),
            new TemplateManifest.Prompt("packageName", "Java package name", "text", "com.example.cli", null, null, true),
            new TemplateManifest.Prompt("commandName", "Main command name", "text", "mycli", null, null, true)
         ),
         List.of(new ComputedVariable("packagePath", "$str.replace($packageName, '.', '/')")),
         List.of(new Transform("delete", "**/.gitkeep", null)),
         List.of(),
         List.of(".git", ".github", "target")
      )
   );

   private final TemplateResolver resolver;



   /***************************************************************************
    * Default constructor.
    *
    * @since 0.2.0
    ***************************************************************************/
   public MockTemplateRegistry()
   {
      this.resolver = new TemplateResolver();
   }



   @Override
   public List<TemplateInfo> listTemplates()
   {
      return MOCK_TEMPLATES;
   }



   @Override
   public Optional<TemplateInfo> getTemplate(String id)
   {
      return MOCK_TEMPLATES.stream()
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
      // No-op for mock registry - data is embedded
   }
}
