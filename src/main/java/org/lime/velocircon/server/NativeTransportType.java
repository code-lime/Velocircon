package org.lime.velocircon.server;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NativeTransportType {
    private static final Class<?> transportType;
    private static final Class<? extends Enum> transportType$Type;
    private static final Method transportType_createEventLoopGroup;
    private static final Method transportType_bestType;
    private static final Field transportType_serverSocketChannelFactory;

    static {
        try {
            transportType = Class.forName("com.velocitypowered.proxy.network.TransportType");
            transportType$Type = (Class<? extends Enum>)Class.forName("com.velocitypowered.proxy.network.TransportType$Type");
            transportType_createEventLoopGroup = transportType.getDeclaredMethod("createEventLoopGroup", transportType$Type);
            transportType_bestType = transportType.getDeclaredMethod("bestType");
            transportType_serverSocketChannelFactory = transportType.getDeclaredField("serverSocketChannelFactory");
            transportType_serverSocketChannelFactory.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Object handle;

    NativeTransportType(Object handle) {
        this.handle = handle;
    }

    public EventLoopGroup createEventLoopGroup(final Type type) {
        try {
            return (EventLoopGroup)transportType_createEventLoopGroup.invoke(handle, type.handle);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public ChannelFactory<? extends ServerSocketChannel> serverSocketChannelFactory() {
        try {
            return (ChannelFactory<? extends ServerSocketChannel>)transportType_serverSocketChannelFactory.get(handle);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static NativeTransportType bestType() {
        try {
            return new NativeTransportType(transportType_bestType.invoke(null));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return handle.toString();
    }

    public enum Type {
        BOSS,
        WORKER;

        private final Object handle;

        Type() {
            handle = Enum.valueOf(transportType$Type, name());
        }
        @Override
        public String toString() {
            return handle.toString();
        }
    }
}