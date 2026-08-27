/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.wasi.preview1.ext

import arrow.core.Either
import arrow.core.flatMap
import at.released.weh.filesystem.path.virtual.VirtualPath
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasi.preview1.type.Size
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.ReadOnlyMemory
import at.released.weh.wasm.core.memory.read
import at.released.weh.wasm.core.memory.sourceWithMaxSize
import kotlinx.io.Buffer
import kotlinx.io.EOFException
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.readByteString
import kotlinx.io.write
import kotlinx.io.writeString

internal fun ReadOnlyMemory.readPathString(
    @IntWasmPtr(Byte::class) path: WasmPtr,
    pathSize: Int,
): Either<Errno, VirtualPath> = Either.catch {
    sourceWithMaxSize(path, pathSize).buffered().use {
        it.readByteString(pathSize).decodeToString()
    }
}.mapLeft {
    when (it) {
        is IllegalArgumentException, is IllegalStateException -> Errno.INVAL
        is IOException -> Errno.IO
        is EOFException -> Errno.IO
        else -> Errno.FAULT
    }
}.flatMap { pathString ->
    VirtualPath.create(pathString).mapLeft { _ -> Errno.INVAL }
}

context(_: MemoryAccess<M>)
internal fun <M> M.readPathString(
    @IntWasmPtr(Byte::class) path: WasmPtr,
    pathSize: Int,
): Either<Errno, VirtualPath> = Either.catch {
    val bytes = ByteArray(pathSize)
    read(bytes, path, pathSize)
    bytes
}.mapLeft {
    when (it) {
        is IllegalArgumentException, is IllegalStateException -> Errno.INVAL
        is IOException -> Errno.IO
        is EOFException -> Errno.IO
        else -> Errno.FAULT
    }
}.flatMap { pathBytes ->
    VirtualPath.createOwnedUtf8(pathBytes).mapLeft { _ -> Errno.INVAL }
}

internal fun <M> M.readPathString(
    @IntWasmPtr(Byte::class) path: WasmPtr,
    pathSize: Int,
    memoryAccess: MemoryAccess<M>,
): Either<Errno, VirtualPath> = with(memoryAccess) {
    readPathString(path, pathSize)
}

/**
 * The size of the binary representation of the WASI file system path.
 * This encoded string is not null-terminated.
 */
internal fun VirtualPath.encodedLength(): Size = this.utf8SizeBytes

internal fun VirtualPath.encodeToBuffer(): Buffer = Buffer().also { buffer ->
    buffer.write(this.utf8Bytes)
}

internal fun String.encodeToBuffer(): Buffer = Buffer().also { buffer ->
    buffer.writeString(this)
}
