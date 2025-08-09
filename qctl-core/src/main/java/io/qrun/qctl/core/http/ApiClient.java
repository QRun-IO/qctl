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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qrun.qctl.shared.api.ProblemDetail;


/**
 * Minimal HTTP client wrapper with JSON serialization, retries, and RFC7807 mapping.
 *
 * Why: Centralizes API calls with consistent error→exit code mapping for the CLI.
 * @since 0.1.0
 */
public class ApiClient
{
   // ---------------------------------------------------------------------
   // Retry/backoff tuning
   // ---------------------------------------------------------------------
   private static final long   RETRY_BASE_MS        = 250L;
   private static final long   RETRY_MAX_SLEEP_MS   = 5000L;
   private static final double BACKOFF_FACTOR       = 1.7D;
   private static final double JITTER_MAX_MS        = 100D;
   private static final int    MAX_ATTEMPTS         = 3;

   // ---------------------------------------------------------------------
   // HTTP status constants (use JDK where available, define missing ones)
   // ---------------------------------------------------------------------
   private static final int HTTP_OK_MIN               = 200;
   private static final int HTTP_REDIRECT_MIN         = 300;

   private static final int HTTP_BAD_REQUEST          = HttpURLConnection.HTTP_BAD_REQUEST;        // 400
   private static final int HTTP_UNAUTHORIZED         = HttpURLConnection.HTTP_UNAUTHORIZED;       // 401
   private static final int HTTP_FORBIDDEN            = HttpURLConnection.HTTP_FORBIDDEN;          // 403
   private static final int HTTP_NOT_FOUND            = HttpURLConnection.HTTP_NOT_FOUND;          // 404
   private static final int HTTP_CONFLICT             = HttpURLConnection.HTTP_CONFLICT;           // 409
   private static final int HTTP_PRECONDITION_FAILED  = HttpURLConnection.HTTP_PRECON_FAILED;      // 412
   private static final int HTTP_REQUEST_TIMEOUT      = HttpURLConnection.HTTP_CLIENT_TIMEOUT;     // 408
   private static final int HTTP_INTERNAL_ERROR       = HttpURLConnection.HTTP_INTERNAL_ERROR;     // 500

   private static final int HTTP_UNPROCESSABLE_ENTITY = 422; // 422 not defined in HttpURLConnection
   private static final int HTTP_TOO_MANY_REQUESTS    = 429; // 429 not defined in HttpURLConnection

   private final HttpClient     client;
   private final ObjectMapper   mapper = new ObjectMapper();
   private final Duration       requestTimeout;
   private final HeaderProvider headerProvider;



   /**
    * Creates a client with the given request timeout and a no-op header provider.
    *
    * @param requestTimeout per-request timeout
    */
   public ApiClient(Duration requestTimeout)
   {
      this(requestTimeout, builder ->
         {
         }
      );
   }



   /**
    * Creates a client with timeout and a header provider hook.
    *
    * @param requestTimeout per-request timeout
    * @param headerProvider callback to inject headers prior to sending
    */
   public ApiClient(Duration requestTimeout, HeaderProvider headerProvider)
   {
      this(HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build(), requestTimeout, headerProvider);
   }

   // Visible for tests



   /**
    * Visible for tests.
    *
    * @param client          underlying HTTP client
    * @param requestTimeout  per-request timeout
    * @param headerProvider  callback to inject headers prior to sending
    */
   ApiClient(HttpClient client, Duration requestTimeout, HeaderProvider headerProvider)
   {
      this.client = client;
      this.requestTimeout = requestTimeout;
      this.headerProvider = headerProvider;
   }



   /**
    * Maps HTTP status to CLI exit code per DESIGN-2.
    *
    * @param sc HTTP status code
    * @return process exit code to use for the CLI
    */
   public static int exitCodeForStatus(int sc)
   {
      if(sc == HTTP_UNAUTHORIZED || sc == HTTP_FORBIDDEN)
      {
         return 4;
      }
      if(sc == HTTP_NOT_FOUND)
      {
         return 5;
      }
      if(sc == HTTP_BAD_REQUEST || sc == HTTP_UNPROCESSABLE_ENTITY)
      {
         return 6;
      }
      if(sc == HTTP_CONFLICT || sc == HTTP_PRECONDITION_FAILED)
      {
         return 8;
      }
      if(sc == HTTP_REQUEST_TIMEOUT || sc == HTTP_TOO_MANY_REQUESTS || sc >= HTTP_INTERNAL_ERROR)
      {
         return 3;
      }
      return 1;
   }



