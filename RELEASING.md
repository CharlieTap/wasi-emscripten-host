# Releasing

Maven Central releases contain only the WASI Preview 1 Chasm binding and the six runtime modules it requires. The
Emscripten and test-fixture modules are not publishable.

## Repository setup

Add these GitHub Actions secrets to the repository:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `SIGNING_KEY_ID`
- `SIGNING_PASSWORD`
- `GPG_KEY_CONTENTS`

The Maven Central account must be authorised to publish the `io.github.charlietap.wasi.emscripten.host` namespace.

## Release process

1. Change `weh_version` in `config/version.properties` to a non-SNAPSHOT version.
2. Update `CHANGELOG.md` and commit the release preparation.
3. Run the CI tasks documented in `README.md`, including Kotlin ABI compatibility validation.
4. Validate the JVM publications without contacting Maven Central:

   ```shell
   ./gradlew publishJvmPublicationToMavenLocal \
       -Dmaven.repo.local=/tmp/wasi-emscripten-host-maven \
       --no-configuration-cache
   ```

   On a macOS machine with Xcode, use `publishToMavenLocal` instead to validate every multiplatform variant.
5. Push the commit and create a GitHub prerelease or release for the version. The `Publish` workflow publishes all
   seven artifacts to a single Maven Central deployment.
6. Confirm the deployment in the [Maven Central Portal](https://central.sonatype.com/publishing/deployments) and publish
   it if it is awaiting manual approval.
7. Change `weh_version` to the next SNAPSHOT version and push that change.

A non-SNAPSHOT release must depend on a non-SNAPSHOT Chasm version.
