package org.lime.velocircon;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.scheduler.Scheduler;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.facet.FacetPointers;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class RconCommandSource implements ConsoleCommandSource {
    private final PermissionFunction permissionFunction = PermissionFunction.ALWAYS_TRUE;
    private final Pointers pointers = ConsoleCommandSource.super.pointers().toBuilder()
            .withDynamic(PermissionChecker.POINTER, this::getPermissionChecker)
            .withStatic(FacetPointers.TYPE, FacetPointers.Type.CONSOLE)
            .build();

    private final ConcurrentLinkedQueue<Component> lines = new ConcurrentLinkedQueue<>();

    private final Object plugin;
    private final Scheduler scheduler;
    private final long flushMs;

    public RconCommandSource(Object plugin, Scheduler scheduler, long flushMs) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.flushMs = flushMs;
    }

    @Override
    public void sendMessage(@NotNull Identity identity, @NotNull Component message, @NotNull MessageType messageType) {
        lines.add(message);
    }

    @Override
    public @NotNull Tristate getPermissionValue(@NotNull String permission) {
        return this.permissionFunction.getPermissionValue(permission);
    }

    @Override
    public @NotNull Pointers pointers() {
        return pointers;
    }

    public CompletableFuture<List<Component>> outputAsync() {
        CompletableFuture<List<Component>> result = new CompletableFuture<>();
        List<Component> messages = new ArrayList<>();
        Runnable poller = new Runnable() {
            private boolean isFirst = true;
            @Override
            public void run() {
                Component line;
                int count = 0;
                while ((line = lines.poll()) != null) {
                    messages.add(line);
                    count++;
                }
                if (isFirst || count > 0) {
                    isFirst = false;
                    scheduler.buildTask(plugin, this)
                            .delay(flushMs, TimeUnit.MILLISECONDS)
                            .schedule();
                } else {
                    result.complete(messages);
                }
            }
        };
        poller.run();
        return result;
    }
}
