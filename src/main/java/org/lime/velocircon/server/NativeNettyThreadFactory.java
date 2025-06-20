/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lime.velocircon.server;

import com.google.common.base.Preconditions;
import io.netty.util.concurrent.FastThreadLocalThread;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NativeNettyThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger();
    private final String nameFormat;

    public NativeNettyThreadFactory(String nameFormat) {
        this.nameFormat = Preconditions.checkNotNull(nameFormat, "nameFormat");
    }

    @Override
    public Thread newThread(@NotNull Runnable runnable) {
        String name = String.format(nameFormat, threadNumber.getAndIncrement());
        return new FastThreadLocalThread(runnable, name);
    }
}
