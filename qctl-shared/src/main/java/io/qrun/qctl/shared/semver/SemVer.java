package io.qrun.qctl.shared.semver;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SemVer implements Comparable<SemVer> {
  private static final Pattern P = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$");
  public final int major, minor, patch;

  public SemVer(int major, int minor, int patch) {
    this.major = major; this.minor = minor; this.patch = patch;
  }

  public static SemVer parse(String s) {
    Matcher m = P.matcher(Objects.requireNonNull(s));
    if (!m.matches()) throw new IllegalArgumentException("Invalid semver: " + s);
    return new SemVer(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
  }

  @Override public int compareTo(SemVer o) {
    int c = Integer.compare(this.major, o.major);
    if (c != 0) return c;
    c = Integer.compare(this.minor, o.minor);
    if (c != 0) return c;
    return Integer.compare(this.patch, o.patch);
  }

  @Override public String toString() { return major+"."+minor+"."+patch; }
}
