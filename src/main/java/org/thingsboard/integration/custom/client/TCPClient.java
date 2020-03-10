/**
 * Copyright © 2020-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.integration.custom.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TCPClient {

    public final ScheduledExecutorService scheduledExecutorService;
    private final NioEventLoopGroup workGroup;
    private final Random random;
    public final long msgGenerationIntervalMs;
    private String client_imev;
    public Channel clientChannel;

    public TCPClient(int port, long msgGenerationIntervalMs, String client_imev) {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.workGroup = new NioEventLoopGroup();
        this.random = new Random();
        this.msgGenerationIntervalMs = msgGenerationIntervalMs;
        this.client_imev = client_imev;
        try {
            TCPClient tcpClient = this;
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(this.workGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    socketChannel.pipeline().addLast("encoder", new ByteArrayEncoder());
                    socketChannel.pipeline().addLast("decoder", new ByteArrayDecoder());
                    socketChannel.pipeline().addLast(new TCPClientSimpleChannelInboundHandler(tcpClient, client_imev));
                }
            });
            clientChannel = bootstrap.connect("localhost", port).sync().channel();
//            byte [] client_imevB = client_imev.getBytes();
            clientChannel.writeAndFlush(generateImevByte());
        } catch (Exception e) {
            log.error("Failed to init TCP client!", e);
            throw new RuntimeException();
        }
    }

    private byte[] generateImevByte() {
        byte imev[] = new byte[17];
        byte[] imevBB = client_imev.getBytes();
        byte[] imevH = new byte[]{0x00, 0x0F};
        System.arraycopy(imevH, 0, imev, 0, 2);
        System.arraycopy(imevBB, 0, imev, 2, 15);
        return imev;
    }

//    private void startGenerator() {
//        this.scheduledExecutorService.scheduleAtFixedRate(() ->
////                clientChannel.writeAndFlush(generateData()), 0, this.msgGenerationIntervalMs, TimeUnit.MILLISECONDS);
//                clientChannel.writeAndFlush(generateImevByte()), 0, this.msgGenerationIntervalMs, TimeUnit.MILLISECONDS);
//    }

//    private String generateData() {
//        int firstV = generateValue(10, 40);
//        int secondV = generateValue(0, 100);
//        int thirdV = generateValue(0, 100);
//        return firstV + "," + secondV + "," + thirdV;
//    }

//    private byte [] generateImevByte() {
//        byte imev [] = new byte [17];
//
//        return new byte[]{0x00, 0x0F};
//    }

//    private int generateValue(int min, int max) {
//        if (min >= max) {
//            throw new IllegalArgumentException("Max value must be greater than min value!");
//        }
//        return random.nextInt((max - min) + 1) + min;
//    }

    public void destroy() {
        if (this.scheduledExecutorService != null) {
            this.scheduledExecutorService.shutdownNow();
        }
        try {
            clientChannel.close().sync();
        } catch (Exception e) {
            log.error("Failed to close the channel!", e);
        } finally {
            this.workGroup.shutdownGracefully();
        }
    }

}
