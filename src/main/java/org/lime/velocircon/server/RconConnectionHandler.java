package org.lime.velocircon.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.ComponentEncoder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.lime.velocircon.RconConfig;
import org.slf4j.Logger;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

public class RconConnectionHandler extends ChannelInboundHandlerAdapter {
    private static final AttributeKey<AtomicBoolean> AUTH_ATTRIBUTE = AttributeKey.valueOf("rcon.auth");

    private static final int MAX_PAYLOAD_LENGTH = 4096;
    private static final int INFO_PAYLOAD_LENGTH = 10;
    private static final Charset ENCODING = CharsetUtil.UTF_8;

    private final RconServer handler;
    private final Logger logger;
    private final RconConfig config;

    public RconConnectionHandler(RconServer handler, Logger logger, RconConfig config) {
        this.handler = handler;
        this.logger = logger;
        this.config = config;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("New connection from {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        AtomicBoolean authed = ctx.channel().attr(AUTH_ATTRIBUTE).get();
        if (authed == null) {
            authed = new AtomicBoolean(false);
            ctx.channel().attr(AUTH_ATTRIBUTE).set(authed);
        }

        try {
            while (in.readableBytes() >= 4) {
                in.markReaderIndex();
                int length = in.readIntLE();
                if (in.readableBytes() < length) {
                    in.resetReaderIndex();
                    return; // Wait for more data
                }

                int requestId = in.readIntLE();
                int requestType = in.readIntLE();
                int payloadLength = length - INFO_PAYLOAD_LENGTH;

                String payload = "";
                if (payloadLength > 0) {
                    payload = in.readCharSequence(payloadLength, ENCODING).toString();
                }
                in.skipBytes(2); // 2 null bytes

                handlePacket(ctx, requestId, requestType, payload, authed);
            }
        } finally {
            in.release();
        }
    }

    private void handlePacket(ChannelHandlerContext ctx, int requestId, int requestType, String payload, AtomicBoolean authed) {
        switch (requestType) {
            case PacketType.AUTH_REQUEST:
                logger.info("Handling authorization request");
                boolean success = payload != null && !payload.isEmpty() && payload.equals(handler.password());
                sendPacket(ctx, success ? requestId : -1, PacketType.AUTH_RESPONSE, "");
                authed.set(success);
                break;
            case PacketType.COMMAND_REQUEST:
                if (authed.get()) {
                    logger.info("Handling command '{}'", payload);
                    handler.execute(payload)
                            .handle((result, ex) -> {
                                if (ex != null) {
                                    logger.error("Handling request '{}' error: {}", payload, ex.getMessage(), ex);
                                    result = Component.text("Error executing: " + payload + " (" + ex.getMessage() + ")")
                                            .color(NamedTextColor.RED);
                                }
                                String postfix = config.colors()
                                        ? LegacyComponentSerializer.SECTION_CHAR + "r"
                                        : "";
                                ComponentEncoder<Component, String> encoder = config.colors()
                                        ? LegacyComponentSerializer.legacySection()
                                        : PlainTextComponentSerializer.plainText();
                                sendResponse(ctx, requestId, PacketType.COMMAND_RESPONSE, encoder.serialize(result) + postfix);
                                return true;
                            });
                } else {
                    logger.warn("Unauthorized command '{}'", payload);
                    sendPacket(ctx, -1, PacketType.AUTH_RESPONSE, "");
                }
                break;
            default:
                logger.warn("Unknown request type 0x{}", Integer.toHexString(requestType));
                sendResponse(ctx, requestId, PacketType.COMMAND_RESPONSE, "Unknown request 0x" + Integer.toHexString(requestType));
        }
    }

    private void sendPacket(ChannelHandlerContext ctx, int id, int type, String message) {
        byte[] messageBytes = message.getBytes(ENCODING);
        int packetLength = 4 + 4 + messageBytes.length + 2;
        ByteBuf buf = ctx.alloc().buffer(4 + packetLength);
        buf.writeIntLE(packetLength);
        buf.writeIntLE(id);
        buf.writeIntLE(type);
        buf.writeBytes(messageBytes);
        buf.writeByte(0);
        buf.writeByte(0);
        ctx.writeAndFlush(buf);
    }

    private void sendResponse(ChannelHandlerContext ctx, int id, int type, String message) {
        byte[] messageBytes = message.getBytes(ENCODING);
        int maxChunk = MAX_PAYLOAD_LENGTH - INFO_PAYLOAD_LENGTH;
        for (int i = 0; i < messageBytes.length; i += maxChunk) {
            int len = Math.min(maxChunk, messageBytes.length - i);
            String chunk = new String(messageBytes, i, len, ENCODING);
            sendPacket(ctx, id, type, chunk);
        }
        if (messageBytes.length == 0) {
            sendPacket(ctx, id, type, "");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception while parsing input", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Connection from {} closed", ctx.channel().remoteAddress());
    }
}
