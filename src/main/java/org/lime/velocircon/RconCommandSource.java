package org.lime.velocircon;

import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.facet.FacetPointers;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RconCommandSource implements ConsoleCommandSource {
    private final PermissionFunction permissionFunction = PermissionFunction.ALWAYS_TRUE;
    private final Pointers pointers = ConsoleCommandSource.super.pointers().toBuilder()
            .withDynamic(PermissionChecker.POINTER, this::getPermissionChecker)
            .withStatic(FacetPointers.TYPE, FacetPointers.Type.CONSOLE)
            .build();

    private final ConcurrentLinkedQueue<Component> lines = new ConcurrentLinkedQueue<>();

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

    public Iterable<Component> pollAll() {
        return new Iterable<Component>() {
            @Override
            public @NotNull Iterator<Component> iterator() {
                return new Iterator<Component>() {
                    @Nullable Component next = null;
                    @Override
                    public boolean hasNext() {
                        return (next = lines.poll()) != null;
                    }
                    @Override
                    public Component next() {
                        Component current = next;
                        if (current == null)
                            throw new NoSuchElementException();
                        return current;
                    }
                };
            }
        };
    }
}
