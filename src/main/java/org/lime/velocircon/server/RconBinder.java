package org.lime.velocircon.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.lime.velocircon.RconConfig;
import org.lime.velocircon.utils.NettyFutureUtils;
import org.slf4j.Logger;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class RconBinder implements Closeable {
    private final NativeTransportType transportType;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final RconServer rconHandler;
    private final Logger logger;

    public RconBinder(RconServer rconHandler, Logger logger) {
        this.transportType = NativeTransportType.bestType();
        this.bossGroup = this.transportType.createEventLoopGroup(NativeTransportType.Type.BOSS);
        this.workerGroup = this.transportType.createEventLoopGroup(NativeTransportType.Type.WORKER);
        this.rconHandler = rconHandler;
        this.logger = logger;
    }

    public CompletableFuture<ChannelGroup> bind(InetSocketAddress address, RconConfig config) {
        ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channelFactory(transportType.serverSocketChannelFactory())
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        channels.add(ch);
                        ch.pipeline().addLast(new RconConnectionHandler(rconHandler, logger, config));
                    }
                })
                .localAddress(address);
        return NettyFutureUtils.toCompletableFuture(bootstrap.bind(), ChannelFuture::channel)
                .handle((channel, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to bind RCON server", ex);
                        return CompletableFuture.<ChannelGroup>failedFuture(ex);
                    } else {
                        logger.info("RCON server bound to {}", channel.localAddress());
                        channels.add(channel);
                        return CompletableFuture.completedFuture(channels);
                    }
                })
                .thenCompose(v -> v);
    }

    public void close() {
        bossGroup.close();
        workerGroup.close();
    }
}
