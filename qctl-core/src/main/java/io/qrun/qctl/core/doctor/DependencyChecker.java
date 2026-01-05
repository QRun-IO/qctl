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

package io.qrun.qctl.core.doctor;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*******************************************************************************
 * Checks for required external dependencies (Java, Maven, Git, Docker).
 *
 * Provides both individual checks and a comprehensive report.
 *
 * @since 0.2.0
 *******************************************************************************/
public class DependencyChecker
{
   private static final int    DEFAULT_TIMEOUT_MS   = 5000;
   private static final int    THREAD_JOIN_TIMEOUT  = 1000;
   private static final int    ERROR_EXIT_CODE      = -1;
   private static final int    MIN_JAVA_VERSION     = 21;
   private static final String JAVA_VERSION_PATTERN = "version \"?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?";
   private static final String MVN_VERSION_PATTERN  = "Apache Maven (\\d+\\.\\d+\\.\\d+)";
   private static final String GIT_VERSION_PATTERN  = "git version (\\d+\\.\\d+\\.\\d+)";



   /***************************************************************************
    * Check if Java is installed and meets minimum version requirement.
    *
    * @return dependency status for Java
    * @since 0.2.0
    ***************************************************************************/
   public static DependencyStatus checkJava()
   {
      return checkJava(MIN_JAVA_VERSION);
   }



   /***************************************************************************
    * Check if Java is installed and meets specified minimum version.
    *
    * @param minVersion minimum required Java version
    * @return dependency status for Java
    * @since 0.2.0
    ***************************************************************************/
   public static DependencyStatus checkJava(int minVersion)
   {
      try
      {
         ProcessResult result = runCommand("java", "-version");
         if(result.exitCode() != 0)
         {
            return DependencyStatus.notFound("Java", "java", "Install Java " + minVersion + "+ from https://adoptium.net/");
         }

         ///////////////////////////////////////////////////////////////////
         // Java prints version to stderr, so check both streams          //
         ///////////////////////////////////////////////////////////////////
         String output = result.stderr().isEmpty() ? result.stdout() : result.stderr();
         Pattern pattern = Pattern.compile(JAVA_VERSION_PATTERN);
         Matcher matcher = pattern.matcher(output);

         if(matcher.find())
         {
            int majorVersion = Integer.parseInt(matcher.group(1));
            String fullVersion = extractFullVersion(output, "java");
            String path = findExecutablePath("java");

            if(majorVersion >= minVersion)
            {
               return DependencyStatus.ok("Java", fullVersion, path);
            }
            else
            {
               return DependencyStatus.versionMismatch("Java", fullVersion, minVersion + "+", path,
                  "Upgrade to Java " + minVersion + "+ from https://adoptium.net/");
            }
         }

         return DependencyStatus.notFound("Java", "java", "Install Java " + minVersion + "+ from https://adoptium.net/");
      }
      catch(Exception e)
      {
         return DependencyStatus.error("Java", e.getMessage());
      }
   }



   /***************************************************************************
    * Check if Maven is installed.
    *
    * @return dependency status for Maven
    * @since 0.2.0
    ***************************************************************************/
   public static DependencyStatus checkMaven()
   {
      try
      {
         ProcessResult result = runCommand("mvn", "-version");
         if(result.exitCode() != 0)
         {
            return DependencyStatus.notFound("Maven", "mvn", "Install Maven from https://maven.apache.org/download.cgi");
         }

         Pattern pattern = Pattern.compile(MVN_VERSION_PATTERN);
         Matcher matcher = pattern.matcher(result.stdout());

         if(matcher.find())
         {
            String version = matcher.group(1);
            String path = findExecutablePath("mvn");
            return DependencyStatus.ok("Maven", version, path);
         }

         return DependencyStatus.notFound("Maven", "mvn", "Install Maven from https://maven.apache.org/download.cgi");
      }
      catch(Exception e)
      {
         return DependencyStatus.error("Maven", e.getMessage());
      }
   }



