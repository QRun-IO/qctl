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


import javax.net.ssl.SSLSession;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import io.qrun.qctl.shared.api.ProblemDetail;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * Tests for {@link ApiClient} header injection and error mapping.
 */
class ApiClientTest
{
   // HTTP status codes - using JDK constants where available
   private static final int HTTP_OK                   = HttpURLConnection.HTTP_OK;                    // 200
   private static final int HTTP_BAD_REQUEST          = HttpURLConnection.HTTP_BAD_REQUEST;           // 400
   private static final int HTTP_UNAUTHORIZED         = HttpURLConnection.HTTP_UNAUTHORIZED;          // 401
   private static final int HTTP_FORBIDDEN            = HttpURLConnection.HTTP_FORBIDDEN;             // 403
   private static final int HTTP_NOT_FOUND            = HttpURLConnection.HTTP_NOT_FOUND;             // 404
   private static final int HTTP_CONFLICT             = HttpURLConnection.HTTP_CONFLICT;              // 409
   private static final int HTTP_PRECONDITION_FAILED  = HttpURLConnection.HTTP_PRECON_FAILED;         // 412
   private static final int HTTP_UNPROCESSABLE_ENTITY = 422; // Not defined in HttpURLConnection
   private static final int HTTP_TOO_MANY_REQUESTS    = 429; // Not defined in HttpURLConnection
   private static final int HTTP_SERVICE_UNAVAILABLE  = HttpURLConnection.HTTP_UNAVAILABLE;           // 503

   // Test timeout constant
   private static final Duration TEST_TIMEOUT = Duration.ofSeconds(1);



   /** Builds a JSON ProblemDetail string for the given HTTP status. */
   private static String problem(int status) throws Exception
   {
      ProblemDetail pd = new ProblemDetail();
      pd.status = status;
      pd.title = "Status " + status;
      pd.detail = "test";
      com.fasterxml.jackson.databind.ObjectMapper m =
         new com.fasterxml.jackson.databind.ObjectMapper();
      return m.writeValueAsString(pd);
   }



   /** Ensures custom header provider is invoked and header is present. */
   @Test
   void header_provider_is_invoked() throws Exception
   {
      CapturingClient client = new CapturingClient(HTTP_OK, "{}");
      ApiClient api =
         new ApiClient(TEST_TIMEOUT, b -> b.header("X-Custom", "abc"))
         {
            @Override
            protected HttpResponse<byte[]> sendOnce(HttpRequest req)
            {
               client.captured = req;
               return client.response;
            }
         };
      api.getJson(URI.create("http://localhost/test"), java.util.Map.class);
      assertThat(client.captured.headers().firstValue("X-Custom")).contains("abc");
   }



   /** Verifies HTTP status codes map to expected exit codes. */
   @Test
   void error_mapping_status_codes() throws Exception
   {
      assertExitForStatus(HTTP_UNAUTHORIZED, ApiClient.exitCodeForStatus(HTTP_UNAUTHORIZED));
      assertExitForStatus(HTTP_FORBIDDEN, ApiClient.exitCodeForStatus(HTTP_FORBIDDEN));
      assertExitForStatus(HTTP_NOT_FOUND, ApiClient.exitCodeForStatus(HTTP_NOT_FOUND));
      assertExitForStatus(HTTP_BAD_REQUEST, ApiClient.exitCodeForStatus(HTTP_BAD_REQUEST));
      assertExitForStatus(HTTP_UNPROCESSABLE_ENTITY, ApiClient.exitCodeForStatus(HTTP_UNPROCESSABLE_ENTITY));
      assertExitForStatus(HTTP_CONFLICT, ApiClient.exitCodeForStatus(HTTP_CONFLICT));
      assertExitForStatus(HTTP_PRECONDITION_FAILED, ApiClient.exitCodeForStatus(HTTP_PRECONDITION_FAILED));
      assertExitForStatus(HTTP_TOO_MANY_REQUESTS, ApiClient.exitCodeForStatus(HTTP_TOO_MANY_REQUESTS));
      assertExitForStatus(HTTP_SERVICE_UNAVAILABLE, ApiClient.exitCodeForStatus(HTTP_SERVICE_UNAVAILABLE));
   }



   /** Helper asserting exit code mapping for a status. */
   private void assertExitForStatus(int status, int expectedExit) throws Exception
   {
      byte[]               body = problem(status).getBytes();
      HttpResponse<byte[]> resp = new SimpleResponse(status, body);
      ApiClient client =
         new ApiClient(TEST_TIMEOUT, b -> {})
         {
            @Override
            protected HttpResponse<byte[]> sendOnce(HttpRequest req)
            {
               return resp;
            }
         };
      assertThatThrownBy(() -> client.getJson(URI.create("http://localhost/x"), java.util.Map.class))
         .isInstanceOf(ApiClient.ApiException.class)
         .satisfies(
            ex -> assertThat(((ApiClient.ApiException) ex).exitCode).isEqualTo(expectedExit));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static final class SimpleResponse implements HttpResponse<byte[]>
   {
      private final int    status;
      private final byte[] body;



      /***************************************************************************
       **
       ***************************************************************************/
      SimpleResponse(int status, byte[] body)
      {
         this.status = status;
         this.body = body;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public int statusCode()
      {
         return status;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public HttpRequest request()
      {
         return null;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Optional<HttpResponse<byte[]>> previousResponse()
      {
         return Optional.empty();
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public java.net.http.HttpHeaders headers()
      {
         return java.net.http.HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public byte[] body()
      {
         return body;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Optional<SSLSession> sslSession()
      {
         return Optional.empty();
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public URI uri()
      {
         return URI.create("http://localhost");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public java.net.http.HttpClient.Version version()
      {
         return java.net.http.HttpClient.Version.HTTP_1_1;
      }
   }



   private static final class CapturingClient
   {
      HttpRequest          captured;
      HttpResponse<byte[]> response;



      /***************************************************************************
       **
       ***************************************************************************/
      CapturingClient(int status, String body)
      {
         this.response = new SimpleResponse(status, body.getBytes());
      }
   }
}
