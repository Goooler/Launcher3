# Complete the standalone Gradle build

## Summary

Launcher3 should be buildable from a standalone clone with the Gradle wrapper, without requiring
an AOSP source checkout or Soong-generated files. The current Gradle configuration is a useful
starting point and can compile selected Kotlin sources, but it does not yet provide a complete
Java, D8, packaging, testing, and runtime path.

This document tracks the remaining work and defines the acceptance criteria for a supported
standalone build.

## Supported products

The standalone build must treat the following as separate products because they have different
runtime requirements:

1. **Launcher3 without Quickstep**
   - Produces a conventional, installable Home application.
   - Must build and run without platform signing or privileged installation.
   - Platform-only features may be disabled when no public API equivalent exists.
2. **Launcher3 with Quickstep**
   - Produces an APK outside Soong, but still targets a matching Android platform release.
   - Requires platform APIs, platform signing, privileged permissions, SystemUI integration, and
     installation in the system image.
   - A locally assembled debug APK is not expected to provide Recents on an arbitrary device.
3. **Launcher3 Go**
   - Is enabled after the two AOSP variants have a stable source-set and dependency model.

The first milestone is `aospWithoutQuickstepDebug`. Quickstep and Go must not block delivery of
that milestone.

## Phase 1: Produce a standalone Launcher3 APK

### AppFunctions

- [ ] Replace the incomplete AppFunctions classes in `SharedLibWrapper` with the real AndroidX
      AppFunctions runtime and service artifacts.
- [ ] Configure the required KSP/compiler plugin and generated metadata.
- [ ] Verify that `AppFunctionConfiguration.Builder`, `ExtensionAppFunctionService`, and generated
      factories are present in the assembled application.
- [ ] If the required artifacts cannot be consumed from Gradle, disable AppFunctions cleanly for
      the standalone product:
  - exclude `modules/appfunctions/src` from that variant;
  - remove the AppFunctions service and metadata from its merged manifest;
  - provide a `LauncherApplication` implementation that has no AppFunctions references.
- [ ] Do not keep API-shaped stubs in the `androidx.appfunctions` namespace.

### Android SDK and hidden APIs

- [ ] Use one consistent platform level for `compileSdk`, `targetSdk`, AAPT2, and the platform API
      stubs. The Android 17 source currently expects Android 37 APIs.
- [ ] Replace the mixed `compileSdk 35` plus `android-37/android.jar` setup.
- [ ] Ensure the selected full-framework stub JAR is used intentionally for hidden APIs and does
      not accidentally conflict with AGP's boot classpath.
- [ ] Remove the blanket disabling of `AarMetadata` tasks and resolve the underlying SDK metadata
      requirements.
- [ ] Align `minSdk` with the APIs actually used by each product. Do not advertise API 26 or 28
      support when the AOSP module requires API 31 or a current platform build.
- [ ] Document the supported JDK, Gradle, AGP, Kotlin, and Android SDK versions.
- [ ] Reconcile the branch's Gradle/AGP versions with `trunk` and keep a tested compatible pair.

### Compatibility implementations

- [ ] Remove placeholder implementations from AndroidX-owned packages in `SharedLibWrapper`.
- [ ] Use released AndroidX APIs where equivalent implementations exist.
- [ ] Put unavoidable compatibility code in a Launcher3-owned package.
- [ ] Replace the placeholder `SvgPathParser.parseFeatures()` and `RoundedPolygon(features)`
      implementation with behavior that preserves the source path and icon shape.
- [ ] Replace placeholder Material 3 motion and typography behavior with the intended APIs or a
      documented Launcher3 compatibility layer.
- [ ] Remove `SharedLibWrapper/src/main/kotlin` from the WidgetPicker source directories; consume
      it only through the `:SharedLibWrapper` project dependency.
- [ ] Remove temporary resource values that merely silence AAPT errors, or document and test each
      intentional standalone fallback.

### Source sets and manifests

- [ ] Define source sets per supported product instead of accumulating all AOSP directories in the
      application module.
- [ ] Verify manifest merging independently for `aospWithoutQuickstepDebug` and
      `aospWithQuickstepDebug`.
