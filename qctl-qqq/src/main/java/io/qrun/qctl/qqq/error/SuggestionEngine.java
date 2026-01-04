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

package io.qrun.qctl.qqq.error;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/*******************************************************************************
 * Provides fuzzy matching suggestions for typos and similar names.
 *
 * Uses Levenshtein distance and prefix matching to suggest alternatives.
 *
 * @since 0.2.0
 *******************************************************************************/
public class SuggestionEngine
{
   private static final int MAX_SUGGESTIONS = 3;
   private static final int MAX_DISTANCE = 3;



   /***************************************************************************
    * Find similar strings from a list of candidates.
    *
    * @param input the user's input
    * @param candidates list of valid options
    * @return list of similar options, sorted by relevance
    * @since 0.2.0
    ***************************************************************************/
   public List<String> findSimilar(String input, List<String> candidates)
   {
      if(input == null || input.isEmpty() || candidates == null || candidates.isEmpty())
      {
         return List.of();
      }

      String lowerInput = input.toLowerCase();
      List<ScoredCandidate> scored = new ArrayList<>();

      for(String candidate : candidates)
      {
         int score = calculateScore(lowerInput, candidate.toLowerCase());
         if(score <= MAX_DISTANCE)
         {
            scored.add(new ScoredCandidate(candidate, score));
         }
      }

      scored.sort(Comparator.comparingInt(ScoredCandidate::score));

      return scored.stream()
         .limit(MAX_SUGGESTIONS)
         .map(ScoredCandidate::value)
         .toList();
   }



   /***************************************************************************
    * Calculate similarity score (lower is better).
    *
    * Combines Levenshtein distance with prefix bonus.
    *
    * @param input user input (lowercase)
    * @param candidate candidate value (lowercase)
    * @return similarity score
    * @since 0.2.0
    ***************************************************************************/
   private int calculateScore(String input, String candidate)
   {
      int distance = levenshteinDistance(input, candidate);

      // Bonus for prefix match (reduce distance)
      if(candidate.startsWith(input) || input.startsWith(candidate))
      {
         distance = Math.max(0, distance - 1);
      }

      // Bonus for containing the input
      if(candidate.contains(input))
      {
         distance = Math.max(0, distance - 1);
      }

      return distance;
   }



   /***************************************************************************
    * Calculate Levenshtein distance between two strings.
    *
    * @param s1 first string
    * @param s2 second string
    * @return edit distance
    * @since 0.2.0
    ***************************************************************************/
   private int levenshteinDistance(String s1, String s2)
   {
      int[][] dp = new int[s1.length() + 1][s2.length() + 1];

      for(int i = 0; i <= s1.length(); i++)
      {
         dp[i][0] = i;
      }
      for(int j = 0; j <= s2.length(); j++)
      {
         dp[0][j] = j;
      }

      for(int i = 1; i <= s1.length(); i++)
      {
         for(int j = 1; j <= s2.length(); j++)
         {
            int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
            dp[i][j] = Math.min(
               Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
               dp[i - 1][j - 1] + cost
            );
         }
      }

      return dp[s1.length()][s2.length()];
   }



   /***************************************************************************
    * Scored candidate for sorting.
    ***************************************************************************/
   private record ScoredCandidate(String value, int score)
   {
   }
}
