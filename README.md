# Launcher3

This repository is a mirror of AOSP (Android Open Source Project) Launcher3, making it easier to view the sources of each release on GitHub.

## About

Launcher3 is the default home screen launcher for Android, providing the main interface for launching apps, managing widgets, and organizing the home screen.

This mirror repository provides:
- Easy browsing of Launcher3 source code on GitHub
- Access to different Android releases and versions
- Better code navigation and search capabilities
- Convenient diff viewing between releases

## Official Source

The official AOSP Launcher3 source is maintained at:
https://android.googlesource.com/platform/packages/apps/Launcher3

## License

This project follows the Apache License 2.0 as per the original AOSP project.

```
Copyright (C) 2008 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Structure

The repository maintains the same structure as the official AOSP Launcher3:
- `src/` - Main source code
- `quickstep/` - Quickstep (recent apps) implementation
- `res/` - Resources (layouts, drawables, strings, etc.)
- `tests/` - Test suites
- `protos/` - Protocol buffer definitions
- And other supporting directories

## Building

This project is designed to be built as part of the Android platform build system. For standalone builds, refer to the Gradle configuration in `build.gradle`.

## Contributing

This is a mirror repository. For contributing to the actual Launcher3 project, please refer to the [official AOSP contribution guidelines](https://source.android.com/setup/contribute).