   /**
    * Performs a GET and parses a JSON response into the given type.
    *
    * @param uri  request URI
    * @param type target response type
    * @param <T>  type of the response
    * @return parsed response
    * @throws IOException          on IO errors
    * @throws InterruptedException if the thread is interrupted
    * @throws ApiException         when a non-2xx response is received
    */
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



   /**
    * Performs a POST with JSON body and parses a JSON response.
    *
    * @param uri  request URI
    * @param body payload object to serialize as JSON
    * @param type target response type
    * @param <T>  type of the response
    * @return parsed response
    * @throws IOException          on IO errors
    * @throws InterruptedException if the thread is interrupted
    * @throws ApiException         when a non-2xx response is received
    */
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



   /***************************************************************************
    ** Sends a request with retry/backoff and maps errors to ApiException.
    ***************************************************************************/
   public <T> T send(HttpRequest req, Class<T> type)
         throws IOException, InterruptedException, ApiException
   {
      int  attempts = 0;
      long backoff  = RETRY_BASE_MS;
      while(true)
      {
         attempts++;
         HttpResponse<byte[]> resp = sendOnce(req);
         int                  sc   = resp.statusCode();

         if(sc >= HTTP_OK_MIN && sc < HTTP_REDIRECT_MIN)
         {
            if(type == Void.class)
            {
               return null;
            }

            return mapper.readValue(resp.body(), type);
         }
         if(sc == HTTP_UNAUTHORIZED || sc == HTTP_FORBIDDEN)
         {
            throw toApiException(resp, 4);
         }
         if(sc == HTTP_NOT_FOUND)
         {
            throw toApiException(resp, 5);
         }
         if(sc == HTTP_BAD_REQUEST || sc == HTTP_UNPROCESSABLE_ENTITY)
         {
            throw toApiException(resp, 6);
         }
         if(sc == HTTP_CONFLICT || sc == HTTP_PRECONDITION_FAILED)
         {
            throw toApiException(resp, 8);
         }
         if(sc == HTTP_REQUEST_TIMEOUT || sc == HTTP_TOO_MANY_REQUESTS || sc >= HTTP_INTERNAL_ERROR)
         {
            if(attempts >= MAX_ATTEMPTS)
            {
               throw toApiException(resp, 3);
            }
            try
            {
               Thread.sleep(Math.min(backoff, RETRY_MAX_SLEEP_MS));
            }
            catch(InterruptedException ie)
            {
               Thread.currentThread().interrupt();
               throw ie;
            }
            backoff = (long) (backoff * BACKOFF_FACTOR + (Math.random() * JITTER_MAX_MS));
            continue;
         }
         throw toApiException(resp, 1);
      }
   }

   // For testability: single HTTP round-trip; override in tests



   /**
    * Single HTTP round-trip (overridable in tests).
    *
    * @param req request to execute
    * @return HTTP response as bytes
    * @throws IOException          on IO errors
    * @throws InterruptedException if interrupted while waiting for response
    */
   protected HttpResponse<byte[]> sendOnce(HttpRequest req)
         throws IOException, InterruptedException
   {
      return client.send(req, HttpResponse.BodyHandlers.ofByteArray());
   }



   /***************************************************************************
    ** Converts an HTTP response into an ApiException using RFC7807 body when available.
    ***************************************************************************/
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
         pd.detail = Optional.ofNullable(resp.headers().firstValue("X-Error").orElse(null)).orElse("Unexpected error");
         return new ApiException(pd, exitCode);
      }
   }



   /**
    * Supplies additional headers to an {@link HttpRequest.Builder} before sending.
    */
   @FunctionalInterface
   public interface HeaderProvider
   {
      /**
       * Apply headers to the provided builder.
       *
       * @param builder request builder to mutate
       */
      void apply(HttpRequest.Builder builder);
   }



   /**
    * Exception representing an API error with an associated CLI exit code.
    */
   public static class ApiException extends Exception
   {
      public final ProblemDetail problem;
      public final int           exitCode;



      /**
       * Create a new ApiException.
       *
       * @param p         parsed RFC7807 problem detail (may be null)
       * @param exitCode  mapped CLI exit code
       */
      public ApiException(ProblemDetail p, int exitCode)
      {
         super(p != null ? p.title + ": " + p.detail : "API error");
         this.problem = p;
         this.exitCode = exitCode;
      }
   }
}
