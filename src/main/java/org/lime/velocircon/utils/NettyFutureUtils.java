package org.lime.velocircon.utils;

import io.netty.util.concurrent.Future;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class NettyFutureUtils {
    public static CompletableFuture<Void> toCompletableFuture(Future<?> nettyFuture) {
        CompletableFuture<Void> completable = new CompletableFuture<>();
        nettyFuture.addListener(f -> {
            if (f.isSuccess()) completable.complete(null);
            else completable.completeExceptionally(f.cause());
        });
        return completable;
    }
    public static <F extends Future<?>, T>CompletableFuture<T> toCompletableFuture(F nettyFuture, Function<F, T> reader) {
        CompletableFuture<T> completable = new CompletableFuture<>();
        nettyFuture.addListener(f -> {
            if (f.isSuccess()) completable.complete(reader.apply(nettyFuture));
            else completable.completeExceptionally(f.cause());
        });
        return completable;
    }
}
