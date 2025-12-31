class Qctl < Formula
  desc "CLI for the QRun ecosystem - scaffold, package, and deploy QQQ apps"
  homepage "https://github.com/QRun-IO/qctl"
  version "0.1.0"
  license "AGPL-3.0"

  on_macos do
    on_arm do
      url "https://github.com/QRun-IO/qctl/releases/download/v#{version}/qctl-macos-arm64"
      sha256 "PLACEHOLDER_ARM64_SHA256"
    end
    on_intel do
      url "https://github.com/QRun-IO/qctl/releases/download/v#{version}/qctl-macos-amd64"
      sha256 "PLACEHOLDER_AMD64_SHA256"
    end
  end

  on_linux do
    on_arm do
      url "https://github.com/QRun-IO/qctl/releases/download/v#{version}/qctl-linux-arm64"
      sha256 "PLACEHOLDER_LINUX_ARM64_SHA256"
    end
    on_intel do
      url "https://github.com/QRun-IO/qctl/releases/download/v#{version}/qctl-linux-amd64"
      sha256 "PLACEHOLDER_LINUX_AMD64_SHA256"
    end
  end

  depends_on "git" => :recommended

  def install
    binary = Dir["qctl*"].first
    bin.install binary => "qctl"
  end

  test do
    assert_match "qctl #{version}", shell_output("#{bin}/qctl --version")
  end
end
