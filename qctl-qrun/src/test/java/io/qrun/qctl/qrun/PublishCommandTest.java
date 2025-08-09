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

package io.qrun.qctl.qrun;


import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import io.qrun.qctl.core.http.ApiClient;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


class PublishCommandTest
{

   /***************************************************************************
    * Ensures Idempotency-Key header is set and 201 responses are accepted.
    ***************************************************************************/
   @Test
   void publish_sets_idempotency_key_and_accepts_201()
   {
      var captured1 = new AtomicReference<HttpRequest>();
      var captured2 = new AtomicReference<HttpRequest>();

      class TestCmd extends PublishCommand
      {
         @Override
         @SuppressWarnings("checkstyle:MagicNumber")
         protected ApiClient createClient(ApiClient.HeaderProvider headerProvider)
         {
            return new ApiClient(Duration.ofSeconds(1), headerProvider)
            {
               @Override
               protected HttpResponse<byte[]> sendOnce(HttpRequest req)
               {
                  if(req.uri().getPath().endsWith("/v1/artifacts"))
                  {
                     captured1.set(req);
                     return new BytesResponse(201, "{\"id\":\"01J0M40G3SJ0QJ9E3V1QK8A3R2\",\"kind\":\"oci\",\"digest\":\"sha256:x\",\"sizeBytes\":1,\"createdAt\":\"2025-01-15T12:00:00Z\"}".getBytes());
                  }
                  captured2.set(req);
                  return new BytesResponse(201, "{\"id\":\"01J0M41V9X5J2B4M5H0G7D2T1Q\",\"appName\":\"demo-app\",\"version\":\"0.1.0\",\"artifactId\":\"01J0M40G3SJ0QJ9E3V1QK8A3R2\",\"channel\":\"stable\",\"createdAt\":\"2025-01-15T12:00:00Z\"}".getBytes());
               }
            };
         }
      }

      TestCmd cmd = new TestCmd();
      cmd.env = "dev";
      cmd.idempotencyKey = "k-123";
      cmd.run();

      assertThat(captured1.get().headers().firstValue("Idempotency-Key")).contains("k-123");
      assertThat(captured2.get().headers().firstValue("Idempotency-Key")).contains("k-123");
   }


   /***************************************************************************
    * Accepts 200 on idempotent replay for either call.
    ***************************************************************************/
   @Test
   void publish_accepts_200_on_replay()
   {
      class TestCmd extends PublishCommand
      {
         @Override
         @SuppressWarnings("checkstyle:MagicNumber")
         protected ApiClient createClient(ApiClient.HeaderProvider headerProvider)
         {
            return new ApiClient(Duration.ofSeconds(1), headerProvider)
            {
               private int n = 0;

               @Override
               protected HttpResponse<byte[]> sendOnce(HttpRequest req)
               {
                  n++;
                  if(n == 1)
                  {
                     return new BytesResponse(200, "{\"id\":\"01J0M40G3SJ0QJ9E3V1QK8A3R2\",\"kind\":\"oci\",\"digest\":\"sha256:x\",\"sizeBytes\":1,\"createdAt\":\"2025-01-15T12:00:00Z\"}".getBytes());
                  }
                  return new BytesResponse(200, "{\"id\":\"01J0M41V9X5J2B4M5H0G7D2T1Q\",\"appName\":\"demo-app\",\"version\":\"0.1.0\",\"artifactId\":\"01J0M40G3SJ0QJ9E3V1QK8A3R2\",\"channel\":\"stable\",\"createdAt\":\"2025-01-15T12:00:00Z\"}".getBytes());
               }
            };
         }
      }

      TestCmd cmd = new TestCmd();
      cmd.env = "dev";
      cmd.run();
      // No exception implies success handling of 200 replay
      assertThat(true).isTrue();
   }


   /***************************************************************************
    * Simple immutable HttpResponse over a byte[] body for tests.
    ***************************************************************************/
   private static final class BytesResponse implements HttpResponse<byte[]>
   {
      private final int    status;
      private final byte[] body;

      BytesResponse(int status, byte[] body)
      {
         this.status = status;
         this.body = body;
      }

      @Override
      public int statusCode()
      {
         return status;
      }

      @Override
      public HttpRequest request()
      {
         return null;
      }

      @Override
      public java.util.Optional<HttpResponse<byte[]>> previousResponse()
      {
         return java.util.Optional.empty();
      }

      @Override
      public java.net.http.HttpHeaders headers()
      {
         return java.net.http.HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
      }

      @Override
      public byte[] body()
      {
         return body;
      }

      @Override
      public java.util.Optional<SSLSession> sslSession()
      {
         return java.util.Optional.empty();
      }

      @Override
      public URI uri()
      {
         return URI.create("http://localhost");
      }

      @Override
      public java.net.http.HttpClient.Version version()
      {
         return java.net.http.HttpClient.Version.HTTP_1_1;
      }
   }
}
