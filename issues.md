# Build Issues Log

---

## Issue 1 — Docker runtime image not found for ARM64

### Error
```
ERROR: failed to build: eclipse-temurin:17-jre-alpine:
no match for platform in manifest: not found
```

### Cause
`eclipse-temurin:17-jre-alpine` only publishes an `amd64` image.
The build machine is Apple Silicon (ARM64 / `linux/arm64`), so Docker could find no matching layer.

### Tries
1. Used `eclipse-temurin:17-jre-alpine` as the runtime base → failed (no ARM64 variant)

### Fix
Changed runtime stage to `eclipse-temurin:17-jre` (Ubuntu-based), which ships both `amd64` and `arm64` variants.
Docker auto-selects the right one at build time.

```diff
- FROM eclipse-temurin:17-jre-alpine
+ FROM eclipse-temurin:17-jre
```

### Learning
Alpine variants of Eclipse Temurin JRE are not always published for all architectures.
Always prefer the standard (`17-jre`) base image for cross-platform compatibility unless image size is a hard constraint.

---

## Issue 2 — shadowJar fails: `Could not add META-INF to ZIP`

### Error
```
GradleException: Could not add META-INF to ZIP '...BrowserStackMachineCodding-1.0-SNAPSHOT.jar'
Caused by: MissingPropertyException: No such property: mode
  for class: NormalizingCopyActionDecorator$StubbedFileCopyDetails
```

### Cause (surface)
Initial assumption: signed JARs in dependencies (`lettuce-core`, `commons-pool2`) write `.SF`/`.DSA`/`.RSA`
files under `META-INF/`, which become invalid when Shadow merges JARs.
Added `exclude 'META-INF/*.SF'` etc. — did NOT fix it.

### Real Cause
The true root cause is a **Gradle 9 incompatibility** in `com.github.johnrengelman.shadow` v8.1.1.
Gradle 9.x removed the `.mode` property from `FileCopyDetails`. The Shadow plugin called `.mode` internally
when visiting directories, causing a `MissingPropertyException` that surfaced as the ZIP error.

The project uses **Gradle 9.2.0** (confirmed in `gradle/wrapper/gradle-wrapper.properties`).

### Tries
1. Added `exclude 'META-INF/*.SF'` etc. in `shadowJar {}` → same error (wrong root cause)
2. Switched plugin from `com.github.johnrengelman.shadow:8.1.1` to `com.gradleup.shadow:8.3.6` → **fixed**

### Fix
```diff
- id 'com.github.johnrengelman.shadow' version '8.1.1'
+ id 'com.gradleup.shadow' version '8.3.6'
```

`com.gradleup.shadow` is the actively maintained successor to `johnrengelman/shadow` with full Gradle 9 support.

### Learning
- The error message `Could not add META-INF to ZIP` is a red herring — it's the symptom, not the cause.
- Always check plugin compatibility against the Gradle version first when seeing unexplained build failures.
- `com.github.johnrengelman.shadow` is effectively unmaintained for Gradle 9+; use `com.gradleup.shadow` instead.
- Gradle 9 introduced breaking API changes (removed `.mode`, `.permissions`, etc. from copy action APIs).
