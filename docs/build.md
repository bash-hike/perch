# Build from source

**Toolchain:** JDK 17, Android SDK (compileSdk 36, build-tools 36), Gradle 8.13 (wrapper), AGP 8.13.2, Kotlin 2.2.10,
Jetpack Compose (BOM 2024.10.01, Material 3 1.3), `androidx.palette`. minSdk 35, targetSdk 36.

```bash
git clone https://github.com/bash-hike/perch.git && cd perch
export JAVA_HOME=/path/to/jdk17          # e.g. /opt/homebrew/opt/openjdk@17
./gradlew :app:testDebugUnitTest   # unit tests (display rules)
./gradlew :app:assembleRelease           # → app/build/outputs/apk/release/app-release.apk
```

Debug builds add a **demo session** (`DemoSession.kt`, debug source set only; release builds contain no trace of
it) used to take screenshots without real content.

## Signing

Release builds sign with a key from the environment or a git-ignored `keystore.properties` (`storeFile`, `storePassword`,
`keyAlias`, `keyPassword`); with neither, they fall back to the debug key so `assembleRelease` still works for local
sideloading. Create a key once with `keytool -genkeypair -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
and keep it, and its password, safe: every update has to be signed with the same key.

CI reads `SIGNING_KEYSTORE_BASE64`, `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` from repository
secrets. Push a `vX.Y.Z` tag to build and publish a release (the CHANGELOG section becomes its notes).
