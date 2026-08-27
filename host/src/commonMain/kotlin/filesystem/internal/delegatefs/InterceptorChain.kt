/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.filesystem.internal.delegatefs

import arrow.core.Either
import at.released.weh.filesystem.FileSystemInterceptor
import at.released.weh.filesystem.error.FileSystemOperationError
import at.released.weh.filesystem.op.FileSystemOperation

internal class InterceptorChain<I : Any, out E : FileSystemOperationError, out R : Any>(
    override val operation: FileSystemOperation<I, E, R>,
    initialInput: I,
    private val interceptors: List<FileSystemInterceptor>,
) : FileSystemInterceptor.Chain<I, E, R> {
    private var currentInput: I = initialInput
    private var index: Int = 0

    override val input: I
        get() = currentInput

    override fun proceed(input: I): Either<E, R> {
        val interceptor = interceptors.getOrNull(index) ?: error("End of interceptor chain")
        val previousInput = currentInput
        currentInput = input
        index += 1
        return try {
            interceptor.intercept(this)
        } finally {
            index -= 1
            currentInput = previousInput
        }
    }
}