   /***************************************************************************
    * Check if Git is installed.
    *
    * @return dependency status for Git
    * @since 0.2.0
    ***************************************************************************/
   public static DependencyStatus checkGit()
   {
      try
      {
         ProcessResult result = runCommand("git", "--version");
         if(result.exitCode() != 0)
         {
            return DependencyStatus.notFound("Git", "git", "Install Git from https://git-scm.com/downloads");
         }

         Pattern pattern = Pattern.compile(GIT_VERSION_PATTERN);
         Matcher matcher = pattern.matcher(result.stdout());

         if(matcher.find())
         {
            String version = matcher.group(1);
            String path = findExecutablePath("git");
            return DependencyStatus.ok("Git", version, path);
         }

         return DependencyStatus.notFound("Git", "git", "Install Git from https://git-scm.com/downloads");
      }
      catch(Exception e)
      {
         return DependencyStatus.error("Git", e.getMessage());
      }
   }



   /***************************************************************************
    * Check if Docker is installed (optional dependency).
    *
    * @return dependency status for Docker
    * @since 0.2.0
    ***************************************************************************/
   public static DependencyStatus checkDocker()
   {
      try
      {
         ProcessResult result = runCommand("docker", "--version");
         if(result.exitCode() != 0)
         {
            return DependencyStatus.optional("Docker", "docker", "Install Docker from https://docs.docker.com/get-docker/");
         }

         ///////////////////////////////////////////////////////////////////
         // Docker version output: "Docker version 24.0.5, build ..."    //
         ///////////////////////////////////////////////////////////////////
         Pattern pattern = Pattern.compile("Docker version (\\d+\\.\\d+\\.\\d+)");
         Matcher matcher = pattern.matcher(result.stdout());

         if(matcher.find())
         {
            String version = matcher.group(1);
            String path = findExecutablePath("docker");
            return DependencyStatus.ok("Docker", version, path);
         }

         return DependencyStatus.optional("Docker", "docker", "Install Docker from https://docs.docker.com/get-docker/");
      }
      catch(Exception e)
      {
         return DependencyStatus.optional("Docker", "docker", "Install Docker from https://docs.docker.com/get-docker/");
      }
   }



   /***************************************************************************
    * Check all dependencies and return a comprehensive report.
    *
    * @return list of dependency statuses
    * @since 0.2.0
    ***************************************************************************/
   public static List<DependencyStatus> checkAll()
   {
      List<DependencyStatus> results = new ArrayList<>();
      results.add(checkJava());
      results.add(checkMaven());
      results.add(checkGit());
      results.add(checkDocker());
      return results;
   }



   /***************************************************************************
    * Require Java to be installed with minimum version. Throws if not met.
    *
    * @param minVersion minimum required Java version
    * @throws DependencyException if Java is not installed or version too low
    * @since 0.2.0
    ***************************************************************************/
   public static void requireJava(int minVersion) throws DependencyException
   {
      DependencyStatus status = checkJava(minVersion);
      if(!status.isOk())
      {
         throw new DependencyException(status);
      }
   }



   /***************************************************************************
    * Require Maven to be installed. Throws if not found.
    *
    * @throws DependencyException if Maven is not installed
    * @since 0.2.0
    ***************************************************************************/
   public static void requireMaven() throws DependencyException
   {
      DependencyStatus status = checkMaven();
      if(!status.isOk())
      {
         throw new DependencyException(status);
      }
   }



   /***************************************************************************
    * Require Git to be installed. Throws if not found.
    *
    * @throws DependencyException if Git is not installed
    * @since 0.2.0
    ***************************************************************************/
   public static void requireGit() throws DependencyException
   {
      DependencyStatus status = checkGit();
      if(!status.isOk())
      {
         throw new DependencyException(status);
      }
   }