- [ ] Keep platform-only services, permissions, providers, and feature-flagged manifest entries out
      of the conventional Launcher APK when they cannot work there.
- [ ] Verify that every manifest component exists in the selected variant.
- [ ] Remove the temporary WidgetPicker `R` type alias once the module namespace and resource
      dependency are configured correctly.

### Phase 1 acceptance criteria

- [ ] A fresh clone with initialized declared dependencies can run:

  ```shell
  ./gradlew --non-interactive :assembleAospWithoutQuickstepDebug
  ```

- [ ] The build succeeds through Java/Kotlin compilation, annotation processing, resource linking,
      D8, and APK packaging.
- [ ] The APK installs on a documented supported emulator or device.
- [ ] It can be selected as the default Home application and completes a basic smoke test:
  - cold start;
  - workspace load;
  - All Apps open and launch;
  - shortcut creation;
  - widget bind flow;
  - rotation/configuration change.
- [ ] Building from a directory that is not nested inside an AOSP checkout produces the same result.

## Phase 2: Make Quickstep buildable outside Soong

### Remove AOSP checkout dependencies

- [ ] Remove `ANDROID_TOP` and `FRAMEWORK_PREBUILTS_DIR` lookups from `build.gradle`.
- [ ] Replace the implicit references to these files:
  - `prebuilts/framework_intermediates/quickstep/libs/sysui_shared.jar`;
  - `prebuilts/framework_intermediates/libs/plugin_core.jar`.
- [ ] Decide how each platform dependency is supplied:
  - a Gradle module built from pinned source;
  - a versioned prebuilt generated from the matching AOSP release; or
  - a published artifact with immutable versioning and checksums.
- [ ] Record the AOSP tag, source commit, build command, license, and checksum for every checked-in
      prebuilt.
- [ ] Fail configuration with a clear error when a required prebuilt is absent. Do not rely on an
      empty `fileTree`, which can silently hide a missing dependency.

The dependency inventory must cover at least:

- [ ] SystemUI Shared and its AIDL interfaces;
- [ ] WindowManager Shell shared classes, flags, and AIDL interfaces;
- [ ] SystemUI animation, contextual education, unfold, view capture, mechanics, MSDL, and icon
      loader APIs used by Launcher;
- [ ] SettingsLib resources and theme APIs;
- [ ] SystemUI/framework stats logging APIs;
- [ ] ProtoLog runtime APIs;
- [ ] framework and shell AConfig flag APIs;
- [ ] AppFunctions and optional extension APIs;
- [ ] any platform Compose libraries that do not have a public AndroidX equivalent.

### ProtoLog

- [ ] Add `quickstep/src_protolog` to the Quickstep variant or generate equivalent sources.
- [ ] Make `QuickstepProtoLogGroup`, `ActiveGestureProtoLogProxy`,
      `OverviewCommandHelperProtoLogProxy`, `StateManagerProtoLogProxy`, and
      `RecentsWindowProtoLogProxy` available during Java and Kotlin compilation.
- [ ] Either reproduce the AOSP ProtoLog transformation/viewer configuration or explicitly provide
      a no-op/logcat-only standalone implementation.
- [ ] Document any loss of binary ProtoLog functionality in standalone builds.

### Flags

- [ ] Replace generated flag methods that unconditionally return `false` with an explicit standalone
      product configuration.
- [ ] Preserve fixed read-only flag values required by the selected AOSP release.
- [ ] Cover Launcher, SystemUI Shared, WindowManager Shell, framework, app widget, security, and
      multi-user flags used by compiled sources.
- [ ] Convert `tools/generate_aconfig_flags.py` into a deterministic Gradle generation task or
      replace it with a supported generator.
- [ ] Make generated sources build outputs where practical; otherwise add a freshness check that
      fails when committed output differs from the `.aconfig` inputs.
- [ ] Keep AAPT2 `--feature-flags` values generated from the same configuration as Java/Kotlin flag
      APIs.

### Platform installation and runtime

- [ ] Add a documented platform signing configuration that does not commit private keys.
- [ ] Document how the Gradle-built APK is inserted into `system_ext/priv-app` in a matching Android
      image.
