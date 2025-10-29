package org.lime.velocircon;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class RconConfig {
    public boolean enable = false;
    public String host = "0.0.0.0";
    public int port = 25575;
    public String password = "PASSWORD";
    public boolean colors = true;
    public Permissions permissions = new Permissions();

    @ConfigSerializable
    public static class Permissions {
    }
}
