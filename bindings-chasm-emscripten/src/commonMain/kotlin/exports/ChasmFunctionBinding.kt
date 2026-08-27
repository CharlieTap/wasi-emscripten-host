/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.weh.bindings.chasm.exports

import at.released.weh.bindings.chasm.exception.ChasmErrorException
import at.released.weh.bindings.chasm.ext.asInt
import at.released.weh.bindings.chasm.ext.asLong
import at.released.weh.bindings.chasm.ext.asWasmAddr
import at.released.weh.bindings.chasm.ext.orThrow
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmFunctionBinding
import at.released.weh.wasm.core.WasmPtr
import io.github.charlietap.chasm.embedding.error.ChasmError
import io.github.charlietap.chasm.embedding.prepareFunction
import io.github.charlietap.chasm.embedding.shapes.Instance
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.fold
import io.github.charlietap.chasm.runtime.value.NumberValue

internal class ChasmFunctionBinding(
    store: Store,
    instance: Instance,
    name: String,
) : WasmFunctionBinding {
    private val preparedFunction = prepareFunction(store, instance, name).orThrow()

    override fun executeVoid(vararg args: Any?) {
        preparedFunction(args.argsToValues()).orThrow()
    }

    override fun executeForInt(vararg args: Any?): Int = preparedFunction(args.argsToValues())
        .fold(
            { it[0].asInt() },
            ::throwOnError,
        )

    override fun executeForLong(vararg args: Any?): Long = preparedFunction(args.argsToValues())
        .fold(
            { it[0].asLong() },
            ::throwOnError,
        )

    override fun executeForFloat(vararg args: Any?): Float = preparedFunction(args.argsToValues())
        .fold(
            { (it[0] as NumberValue.F32).value },
            ::throwOnError,
        )

    override fun executeForDouble(vararg args: Any?): Double = preparedFunction(args.argsToValues())
        .fold(
            { (it[0] as NumberValue.F64).value },
            ::throwOnError,
        )

    @IntWasmPtr
    override fun executeForPtr(vararg args: Any?): WasmPtr = preparedFunction(args.argsToValues())
        .fold(
            { it[0].asWasmAddr() },
            ::throwOnError,
        )
}

private fun Array<out Any?>.argsToValues(): List<NumberValue<*>> {
    return if (this.isEmpty()) {
        emptyList()
    } else {
        List(this.size) { idx ->
            when (val arg = this[idx]) {
                is Int -> NumberValue.I32(arg)
                is UInt -> NumberValue.I32(arg.toInt())
                is Long -> NumberValue.I64(arg)
                is ULong -> NumberValue.I64(arg.toLong())
                is Float -> NumberValue.F32(arg)
                is Double -> NumberValue.F64(arg)
                else -> error("Unsupported argument type $arg")
            }
        }
    }
}

private fun throwOnError(error: ChasmError.ExecutionError): Nothing = throw ChasmErrorException(error)
