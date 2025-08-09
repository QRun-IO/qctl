package io.qrun.qctl.core.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qrun.qctl.shared.api.ProblemDetail;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public class ApiClient {
  private final HttpClient client;
  private final ObjectMapper mapper = new ObjectMapper();
  private final Duration requestTimeout;
  private final HeaderProvider headerProvider;

  public ApiClient(Duration requestTimeout) {
    this(requestTimeout, builder -> {});
  }

  public ApiClient(Duration requestTimeout, HeaderProvider headerProvider) {
    this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    this.requestTimeout = requestTimeout;
    this.headerProvider = headerProvider;
  }

  public <T> T getJson(URI uri, Class<T> type) throws IOException, InterruptedException, ApiException {
    HttpRequest.Builder b = HttpRequest.newBuilder(uri)
        .timeout(requestTimeout)
        .header("Accept", "application/json")
        .GET();
    headerProvider.apply(b);
    HttpRequest req = b.build();
    return send(req, type);
  }

  public <T> T send(HttpRequest req, Class<T> type) throws IOException, InterruptedException, ApiException {
    int attempts = 0;
    long backoff = 250;
    while (true) {
      attempts++;
      HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
      int sc = resp.statusCode();
      if (sc >= 200 && sc < 300) {
        if (type == Void.class) return null;
        return mapper.readValue(resp.body(), type);
      }
      if (sc == 401 || sc == 403) throw toApiException(resp, 4);
      if (sc == 404) throw toApiException(resp, 5);
      if (sc == 400 || sc == 422) throw toApiException(resp, 6);
      if (sc == 409 || sc == 412) throw toApiException(resp, 8);
      if (sc == 408 || sc == 429 || sc >= 500) {
        if (attempts >= 3) throw toApiException(resp, 3);
        try { Thread.sleep(Math.min(backoff, 5000)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw ie; }
        backoff = (long) (backoff * 1.7 + (Math.random() * 100));
        continue;
      }
      throw toApiException(resp, 1);
    }
  }

  private ApiException toApiException(HttpResponse<byte[]> resp, int exitCode) {
    try {
      ProblemDetail pd = mapper.readValue(resp.body(), ProblemDetail.class);
      return new ApiException(pd, exitCode);
    } catch (Exception e) {
      ProblemDetail pd = new ProblemDetail();
      pd.status = resp.statusCode();
      pd.title = "HTTP " + resp.statusCode();
      pd.detail = Optional.ofNullable(resp.headers().firstValue("X-Error").orElse(null)).orElse("Unexpected error");
      return new ApiException(pd, exitCode);
    }
  }

  public static int exitCodeForStatus(int sc) {
    if (sc == 401 || sc == 403) return 4;
    if (sc == 404) return 5;
    if (sc == 400 || sc == 422) return 6;
    if (sc == 409 || sc == 412) return 8;
    if (sc == 408 || sc == 429 || sc >= 500) return 3;
    return 1;
  }

  @FunctionalInterface
  public interface HeaderProvider {
    void apply(HttpRequest.Builder builder);
  }

  public static class ApiException extends Exception {
    public final ProblemDetail problem;
    public final int exitCode;
    public ApiException(ProblemDetail p, int exitCode) {
      super(p != null ? p.title + ": " + p.detail : "API error");
      this.problem = p;
      this.exitCode = exitCode;
    }
  }
}
