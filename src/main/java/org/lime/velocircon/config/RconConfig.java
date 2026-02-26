package org.lime.velocircon.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class RconConfig {
    public boolean enable = false;
    public String host = "0.0.0.0";
    public int port = 25575;
    public String password = "PASSWORD";
    public boolean colors = true;
    public boolean consoleOutput = true;
    public Permissions permissions = new Permissions();

    @ConfigSerializable
    public static class Permissions {
        public LuckPerms luckPerms = new LuckPerms();
        public Regex regex = new Regex();

        @ConfigSerializable
        public static class LuckPerms {
            public boolean enable = false;
            public String group = "*";
        }
        @ConfigSerializable
        public static class Regex {
            public boolean enable = false;
            public String regex = "minecraft\\.(.*)";
        }
    }
}