   /***************************************************************************
    * Run a command and capture output.
    *
    * @param command command and arguments
    * @return process result with exit code and output
    * @since 0.2.0
    ***************************************************************************/
   private static ProcessResult runCommand(String... command)
   {
      try
      {
         ProcessBuilder pb = new ProcessBuilder(command);
         pb.redirectErrorStream(false);
         Process process = pb.start();

         StringBuilder stdout = new StringBuilder();
         StringBuilder stderr = new StringBuilder();

         Thread stdoutThread = new Thread(() -> readStream(process.getInputStream(), stdout));
         Thread stderrThread = new Thread(() -> readStream(process.getErrorStream(), stderr));
         stdoutThread.start();
         stderrThread.start();

         boolean completed = process.waitFor(DEFAULT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
         if(!completed)
         {
            process.destroyForcibly();
            return new ProcessResult(ERROR_EXIT_CODE, "", "Command timed out");
         }

         stdoutThread.join(THREAD_JOIN_TIMEOUT);
         stderrThread.join(THREAD_JOIN_TIMEOUT);

         return new ProcessResult(process.exitValue(), stdout.toString(), stderr.toString());
      }
      catch(Exception e)
      {
         return new ProcessResult(ERROR_EXIT_CODE, "", e.getMessage());
      }
   }



   /***************************************************************************
    * Read an input stream into a StringBuilder.
    *
    * @param inputStream stream to read
    * @param output builder to append to
    * @since 0.2.0
    ***************************************************************************/
   private static void readStream(java.io.InputStream inputStream, StringBuilder output)
   {
      try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream)))
      {
         String line;
         while((line = reader.readLine()) != null)
         {
            output.append(line).append("\n");
         }
      }
      catch(Exception e)
      {
         // Ignore read errors
      }
   }



   /***************************************************************************
    * Find the path to an executable using 'which' (Unix) or 'where' (Windows).
    *
    * @param executable name of executable
    * @return path or "unknown"
    * @since 0.2.0
    ***************************************************************************/
   private static String findExecutablePath(String executable)
   {
      try
      {
         String os = System.getProperty("os.name").toLowerCase();
         String[] command = os.contains("win")
            ? new String[] { "where", executable }
            : new String[] { "which", executable };

         ProcessResult result = runCommand(command);
         if(result.exitCode() == 0 && !result.stdout().isEmpty())
         {
            return result.stdout().trim().split("\n")[0];
         }
      }
      catch(Exception e)
      {
         // Ignore
      }
      return "unknown";
   }



   /***************************************************************************
    * Extract full version string from command output.
    *
    * @param output command output
    * @param tool tool name for context
    * @return version string or "unknown"
    * @since 0.2.0
    ***************************************************************************/
   private static String extractFullVersion(String output, String tool)
   {
      if("java".equals(tool))
      {
         ////////////////////////////////////////////////////////////////
         // Try to extract version like "21.0.5" or "21"               //
         ////////////////////////////////////////////////////////////////
         Pattern pattern = Pattern.compile("version \"?(\\d+(?:\\.\\d+)*(?:[._][\\w-]+)?)");
         Matcher matcher = pattern.matcher(output);
         if(matcher.find())
         {
            String version = matcher.group(1);
            ////////////////////////////////////////////////////////////
            // Check for GraalVM or other JVM vendors                  //
            ////////////////////////////////////////////////////////////
            if(output.contains("GraalVM"))
            {
               return version + " (GraalVM)";
            }
            else if(output.contains("Temurin"))
            {
               return version + " (Temurin)";
            }
            else if(output.contains("OpenJDK"))
            {
               return version + " (OpenJDK)";
            }
            return version;
         }
      }
      return "unknown";
   }



   /***************************************************************************
    * Result of running an external process.
    *
    * @param exitCode process exit code
    * @param stdout standard output
    * @param stderr standard error
    * @since 0.2.0
    ***************************************************************************/
   private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
