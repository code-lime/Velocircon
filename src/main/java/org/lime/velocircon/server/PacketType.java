package org.lime.velocircon.server;

public interface PacketType {
    int AUTH_REQUEST = 3;
    int AUTH_RESPONSE = 2;
    int COMMAND_REQUEST = 2;
    int COMMAND_RESPONSE = 0;
}
