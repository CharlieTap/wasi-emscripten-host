/*
 * Copyright 2024, the wasi-emscripten-host project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")

package at.released.weh.wasi.preview1.function

import at.released.weh.filesystem.op.poll.Poll
import at.released.weh.host.EmbedderHost
import at.released.weh.wasi.preview1.WasiPreview1HostFunction
import at.released.weh.wasi.preview1.ext.EventMapper.EVENT_PACKED_SIZE
import at.released.weh.wasi.preview1.ext.EventMapper.writeTo
import at.released.weh.wasi.preview1.ext.SubscriptionMapper
import at.released.weh.wasi.preview1.ext.foldToErrno
import at.released.weh.wasi.preview1.type.Errno
import at.released.weh.wasi.preview1.type.Event
import at.released.weh.wasi.preview1.type.Size
import at.released.weh.wasi.preview1.type.SizeType
import at.released.weh.wasi.preview1.type.Subscription
import at.released.weh.wasm.core.IntWasmPtr
import at.released.weh.wasm.core.WasmPtr
import at.released.weh.wasm.core.memory.MemoryAccess
import at.released.weh.wasm.core.memory.defaultMemoryAccess
import at.released.weh.wasm.core.memory.writeI32
import kotlinx.io.IOException
import at.released.weh.filesystem.op.poll.Event as FileSystemEvent

public class PollOneoffFunctionHandle(
    host: EmbedderHost,
) : WasiPreview1HostFunctionHandle(WasiPreview1HostFunction.POLL_ONEOFF, host) {
    public fun <M> execute(
        memory: M,
        @IntWasmPtr(Subscription::class) inSubscriptionPtr: WasmPtr,
        @IntWasmPtr(Event::class) outEventsPtr: WasmPtr,
        @SizeType subscriptionCount: Size,
        @IntWasmPtr(Int::class) eventsStoredAddr: WasmPtr,
        memoryAccess: MemoryAccess<M> = memory.defaultMemoryAccess(),
    ): Errno = with(memoryAccess) {
        if (subscriptionCount == 0) {
            return Errno.INVAL
        }
        val subscriptions = try {
            SubscriptionMapper.readFileSystemSubscriptions(memory, inSubscriptionPtr, subscriptionCount)
        } catch (ex: Exception) {
            if (ex is IllegalStateException || ex is IllegalArgumentException || ex is IOException) {
                return Errno.INVAL
            } else {
                throw ex
            }
        }

        return host.fileSystem.execute(Poll, Poll(subscriptions))
            .onRight { events: List<FileSystemEvent> ->
                val eventsToWrite = minOf(events.size, subscriptionCount)
                memory.writeI32(eventsStoredAddr, eventsToWrite)
                repeat(eventsToWrite) { index ->
                    events[index].writeTo(memory, outEventsPtr + index * EVENT_PACKED_SIZE)
                }
            }
            .foldToErrno()
    }
}