- [ ] Provide or document the matching privapp permission allowlist.
- [ ] Verify that the package receives every signature/privileged permission required by the
      Quickstep manifest.
- [ ] Verify the SystemUI/Launcher binder interface versions match the target system image.
- [ ] Test gesture navigation, Recents, task launch, split screen, desktop mode, taskbar, bubbles,
      unfold, and SystemUI reconnection on the supported platform image.

### Phase 2 acceptance criteria

- [ ] A fresh standalone clone can run:

  ```shell
  ./gradlew --non-interactive :assembleAospWithQuickstepDebug
  ```

- [ ] No input is read from an enclosing AOSP checkout or Soong output directory.
- [ ] The result can be platform-signed outside the repository and installed into a documented
      matching system image.
- [ ] Launcher starts as Home and the Quickstep service connects to SystemUI without class, binder,
      permission, or hidden-API failures.

## Phase 3: Restore variants and release builds

- [ ] Add the missing `go/quickstep/src` and `go/quickstep/res` inputs to the appropriate Go
      variants.
- [ ] Verify that Go variants do not combine incompatible regular Quickstep and Go override sources.
- [ ] Define the exact supported flavor matrix and disable unsupported combinations through the
      modern Android Components variant API.
- [ ] Re-enable release variants.
- [ ] Configure R8 and resource shrinking with `proguard.flags`.
- [ ] Add a secure, documented release-signing handoff.
- [ ] Set meaningful version code/name values through release configuration rather than hardcoded
      placeholders.
- [ ] Validate reproducibility of release artifacts.

## Phase 4: Tests and continuous integration

### Test source sets

- [ ] Restore Gradle unit-test configuration for Launcher tests under `tests/multivalentTests`.
- [ ] Restore Quickstep unit-test configuration under `quickstep/tests/multivalentTests`.
- [ ] Restore WidgetPicker unit, instrumentation, and screenshot test source sets.
- [ ] Reintroduce Robolectric, JUnit, AndroidX Test, Mockito, Truth, coroutine-test, and Compose test
      dependencies as required.
- [ ] Separate tests that require a platform image from tests that run on a regular Gradle worker.
- [ ] Replace dependencies on AOSP-only test libraries with local Gradle modules or documented
      test prebuilts.

### CI

- [ ] Add a GitHub Actions workflow that checks out all pinned dependencies.
- [ ] Run, at minimum:

  ```shell
  ./gradlew --non-interactive :assembleAospWithoutQuickstepDebug
  ./gradlew --non-interactive :testAospWithoutQuickstepDebugUnitTest
  ./gradlew --non-interactive :lintAospWithoutQuickstepDebug
  ```

- [ ] Add the Quickstep assemble task once Phase 2 is complete.
- [ ] Cache Gradle safely without making the build depend on undeclared local state.
- [ ] Verify generated flags and pinned prebuilt checksums in CI.
- [ ] Run `git diff --check` and remove the current trailing whitespace and extra EOF blank lines.

## Maintenance requirements

- [ ] Document the standalone build in `README.md`, including prerequisites, supported variants,
      initialization steps, build commands, output paths, and runtime limitations.
- [ ] Pin every source and binary dependency to an immutable version.
- [ ] Provide an update procedure for each AOSP release merge.
- [ ] Add a dependency audit comparing Gradle inputs against the relevant `Android.bp` modules so
      upstream changes do not silently omit new source directories or libraries.
- [ ] Avoid modifying upstream Launcher behavior solely to satisfy compilation. Compatibility
      changes must be isolated by source set or a documented adapter.
- [ ] Keep standalone-build changes in focused commits to make future AOSP merges reviewable.

## Definition of done

This work is complete when:

1. The supported Gradle variants build from a fresh standalone clone without an AOSP checkout.
2. No missing dependency is masked by an empty file collection, disabled metadata validation, or
   an API-shaped stub with incorrect runtime behavior.
3. The ordinary Launcher APK passes its documented device smoke tests.
4. The Quickstep APK passes its documented platform-image integration tests.
5. Unit tests, lint, generation checks, and assembly run continuously in CI.
6. A new AOSP tag can be merged and its Gradle dependency changes can be identified and updated by
   following the documented maintenance procedure.
