package org.lime.velocircon.server;

import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;

public interface RconServer {
    String password();
    CompletableFuture<Component> execute(String command);
}
