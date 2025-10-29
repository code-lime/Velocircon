package org.lime.velocircon.permissions;

import com.velocitypowered.api.permission.Tristate;

import java.util.regex.Pattern;

public record RegexPermissionFactory(
        Pattern regex)
        implements PermissionFactory {
    public static RegexPermissionFactory create(String regex) {
        return new RegexPermissionFactory(Pattern.compile(regex));
    }

    public Tristate check(String permission) {
        return regex.matcher(permission).matches()
                ? Tristate.TRUE
                : Tristate.UNDEFINED;
    }
}
