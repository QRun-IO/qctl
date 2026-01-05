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


import java.util.ArrayList;
import java.util.List;


/*******************************************************************************
 * Generates unified diff output for file comparison.
 *
 * @since 0.2.0
 *******************************************************************************/
public class FileDiffer
{
   private static final String ANSI_RED = "\u001B[31m";
   private static final String ANSI_GREEN = "\u001B[32m";
   private static final String ANSI_CYAN = "\u001B[36m";
   private static final String ANSI_RESET = "\u001B[0m";

   private static final int CONTEXT_LINES = 3;



   /***************************************************************************
    * Generate a unified diff between two strings.
    *
    * @param existing the existing file content
    * @param updated the new file content
    * @param filename the filename for the header
    * @return formatted diff output
    * @since 0.2.0
    ***************************************************************************/
   public String diff(String existing, String updated, String filename)
   {
      String[] oldLines = existing.split("\n", -1);
      String[] newLines = updated.split("\n", -1);

      List<DiffLine> diffLines = computeDiff(oldLines, newLines);

      return formatDiff(diffLines, filename, oldLines.length, newLines.length);
   }



   /***************************************************************************
    * Compute the diff between two arrays of lines.
    *
    * @param oldLines original lines
    * @param newLines new lines
    * @return list of diff lines
    * @since 0.2.0
    ***************************************************************************/
   private List<DiffLine> computeDiff(String[] oldLines, String[] newLines)
   {
      List<DiffLine> result = new ArrayList<>();

      /////////////////////////////////////////////////////////////////////////
      // Simple line-by-line diff using longest common subsequence           //
      /////////////////////////////////////////////////////////////////////////
      int[][] lcs = computeLCS(oldLines, newLines);
      int i = oldLines.length;
      int j = newLines.length;

      List<DiffLine> tempResult = new ArrayList<>();

      while(i > 0 || j > 0)
      {
         if(i > 0 && j > 0 && oldLines[i - 1].equals(newLines[j - 1]))
         {
            tempResult.add(new DiffLine(DiffType.CONTEXT, oldLines[i - 1], i, j));
            i--;
            j--;
         }
         else if(j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j]))
         {
            tempResult.add(new DiffLine(DiffType.ADD, newLines[j - 1], -1, j));
            j--;
         }
         else
         {
            tempResult.add(new DiffLine(DiffType.DELETE, oldLines[i - 1], i, -1));
            i--;
         }
      }

      /////////////////////////////////////////////////////////////////////////
      // Reverse to get correct order                                        //
      /////////////////////////////////////////////////////////////////////////
      for(int k = tempResult.size() - 1; k >= 0; k--)
      {
         result.add(tempResult.get(k));
      }

      return result;
   }



   /***************************************************************************
    * Compute the longest common subsequence matrix.
    *
    * @param oldLines original lines
    * @param newLines new lines
    * @return LCS matrix
    * @since 0.2.0
    ***************************************************************************/
   private int[][] computeLCS(String[] oldLines, String[] newLines)
   {
      int m = oldLines.length;
      int n = newLines.length;
      int[][] lcs = new int[m + 1][n + 1];

      for(int i = 1; i <= m; i++)
      {
         for(int j = 1; j <= n; j++)
         {
            if(oldLines[i - 1].equals(newLines[j - 1]))
            {
               lcs[i][j] = lcs[i - 1][j - 1] + 1;
            }
            else
            {
               lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
            }
         }
      }

      return lcs;
   }



   /***************************************************************************
    * Format the diff lines into unified diff output.
    *
    * @param diffLines the diff lines
    * @param filename the filename
    * @param oldCount original line count
    * @param newCount new line count
    * @return formatted diff string
    * @since 0.2.0
    ***************************************************************************/
   private String formatDiff(List<DiffLine> diffLines, String filename,
                             int oldCount, int newCount)
   {
      StringBuilder sb = new StringBuilder();

      /////////////////////////////////////////////////////////////////////////
      // Header                                                              //
      /////////////////////////////////////////////////////////////////////////
      sb.append(ANSI_RED).append("--- existing ").append(filename)
        .append(ANSI_RESET).append("\n");
      sb.append(ANSI_GREEN).append("+++ template ").append(filename)
        .append(ANSI_RESET).append("\n");

      /////////////////////////////////////////////////////////////////////////
      // Find hunks (groups of changes with context)                         //
      /////////////////////////////////////////////////////////////////////////
      List<Hunk> hunks = findHunks(diffLines);

      for(Hunk hunk : hunks)
      {
         sb.append(ANSI_CYAN)
           .append("@@ -").append(hunk.oldStart).append(",").append(hunk.oldCount)
           .append(" +").append(hunk.newStart).append(",").append(hunk.newCount)
           .append(" @@").append(ANSI_RESET).append("\n");

         for(DiffLine line : hunk.lines)
         {
            switch(line.type)
            {
               case ADD:
                  sb.append(ANSI_GREEN).append("+").append(line.content)
                    .append(ANSI_RESET).append("\n");
                  break;
               case DELETE:
                  sb.append(ANSI_RED).append("-").append(line.content)
                    .append(ANSI_RESET).append("\n");
                  break;
               case CONTEXT:
                  sb.append(" ").append(line.content).append("\n");
                  break;
               default:
                  break;
            }
         }
      }

      if(hunks.isEmpty())
      {
         sb.append("(no differences)\n");
      }

      return sb.toString();
   }



   /***************************************************************************
    * Find hunks (groups of changes with surrounding context).
    *
    * @param diffLines all diff lines
    * @return list of hunks
    * @since 0.2.0
    ***************************************************************************/
   private List<Hunk> findHunks(List<DiffLine> diffLines)
   {
      List<Hunk> hunks = new ArrayList<>();
      List<Integer> changeIndices = new ArrayList<>();

      /////////////////////////////////////////////////////////////////////////
      // Find all change indices                                             //
      /////////////////////////////////////////////////////////////////////////
      for(int i = 0; i < diffLines.size(); i++)
      {
         if(diffLines.get(i).type != DiffType.CONTEXT)
         {
            changeIndices.add(i);
         }
      }

      if(changeIndices.isEmpty())
      {
         return hunks;
      }

      /////////////////////////////////////////////////////////////////////////
      // Group changes into hunks                                            //
      /////////////////////////////////////////////////////////////////////////
      int hunkStart = Math.max(0, changeIndices.get(0) - CONTEXT_LINES);
      int hunkEnd = Math.min(diffLines.size() - 1, changeIndices.get(0) + CONTEXT_LINES);

      for(int i = 1; i < changeIndices.size(); i++)
      {
         int changeIdx = changeIndices.get(i);
         int changeStart = changeIdx - CONTEXT_LINES;

         if(changeStart <= hunkEnd + 1)
         {
            hunkEnd = Math.min(diffLines.size() - 1, changeIdx + CONTEXT_LINES);
         }
         else
         {
            hunks.add(createHunk(diffLines, hunkStart, hunkEnd));
            hunkStart = Math.max(0, changeIdx - CONTEXT_LINES);
            hunkEnd = Math.min(diffLines.size() - 1, changeIdx + CONTEXT_LINES);
         }
      }

      hunks.add(createHunk(diffLines, hunkStart, hunkEnd));

      return hunks;
   }



   /***************************************************************************
    * Create a hunk from a range of diff lines.
    *
    * @param diffLines all diff lines
    * @param start start index
    * @param end end index
    * @return the hunk
    * @since 0.2.0
    ***************************************************************************/
   private Hunk createHunk(List<DiffLine> diffLines, int start, int end)
   {
      List<DiffLine> hunkLines = new ArrayList<>();
      int oldStart = 1;
      int oldCount = 0;
      int newStart = 1;
      int newCount = 0;

      boolean foundOldStart = false;
      boolean foundNewStart = false;

      for(int i = start; i <= end && i < diffLines.size(); i++)
      {
         DiffLine line = diffLines.get(i);
         hunkLines.add(line);

         switch(line.type)
         {
            case CONTEXT:
               if(!foundOldStart && line.oldLine > 0)
               {
                  oldStart = line.oldLine;
                  foundOldStart = true;
               }
               if(!foundNewStart && line.newLine > 0)
               {
                  newStart = line.newLine;
                  foundNewStart = true;
               }
               oldCount++;
               newCount++;
               break;
            case DELETE:
               if(!foundOldStart && line.oldLine > 0)
               {
                  oldStart = line.oldLine;
                  foundOldStart = true;
               }
               oldCount++;
               break;
            case ADD:
               if(!foundNewStart && line.newLine > 0)
               {
                  newStart = line.newLine;
                  foundNewStart = true;
               }
               newCount++;
               break;
            default:
               break;
         }
      }

      return new Hunk(hunkLines, oldStart, oldCount, newStart, newCount);
   }



   /***************************************************************************
    * Types of diff lines.
    ***************************************************************************/
   private enum DiffType
   {
      CONTEXT,
      ADD,
      DELETE
   }



   /***************************************************************************
    * A single line in the diff.
    ***************************************************************************/
   private record DiffLine(DiffType type, String content, int oldLine, int newLine)
   {
   }



   /***************************************************************************
    * A hunk (group of changes with context).
    ***************************************************************************/
   private record Hunk(List<DiffLine> lines, int oldStart, int oldCount,
                       int newStart, int newCount)
   {
   }
}
