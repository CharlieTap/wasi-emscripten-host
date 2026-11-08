/*
 * Copyright 2026, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

#include <errno.h>
#include <fcntl.h>
#include <jni.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <time.h>

#define NANOS_PER_SECOND 1000000000LL

static int to_timespec(jboolean present, jlong nanoseconds, struct timespec *out) {
    if (!present) {
        out->tv_sec = 0;
        out->tv_nsec = UTIME_OMIT;
        return 0;
    }
    long long seconds = nanoseconds / NANOS_PER_SECOND;
    long long remainder = nanoseconds % NANOS_PER_SECOND;
    if (remainder < 0) {
        remainder += NANOS_PER_SECOND;
        seconds -= 1;
    }
    if ((long long) (time_t) seconds != seconds) {
        return EOVERFLOW;
    }
    out->tv_sec = (time_t) seconds;
    out->tv_nsec = (long) remainder;
    return 0;
}

JNIEXPORT jint JNICALL
Java_at_released_weh_filesystem_nio_android_NativeTimestamps_utimensat(
    JNIEnv *env,
    jclass clazz,
    jbyteArray path,
    jboolean followSymlinks,
    jboolean hasAtime,
    jlong atime,
    jboolean hasMtime,
    jlong mtime
) {
    (void) clazz;
    struct timespec times[2];
    int error = to_timespec(hasAtime, atime, &times[0]);
    if (error == 0) {
        error = to_timespec(hasMtime, mtime, &times[1]);
    }
    if (error != 0) {
        return error;
    }

    jsize length = (*env)->GetArrayLength(env, path);
    char *nativePath = malloc((size_t) length + 1);
    if (nativePath == NULL) {
        return ENOMEM;
    }
    (*env)->GetByteArrayRegion(env, path, 0, length, (jbyte *) nativePath);
    nativePath[length] = '\0';

    int flags = followSymlinks ? 0 : AT_SYMLINK_NOFOLLOW;
    int result = utimensat(AT_FDCWD, nativePath, times, flags);
    int savedErrno = result == 0 ? 0 : errno;
    free(nativePath);
    return savedErrno;
}
