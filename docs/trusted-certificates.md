# Trusted CA Certificates

Import custom CA certificates into the bundled JVM's `cacerts` keystore at build time.

## Use Case

Corporate proxies, VPN gateways, or ISP-level filtering services often
use a private root CA that is not trusted by the default JDK trust store. Without their
certificate, any HTTPS connection your app makes will throw an `SSLHandshakeException`.

Instead of asking users to patch their JVM manually, Nucleus lets you declare the
certificates once in your build script — they are imported automatically during packaging.

## Configuration

```kotlin
nativeDistributions {
    trustedCertificates.from(files(
        "certs/company-proxy-ca.pem",
        "certs/company-proxy-ca-2.pem"
    ))
}
```

Both PEM (`-----BEGIN CERTIFICATE-----`) and DER (binary) formats are accepted.

## How It Works

1. After the JLink runtime image is created, Nucleus copies it to a separate
   `runtime-patched/` directory.
2. For each certificate file, it runs:
   ```
   keytool -import -trustcacerts -alias <name>-<hash> \
           -keystore <runtime>/lib/security/cacerts   \
           -storepass changeit -noprompt -file <cert>
   ```
3. `createDistributable` and `createSandboxedDistributable` both use the patched
   runtime, so every packaging format (DMG, NSIS, DEB, PKG, AppX…) embeds the
   trusted certificate.

## Alias Generation

Each certificate is imported under a unique alias derived from its filename and a
short SHA-256 fingerprint of its content:

```
corp-root-ca.crt        →  corp-root-ca-3a1f8b2c
proxy/ca.crt            →  ca-d341ce29
vpn/ca.crt              →  ca-8473a9f4
```

This guarantees no collision even when multiple certificate files share the same name
(e.g. two different `ca.crt` from different directories).

## Idempotency

If a certificate with the same alias is already present in `cacerts`, the import is
silently skipped. Rebuilding the project without changing the certificate files or the
JLink runtime is instant (the `patchCaCertificates` Gradle task is up-to-date).

## Gradle Task

| Task | Description |
|------|-------------|
| `patchCaCertificates` | Copies the runtime image and imports all configured certificates |

The task is only registered when `trustedCertificates` is non-empty. It runs
automatically as part of `createDistributable`; you do not need to invoke it manually.

```
createRuntimeImage
       ↓
patchCaCertificates     ←  copies runtime, runs keytool for each cert
       ↓
createDistributable
createSandboxedDistributable
       ↓
packageDmg / packageDeb / packageNsis / …
```

## Notes

- The original JLink runtime image is **never modified**. The patched copy lives in
  `build/compose/tmp/<appName>/runtime-patched/`.
- The `keytool` binary used is the one from the JDK configured via `javaHome` (or the
  Gradle daemon's JVM if not set).
- This feature patches the **bundled JVM** only. The host machine's JVM is not affected.
