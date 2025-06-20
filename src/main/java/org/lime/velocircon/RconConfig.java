package org.lime.velocircon;

public record RconConfig(
        boolean enable,
        String host,
        int port,
        String password,
        boolean colors
) {
    public static final RconConfig DEFAULT = new RconConfig(
            false,
            "0.0.0.0",
            25575,
            "PASSWORD",
            true);
}
