package io.qrun.qctl.core.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.qrun.qctl.shared.api.ProblemDetail;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import javax.net.ssl.SSLSession;

class ApiClientTest {

  @Test
  void header_provider_is_invoked() throws Exception {
    CapturingClient client = new CapturingClient(200, "{}");
    ApiClient api = new ApiClient(Duration.ofSeconds(1), b -> b.header("X-Custom", "abc")) {
      @Override protected HttpResponse<byte[]> sendOnce(HttpRequest req) { client.captured = req; return client.response; }
    };
    api.getJson(URI.create("http://localhost/test"), java.util.Map.class);
    assertThat(client.captured.headers().firstValue("X-Custom")).contains("abc");
  }

  @Test
  void error_mapping_status_codes() throws Exception {
    assertExitForStatus(401, 4);
    assertExitForStatus(403, 4);
    assertExitForStatus(404, 5);
    assertExitForStatus(400, 6);
    assertExitForStatus(422, 6);
    assertExitForStatus(409, 8);
    assertExitForStatus(412, 8);
    assertExitForStatus(429, 3);
    assertExitForStatus(503, 3);
  }

  private void assertExitForStatus(int status, int expectedExit) throws Exception {
    byte[] body = problem(status).getBytes();
    HttpResponse<byte[]> resp = new SimpleResponse(status, body);
    ApiClient client = new ApiClient(Duration.ofSeconds(1), b -> {}) {
      @Override protected HttpResponse<byte[]> sendOnce(HttpRequest req) { return resp; }
    };
    assertThatThrownBy(() -> client.getJson(URI.create("http://localhost/x"), java.util.Map.class))
        .isInstanceOf(ApiClient.ApiException.class)
        .satisfies(ex -> assertThat(((ApiClient.ApiException) ex).exitCode).isEqualTo(expectedExit));
  }

  private static String problem(int status) throws Exception {
    ProblemDetail pd = new ProblemDetail();
    pd.status = status;
    pd.title = "Status " + status;
    pd.detail = "test";
    com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
    return m.writeValueAsString(pd);
  }

  private static final class SimpleResponse implements HttpResponse<byte[]> {
    private final int status;
    private final byte[] body;
    SimpleResponse(int status, byte[] body) { this.status = status; this.body = body; }
    @Override public int statusCode() { return status; }
    @Override public HttpRequest request() { return null; }
    @Override public Optional<HttpResponse<byte[]>> previousResponse() { return Optional.empty(); }
    @Override public java.net.http.HttpHeaders headers() { return java.net.http.HttpHeaders.of(java.util.Map.of(), (a,b)->true); }
    @Override public byte[] body() { return body; }
    @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
    @Override public URI uri() { return URI.create("http://localhost"); }
    @Override public java.net.http.HttpClient.Version version() { return java.net.http.HttpClient.Version.HTTP_1_1; }
  }

  private static final class CapturingClient {
    HttpRequest captured;
    HttpResponse<byte[]> response;
    CapturingClient(int status, String body) {
      this.response = new SimpleResponse(status, body.getBytes());
    }
  }
}
