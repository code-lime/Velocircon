package org.lime.velocircon.permissions;

import com.velocitypowered.api.permission.Tristate;
import org.lime.velocircon.RconConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface PermissionFactory {
    Tristate check(String permission);

    static Collection<PermissionFactory> load(RconConfig config, Logger logger) {
        List<PermissionFactory> permissionFactories = new ArrayList<>();
        var regex = config.permissions.regex;
        if (regex.enable) {
            permissionFactories.add(RegexPermissionFactory.create(regex.regex));
            logger.info("Regex permission provider loaded");
        } else {
            logger.info("Regex permission provider disabled");
        }
        return permissionFactories;
    }
}
