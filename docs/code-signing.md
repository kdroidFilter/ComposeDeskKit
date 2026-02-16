# Code Signing

Code signing ensures your application is trusted by the operating system and not flagged as malware. Nucleus supports signing for Windows and macOS.

## Windows

### PFX Certificate

Sign Windows installers (NSIS, MSI, AppX) with a `.pfx` / `.p12` certificate:

```kotlin
windows {
    signing {
        enabled = true
        certificateFile.set(file("certs/certificate.pfx"))
        certificatePassword = "your-password"
        algorithm = SigningAlgorithm.Sha256
        timestampServer = "http://timestamp.digicert.com"
    }
}
```

### Signing DSL Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | `Boolean` | `false` | Enable code signing |
| `certificateFile` | `RegularFileProperty` | — | Path to `.pfx` / `.p12` certificate |
| `certificatePassword` | `String?` | `null` | Certificate password |
| `certificateSha1` | `String?` | `null` | SHA-1 thumbprint (for store-installed certs) |
| `certificateSubjectName` | `String?` | `null` | Subject name of the certificate |
| `algorithm` | `SigningAlgorithm` | `Sha256` | Signing algorithm |
| `timestampServer` | `String?` | `null` | Timestamp server URL |

### Signing Algorithms

| Algorithm | Description |
|-----------|-------------|
| `SigningAlgorithm.Sha1` | Legacy, for older Windows |
| `SigningAlgorithm.Sha256` | Recommended |
| `SigningAlgorithm.Sha512` | Strongest |

### Common Timestamp Servers

| Provider | URL |
|----------|-----|
| DigiCert | `http://timestamp.digicert.com` |
| Sectigo | `http://timestamp.sectigo.com` |
| GlobalSign | `http://timestamp.globalsign.com` |

### Azure Trusted Signing

For cloud-based signing with [Azure Trusted Signing](https://learn.microsoft.com/en-us/azure/trusted-signing/):

```kotlin
windows {
    signing {
        enabled = true
        azureTenantId = "your-tenant-id"
        azureEndpoint = "https://your-region.codesigning.azure.net"
        azureCertificateProfileName = "your-profile"
        azureCodeSigningAccountName = "your-account"
    }
}
```

### CI/CD: Secrets Management

Never commit certificates or passwords to source control. Use environment variables or CI secrets:

```kotlin
windows {
    signing {
        enabled = true
        certificateFile.set(file(System.getenv("WIN_CSC_LINK") ?: "certs/certificate.pfx"))
        certificatePassword = System.getenv("WIN_CSC_KEY_PASSWORD")
        algorithm = SigningAlgorithm.Sha256
        timestampServer = "http://timestamp.digicert.com"
    }
}
```

In GitHub Actions:

```yaml
env:
  WIN_CSC_LINK: ${{ secrets.WIN_CSC_LINK }}
  WIN_CSC_KEY_PASSWORD: ${{ secrets.WIN_CSC_KEY_PASSWORD }}
```

> **Tip:** Base64-encode your `.pfx` file for CI:
> ```bash
> base64 -i certificate.pfx -o certificate.b64
> ```
> Store the content as a GitHub secret, then decode at build time:
> ```yaml
> - name: Decode certificate
>   run: echo "${{ secrets.WIN_CSC_LINK }}" | base64 -d > certificate.pfx
> ```

## macOS

### Prerequisites

macOS signing requires an [Apple Developer ID certificate](https://developer.apple.com/developer-id/):

1. Enroll in the [Apple Developer Program](https://developer.apple.com/programs/)
2. Create a "Developer ID Application" certificate in Xcode or the Apple Developer portal
3. The certificate must be in your local Keychain (or a CI keychain)

### Signing Configuration

```kotlin
macOS {
    signing {
        sign.set(true)
        identity.set("Developer ID Application: My Company (TEAMID)")
        // keychain.set("/path/to/keychain.keychain-db")  // Optional
    }
}
```

### Notarization

Apple notarization is required for distributing outside the Mac App Store on macOS 10.15+:

```kotlin
macOS {
    notarization {
        appleID.set("dev@example.com")
        password.set("@keychain:AC_PASSWORD")
        teamID.set("TEAMID")
    }
}
```

> **Tip:** Use `xcrun notarytool store-credentials` to save credentials in the keychain:
> ```bash
> xcrun notarytool store-credentials "AC_PASSWORD" \
>   --apple-id "dev@example.com" \
>   --team-id "TEAMID" \
>   --password "app-specific-password"
> ```

### CI/CD: macOS Signing

For GitHub Actions, import the certificate into a temporary keychain:

```yaml
- name: Import certificate
  env:
    MACOS_CERTIFICATE: ${{ secrets.MACOS_CERTIFICATE }}
    MACOS_CERTIFICATE_PWD: ${{ secrets.MACOS_CERTIFICATE_PWD }}
    KEYCHAIN_PWD: ${{ secrets.KEYCHAIN_PWD }}
  run: |
    echo "$MACOS_CERTIFICATE" | base64 -d > certificate.p12
    security create-keychain -p "$KEYCHAIN_PWD" build.keychain
    security default-keychain -s build.keychain
    security unlock-keychain -p "$KEYCHAIN_PWD" build.keychain
    security import certificate.p12 -k build.keychain -P "$MACOS_CERTIFICATE_PWD" -T /usr/bin/codesign
    security set-key-partition-list -S apple-tool:,apple: -s -k "$KEYCHAIN_PWD" build.keychain
```

### Entitlements

For apps using certain capabilities (network, file access, JIT), provide entitlements:

```kotlin
macOS {
    entitlementsFile.set(project.file("entitlements.plist"))
    runtimeEntitlementsFile.set(project.file("runtime-entitlements.plist"))
}
```

Minimal `entitlements.plist` for a JVM app:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.cs.allow-jit</key>
    <true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>
    <true/>
    <key>com.apple.security.cs.allow-dyld-environment-variables</key>
    <true/>
</dict>
</plist>
```
