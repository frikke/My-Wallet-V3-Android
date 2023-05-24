fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android build

```sh
[bundle exec] fastlane android build
```

Build using the given environment (default: Staging) and build type (default: Debug).

### android staging_release

```sh
[bundle exec] fastlane android staging_release
```

Build Staging Release

### android prod_debug

```sh
[bundle exec] fastlane android prod_debug
```

Build Prod Debug

### android prod_release

```sh
[bundle exec] fastlane android prod_release
```

Build Prod Release

### android test

```sh
[bundle exec] fastlane android test
```

Run tests. Optional flags: environment (Staging), build_type (Debug), module(app), test_name (runs all by default). Environment and build_type are app module-only.

### android upload_to_internal_track

```sh
[bundle exec] fastlane android upload_to_internal_track
```

Submit a release build to the Play Store internal test track.

### android credentials

```sh
[bundle exec] fastlane android credentials
```

Get the configuration files from the Android credentials repository.

### android ci_run_tests

```sh
[bundle exec] fastlane android ci_run_tests
```

Bundle of build, perform checks and run tests on CI.

### android ci_test_app

```sh
[bundle exec] fastlane android ci_test_app
```

Tests to run on CI app

### android ci_modules_tests

```sh
[bundle exec] fastlane android ci_modules_tests
```

Tests to run on all modules

### android ci_credentials

```sh
[bundle exec] fastlane android ci_credentials
```

Get the configuration files from the Android credentials repository on CI.

### android ci_credentials_cleanup

```sh
[bundle exec] fastlane android ci_credentials_cleanup
```

Cleanup the credentials repository

### android ci_upload_to_appcenter

```sh
[bundle exec] fastlane android ci_upload_to_appcenter
```

Upload to AppCenter.

### android ci_export_build

```sh
[bundle exec] fastlane android ci_export_build
```

Export the build path to environment variables for upload. Optional flags: export_bundle (APK is default), do_sign (False is default).

### android ci_build

```sh
[bundle exec] fastlane android ci_build
```

Build to run on CI. Optional flags: copy_credentials, build_bundle (APK is default), export_build(False is default), do_sign (False is default).

### android ci_lint

```sh
[bundle exec] fastlane android ci_lint
```

Checks to run on CI

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
