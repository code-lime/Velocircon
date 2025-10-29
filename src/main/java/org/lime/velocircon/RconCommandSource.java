package org.lime.velocircon;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.scheduler.Scheduler;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.facet.FacetPointers;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.lime.velocircon.permissions.PermissionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class RconCommandSource
        implements CommandSource {
    private final Pointers pointers = CommandSource.super.pointers().toBuilder()
            .withDynamic(PermissionChecker.POINTER, this::getPermissionChecker)
            .withStatic(FacetPointers.TYPE, FacetPointers.Type.CONSOLE)
            .build();

    private final ConcurrentLinkedQueue<Component> lines = new ConcurrentLinkedQueue<>();

    private final Object plugin;
    private final Scheduler scheduler;
    private final Collection<PermissionFactory> permissionFactories;
    private final long flushMs;
    private final int flushWaitCount;

    public RconCommandSource(
            Object plugin,
            Scheduler scheduler,
            Collection<PermissionFactory> permissionFactories,
            long flushMs,
            int flushWaitCount) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.permissionFactories = permissionFactories;
        this.flushMs = flushMs;
        this.flushWaitCount = flushWaitCount;
    }

    @Override
    public void sendMessage(@NotNull Identity identity, @NotNull Component message, @NotNull MessageType messageType) {
        lines.add(message);
    }

    @Override
    public @NotNull Tristate getPermissionValue(@NotNull String permission) {
        if (permissionFactories.isEmpty())
            return Tristate.TRUE;
        Tristate current = Tristate.UNDEFINED;
        for (var permissionFactory : permissionFactories) {
            var value = permissionFactory.check(permission);
            switch (value) {
                case FALSE -> {
                    return Tristate.FALSE;
                }
                case TRUE -> current = Tristate.TRUE;
            }
        }
        return current;
    }

    @Override
    public @NotNull Pointers pointers() {
        return pointers;
    }

    public CompletableFuture<List<Component>> outputAsync() {
        CompletableFuture<List<Component>> result = new CompletableFuture<>();
        List<Component> messages = new ArrayList<>();
        Runnable poller = new Runnable() {
            private int wait = flushWaitCount;
            @Override
            public void run() {
                Component line;
                int count = 0;
                while ((line = lines.poll()) != null) {
                    messages.add(line);
                    count++;
                }
                wait = count > 0 ? flushWaitCount : (wait - 1);
                if (wait > 0) {
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
