package org.lime.velocircon.permissions;

import com.velocitypowered.api.permission.Tristate;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.query.QueryOptions;

import java.util.Optional;

public record LuckPermsPermissionFactory(
        LuckPerms luckPerms,
        String groupName)
        implements PermissionFactory {
    public static LuckPermsPermissionFactory create(String groupName) {
        return new LuckPermsPermissionFactory(LuckPermsProvider.get(), groupName);
    }

    public Tristate check(String permission) {
        if (groupName == null || groupName.equals("*") || groupName.isBlank())
            return Tristate.TRUE;
        return Optional.ofNullable(luckPerms.getGroupManager().getGroup(groupName))
                .map(v -> v.getCachedData().getPermissionData(QueryOptions.nonContextual()).checkPermission(permission))
                .map(v -> switch (v) {
                    case TRUE -> Tristate.TRUE;
                    case FALSE -> Tristate.FALSE;
                    default -> Tristate.UNDEFINED;
                })
                .orElse(Tristate.UNDEFINED);
    }
}
