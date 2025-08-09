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
package io.qrun.qctl.core.http;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qrun.qctl.shared.api.ProblemDetail;


public class ApiClient
{
   private final HttpClient     client;
   private final ObjectMapper   mapper = new ObjectMapper();
   private final Duration       requestTimeout;
   private final HeaderProvider headerProvider;



   public ApiClient(Duration requestTimeout)
   {
      this(requestTimeout, builder -> {
      });
   }



   public ApiClient(Duration requestTimeout, HeaderProvider headerProvider)
   {
      this(
         HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build(),
         requestTimeout,
         headerProvider);
   }



   // Visible for tests
   ApiClient(HttpClient client, Duration requestTimeout, HeaderProvider headerProvider)
   {
      this.client = client;
      this.requestTimeout = requestTimeout;
      this.headerProvider = headerProvider;
   }



   public static int exitCodeForStatus(int sc)
   {
      if(sc == 401 || sc == 403)
         return 4;
      if(sc == 404)
         return 5;
      if(sc == 400 || sc == 422)
         return 6;
      if(sc == 409 || sc == 412)
         return 8;
      if(sc == 408 || sc == 429 || sc >= 500)
         return 3;
      return 1;
   }



   public <T> T getJson(URI uri, Class<T> type)
      throws IOException, InterruptedException, ApiException
   {
      HttpRequest.Builder b =
         HttpRequest.newBuilder(uri)
            .timeout(requestTimeout)
            .header("Accept", "application/json")
            .GET();
      headerProvider.apply(b);
      HttpRequest req = b.build();
      return send(req, type);
   }



   public <T> T postJson(URI uri, Object body, Class<T> type)
      throws IOException, InterruptedException, ApiException
   {
      byte[] payload = mapper.writeValueAsBytes(body);
      HttpRequest.Builder b =
         HttpRequest.newBuilder(uri)
            .timeout(requestTimeout)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(payload));
      headerProvider.apply(b);
      HttpRequest req = b.build();
      return send(req, type);
   }



   public <T> T send(HttpRequest req, Class<T> type)
      throws IOException, InterruptedException, ApiException
   {
      int  attempts = 0;
      long backoff  = 250;
      while(true)
      {
         attempts++;
         HttpResponse<byte[]> resp = sendOnce(req);
         int                  sc   = resp.statusCode();
         if(sc >= 200 && sc < 300)
         {
            if(type == Void.class)
               return null;
            return mapper.readValue(resp.body(), type);
         }
         if(sc == 401 || sc == 403)
            throw toApiException(resp, 4);
         if(sc == 404)
            throw toApiException(resp, 5);
         if(sc == 400 || sc == 422)
            throw toApiException(resp, 6);
         if(sc == 409 || sc == 412)
            throw toApiException(resp, 8);
         if(sc == 408 || sc == 429 || sc >= 500)
         {
            if(attempts >= 3)
               throw toApiException(resp, 3);
            try
            {
               Thread.sleep(Math.min(backoff, 5000));
            }
            catch(InterruptedException ie)
            {
               Thread.currentThread().interrupt();
               throw ie;
            }
            backoff = (long) (backoff * 1.7 + (Math.random() * 100));
            continue;
         }
         throw toApiException(resp, 1);
      }
   }



   // For testability: single HTTP round-trip; override in tests
   protected HttpResponse<byte[]> sendOnce(HttpRequest req)
      throws IOException, InterruptedException
   {
      return client.send(req, HttpResponse.BodyHandlers.ofByteArray());
   }



   private ApiException toApiException(HttpResponse<byte[]> resp, int exitCode)
   {
      try
      {
         ProblemDetail pd = mapper.readValue(resp.body(), ProblemDetail.class);
         return new ApiException(pd, exitCode);
      }
      catch(Exception e)
      {
         ProblemDetail pd = new ProblemDetail();
         pd.status = resp.statusCode();
         pd.title = "HTTP " + resp.statusCode();
         pd.detail =
            Optional.ofNullable(resp.headers().firstValue("X-Error").orElse(null))
               .orElse("Unexpected error");
         return new ApiException(pd, exitCode);
      }
   }



   @FunctionalInterface
   public interface HeaderProvider
   {
      void apply(HttpRequest.Builder builder);
   }



   public static class ApiException extends Exception
   {
      public final ProblemDetail problem;
      public final int           exitCode;



      public ApiException(ProblemDetail p, int exitCode)
      {
         super(p != null ? p.title + ": " + p.detail : "API error");
         this.problem = p;
         this.exitCode = exitCode;
      }
   }
}
