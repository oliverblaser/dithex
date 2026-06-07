# Dithex

Dithex is a Compose Multiplatform image workbench that translates color images into a
tightly packed, three-tone hexagonal dot grid using serpentine Floyd-Steinberg dithering.

The project targets Desktop (JVM 25) and modern browsers through Kotlin/Wasm.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`
- Web app: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Desktop tests: `./gradlew :shared:jvmTest`
- Web tests: `./gradlew :shared:wasmJsTest`

### Versioning

Dithex follows semantic versioning while it is under active development. The current application
version is defined by `dithex.version` in `gradle.properties`, and published releases are marked
with matching Git tags such as `v0.2.0`.

- Minor versions add user-facing functionality.
- Patch versions contain compatible fixes and refinements.
- Version `1.0.0` will mark the first stable release.

### Deploying the web app

The web application is static and processes loaded images entirely inside the visitor's
browser. The repository includes an AWS CDK app in [`infrastructure`](./infrastructure)
that creates private S3 and CloudFront hosting without committing AWS account details or
credentials.

Infrastructure deployment and application uploads are intentionally manual. See the
[infrastructure deployment guide](./infrastructure/README.md) for the required commands.

Before publishing this repository as open source, add the license under which you want
others to use and modify Dithex.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).
