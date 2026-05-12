# OkSocket

Lightweight blocking TCP socket library for Java and Android.

## Modules

- `socket-core` - low-level IO, packet reader/writer, protocol support.
- `socket-common-interface` - shared interfaces and default protocol helpers.
- `socket-client` - client-side connection management, reconnect, heartbeat.
- `socket-server` - server-side accept loop and client session handling.
- `app` - AndroidX/Material demo application, not required for `jar` builds.

## Features

- TCP client connections
- TCP server support
- Packet-based protocol with custom header parser
- Reconnect handling
- Heartbeat support
- SSL socket support

## TLS Notes

- `OkSocketSSLConfig` now uses the platform trust manager by default when no custom `TrustManager[]` is provided.
- TLS socket creation failures no longer downgrade to a plaintext `Socket`.
- If you explicitly need to trust self-signed or otherwise untrusted certificates for local development, pass `new TrustManager[]{new TrustAllX509TrustManager()}` in `OkSocketSSLConfig`.
- `TrustAllX509TrustManager` disables certificate validation and must not be used in production traffic.

## Requirements

- JDK 17 or newer
- Gradle 8.13 or newer

## Build JAR Files

Build all library jars from the project root:

### Windows

```powershell
gradle :socket-core:jar :socket-common-interface:jar :socket-client:jar :socket-server:jar
```

### macOS / Linux

```bash
gradle :socket-core:jar :socket-common-interface:jar :socket-client:jar :socket-server:jar
```

Generated artifacts:

- `socket-core/build/libs/socket-core.jar`
- `socket-common-interface/build/libs/socket-common-interface.jar`
- `socket-client/build/libs/socket-client.jar`
- `socket-server/build/libs/socket-server.jar`

You can also build a single module jar:

```powershell
gradle :socket-client:jar
```

## Runtime Notes

These are regular module jars, not a fat jar.

If you use:

- `socket-client.jar`, also include `socket-common-interface.jar` and `socket-core.jar`
- `socket-server.jar`, also include `socket-common-interface.jar` and `socket-core.jar`

## Quality Gates

- `gradle libraryCheck` - builds library jars and runs library unit tests only.
- `gradle libraryPublish` - publishes all Java library modules to `build/repos/releases` or `build/repos/snapshots`.
- `gradle demoCheck` - runs the Android demo release build and demo lint checks.
- `gradle qualityCheck` - runs both library and demo validation flows.

## License

Apache License 2.0. See [LICENSE](LICENSE).
