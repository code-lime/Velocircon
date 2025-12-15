package org.lime.velocircon.sources;

import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.scheduler.Scheduler;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;
import org.lime.velocircon.permissions.PermissionFactory;

import java.util.Collection;

public class PermissibleRconCommandSource
        extends BaseRconCommandSource {
    private final Collection<PermissionFactory> permissionFactories;

    public PermissibleRconCommandSource(
            String command,
            Object plugin,
            Scheduler scheduler,
            Collection<PermissionFactory> permissionFactories,
            long flushMs,
            int flushWaitCount,
            boolean consoleOutput,
            ComponentLogger componentLogger) {
        super(command, plugin, scheduler, flushMs, flushWaitCount, consoleOutput, componentLogger);
        this.permissionFactories = permissionFactories;
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
}
