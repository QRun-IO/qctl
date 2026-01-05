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


/*******************************************************************************
 * Factory for creating template registry instances.
 *
 * Selects the appropriate registry implementation based on configuration.
 * Currently supports: GitHub (default), Mock (for testing/offline).
 * Future: Voyage API registry.
 *
 * @since 0.2.0
 *******************************************************************************/
public final class TemplateRegistryFactory
{
   private static final String REGISTRY_TYPE_ENV = "QCTL_REGISTRY_TYPE";
   private static final String REGISTRY_TYPE_GITHUB = "github";
   private static final String REGISTRY_TYPE_MOCK = "mock";

   private static TemplateRegistry instance;



   /***************************************************************************
    * Private constructor - utility class.
    *
    * @since 0.2.0
    ***************************************************************************/
   private TemplateRegistryFactory()
   {
   }



   /***************************************************************************
    * Get the configured template registry.
    *
    * Registry type is determined by QCTL_REGISTRY_TYPE environment variable:
    * - "github" (default): GitHub templates-hub repository
    * - "mock": Embedded mock templates for testing/offline
    * - "voyage": Voyage API (future)
    *
    * @return template registry instance
    * @since 0.2.0
    ***************************************************************************/
   public static TemplateRegistry getRegistry()
   {
      if(instance == null)
      {
         instance = createRegistry();
      }
      return instance;
   }



   /***************************************************************************
    * Create a new registry instance based on configuration.
    *
    * @return new template registry
    * @since 0.2.0
    ***************************************************************************/
   private static TemplateRegistry createRegistry()
   {
      String registryType = System.getenv(REGISTRY_TYPE_ENV);
      if(registryType == null || registryType.isBlank())
      {
         registryType = REGISTRY_TYPE_GITHUB;
      }

      return switch(registryType.toLowerCase())
      {
         case REGISTRY_TYPE_MOCK -> new MockTemplateRegistry();
         case REGISTRY_TYPE_GITHUB -> new GitHubTemplateRegistry();
         default -> new GitHubTemplateRegistry();
      };
   }



   /***************************************************************************
    * Reset the cached registry instance.
    *
    * Used for testing to allow switching registry types.
    *
    * @since 0.2.0
    ***************************************************************************/
   public static void reset()
   {
      instance = null;
   }



   /***************************************************************************
    * Set a specific registry instance.
    *
    * Used for testing to inject mock registries.
    *
    * @param registry registry to use
    * @since 0.2.0
    ***************************************************************************/
   public static void setRegistry(TemplateRegistry registry)
   {
      instance = registry;
   }
}
