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

import com.google.common.primitives.Ints;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.util.Crc16_Modbus;

import java.nio.ByteBuffer;
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

    public TCPClient(int port, long msgGenerationIntervalMs, String client_imev, String typeDevice) {
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
            if (typeDevice.equals("teltonika")) {
                startGeneratorTeltonika();
            } else if (typeDevice.equals("galileosky")) {
                startGenerator();
            }
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

    private byte[] generateFirstPacket() {
        String payload = "011780011102df03383638323034303035363437383338043200869C";
        int signatura = 1;
//        int dataPacketLength = 23;
        int dataPacketLength = 7;
        int sizePacketlen = 2;
        int headerConnectLen = 1;
        int crcLen = 2;
        int dataToArchive = 128;
        int typeUnitId = 1;
        int typeUnitValue = 17;
        int firmwareVersionId = 2;
        int firmwareVersionValue = 223;
        int IMEI_Id = 3;
        int deviceNumberId = 4;
        int deviceNumberValue = 50;
        int deviceNumberValueRev = Integer.reverseBytes(deviceNumberValue);
//        byte[] bytes = ByteBuffer.allocate(2).putInt(deviceNumberValueRev).array();
        byte[] deviceNumberValueArray = Ints.toByteArray(deviceNumberValueRev);

        byte[] msgAllBytes = new byte[headerConnectLen + sizePacketlen + dataPacketLength + crcLen];
        int off = 0;
        int len = 1;
        msgAllBytes[off] = (byte) signatura;
        msgAllBytes[off += len] = (byte) dataPacketLength;
        msgAllBytes[off += len] = (byte) dataToArchive;
        msgAllBytes[off += len] = (byte) typeUnitId;
        msgAllBytes[off += len] = (byte) typeUnitValue;
        msgAllBytes[off += len] = (byte) firmwareVersionId;
        msgAllBytes[off += len] = (byte) firmwareVersionValue;

//        msgAllBytes[off += len] = (byte) IMEI_Id;
//        byte[] imevBB = client_imev.getBytes();
////        len = imevBB.length;
//        System.arraycopy(imevBB, 0, msgAllBytes, off += len, imevBB.length);
//        off += imevBB.length;
        off += len;
        len = 1;
        msgAllBytes[off] = (byte) deviceNumberId;
        msgAllBytes[off += len] = deviceNumberValueArray[0];
        msgAllBytes[off += len] = deviceNumberValueArray[1];
        byte[] crcB = new byte[msgAllBytes.length - crcLen];
        System.arraycopy(msgAllBytes, 0, crcB, 0, crcB.length);


        Crc16_Modbus crc16_modbus = new Crc16_Modbus();
        int[] crc_16_val_modbus = crc16_modbus.calculateCRC(msgAllBytes, 0, msgAllBytes.length - 2);
//        String crc_16_val_modbus_HexO = Integer.toHexString(crc_16_val_modbus [0]); // 86
//        String crc_16_val_modbus_HexL = Integer.toHexString(crc_16_val_modbus[1] ); //9C
        msgAllBytes[msgAllBytes.length - 2] = (byte) crc_16_val_modbus[0];
        msgAllBytes[msgAllBytes.length - 1] = (byte) crc_16_val_modbus[1];
        String msgAllBytesHex = Hex.toHexString(msgAllBytes);
        if (payload.toLowerCase().equals(msgAllBytesHex.toLowerCase())) {
            System.out.println(payload);
            System.out.println(msgAllBytesHex);
        }
        return msgAllBytes;
    }

    private void startGeneratorTeltonika() {
        this.scheduledExecutorService.scheduleAtFixedRate(() ->
                clientChannel.writeAndFlush(generateImevByte()), 0, this.msgGenerationIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void startGenerator() {
        this.scheduledExecutorService.scheduleAtFixedRate(() ->
                clientChannel.writeAndFlush(generateFirstPacket()), 0, this.msgGenerationIntervalMs, TimeUnit.MILLISECONDS);
    }

    private String generateData() {
        int firstV = generateValue(10, 40);
        int secondV = generateValue(0, 100);
        int thirdV = generateValue(0, 100);
        return firstV + "," + secondV + "," + thirdV;
    }

    private int generateValue(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Max value must be greater than min value!");
        }
        return random.nextInt((max - min) + 1) + min;
    }

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
