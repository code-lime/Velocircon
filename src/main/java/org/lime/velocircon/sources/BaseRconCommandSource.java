package org.lime.velocircon.sources;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.scheduler.Scheduler;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public abstract class BaseRconCommandSource
        implements CommandSource {
    private final Pointers pointers = CommandSource.super.pointers().toBuilder()
            .withDynamic(PermissionChecker.POINTER, this::getPermissionChecker)
            .build();

    private final ConcurrentLinkedQueue<Component> lines = new ConcurrentLinkedQueue<>();

    private final String command;
    private final Object plugin;
    private final Scheduler scheduler;
    private final long flushMs;
    private final int flushWaitCount;
    private final boolean consoleOutput;
    private final ComponentLogger componentLogger;

    public BaseRconCommandSource(
            String command,
            Object plugin,
            Scheduler scheduler,
            long flushMs,
            int flushWaitCount,
            boolean consoleOutput,
            ComponentLogger componentLogger) {
        this.command = command;
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.flushMs = flushMs;
        this.flushWaitCount = flushWaitCount;
        this.consoleOutput = consoleOutput;
        this.componentLogger = componentLogger;
    }

    @Override
    public void sendMessage(final @NonNull Component message) {
        lines.add(message);
        if (consoleOutput)
            componentLogger.info(Component.empty()
                    .append(Component.text("[/"+command+"] "))
                    .append(message));
    }
    @Override
    public void sendMessage(final @NonNull Component message, final ChatType.@NonNull Bound boundChatType) {
        sendMessage(message);
    }
    @Override
    public void sendMessage(final @NonNull SignedMessage signedMessage, final ChatType.@NonNull Bound boundChatType) {
        var message = signedMessage.unsignedContent();
        if (message == null)
            message = Component.text(signedMessage.message());
        sendMessage(message);
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
