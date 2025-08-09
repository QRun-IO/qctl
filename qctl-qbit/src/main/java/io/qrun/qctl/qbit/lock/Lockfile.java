package io.qrun.qctl.qbit.lock;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Lockfile {
  public int lockfileVersion = 1;
  public String generatedAt = Instant.now().toString();
  public Map<String, PackageEntry> packages = new LinkedHashMap<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class PackageEntry {
    public String name;
    public String version;
    public String resolved;
    public String integrity;
    public Map<String, String> dependencies;
    public String registry;
    public String vendorPath;
    public String signature;
  }
}
