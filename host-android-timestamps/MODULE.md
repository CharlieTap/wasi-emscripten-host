# Module host-android-timestamps

Nanosecond file timestamps for the JVM NIO file system on Android.

On Android, `java.nio` reads and sets file timestamps with microsecond precision and cannot set the timestamps
of a symbolic link itself. With this artifact on the classpath, the host reads timestamps with `stat` and sets
them with `utimensat` instead, so `fd_filestat_get`, `path_filestat_get`, `fd_filestat_set_times` and
`path_filestat_set_times` keep nanoseconds and work on symbolic links.

## Usage

Add the dependency next to the bindings. No configuration is needed: the host discovers it when running on
Android.

```kotlin
dependencies {
    implementation("io.github.charlietap.wasi.emscripten.host:bindings-chasm-wasip1:<version>")
    implementation("io.github.charlietap.wasi.emscripten.host:host-android-timestamps:<version>")
}
```

The artifact contains a small native library for `arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`, and ships the
R8 rules it needs.

## Limitations

* Reading and setting the timestamps of a file descriptor use the path it was opened with, not the open
  descriptor. After the file is renamed or deleted, they may fail or act on another file at that path, as
  without this artifact.
* Android does not allow apps to create hard links; this artifact does not change that.
