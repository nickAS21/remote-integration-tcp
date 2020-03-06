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
package org.thingsboard.integration.custom.basic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.custom.client.CustomClient;
import org.thingsboard.integration.custom.message.CustomIntegrationMsg;
import org.thingsboard.integration.custom.message.CustomResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CustomIntegration extends AbstractIntegration<CustomIntegrationMsg> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int bindPort = 1994;
    private static final long msgGenerationIntervalMs = 5000;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workGroup;
    private Channel serverChannel;
    private CustomClient client;
    private String deviceName;

    private boolean initData = false;
    private int dataLength = 0;
    private int posLast = 0;
    byte [] dataAVL;




    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        JsonNode configuration = mapper.readTree(params.getConfiguration().getConfiguration().get("configuration").asText());
        try {
            bossGroup = new NioEventLoopGroup();
            workGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
//                    socketChannel.pipeline().addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
//                    socketChannel.pipeline().addLast("frameEncoder", new LengthFieldPrepender(4));
                    socketChannel.pipeline().addLast("encoder", new ByteArrayEncoder());
                    socketChannel.pipeline().addLast("decoder", new ByteArrayDecoder());
                    socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<Object>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                            byte[] msgBytes = (byte[]) msg;
                            String msgHex = Hex.toHexString(msgBytes);
                            if (msgBytes.length > 1 && msgBytes[0] == 0 && msgBytes[1] == 0xF) {
                                byte[] bb = {0x01};
                                ctx.writeAndFlush(bb);
                            } else {
                                int msgLen = msgBytes.length;
//                                int numberOfData1 = 0;
//                                dataLength = 0;
//                                int codecID;
//                                int numberOfData2 = 0;
//                                int crc_16 = 0;
                                if (msgBytes.length > 4 && (msgBytes[0] == 0 && msgBytes[1] == 0 && msgBytes[2] == 0 && msgBytes[3] == 0)) {
                                    initData = false;
                                    byte[] msgBytesLen = new byte[4];
                                    System.arraycopy(msgBytes, 4, msgBytesLen, 0, 4);
                                    dataLength = Integer.parseInt(Hex.toHexString(msgBytesLen), 16);
                                    dataAVL = new byte [dataLength + 12];
                                    System.arraycopy(msgBytes, 0, dataAVL, posLast, msgLen);
                                    posLast += msgLen;
//                                    codecID = msgBytes[8];
//                                    numberOfData1 = msgBytes[9];

                                    log.error("dataLength {}", dataLength);
//                                    log.error("codecID {}", codecID);
//                                    log.error("numberOfData1 {}", numberOfData1);
//                                    byte[] bb = {0x11};
//                                    ctx.writeAndFlush(numberOfData1);
                                    if (dataLength > msgLen) {
                                        initData = true;
                                    }
                                    else {
                                        initData = false;
                                    }
                                } else if (initData){
                                    System.arraycopy(msgBytes, 0, dataAVL, posLast, msgLen);
                                    posLast = 0;
                                    initData = false;
                                }
                                log.error("Length All {}", msgBytes.length);
                                log.error("Length Data {}", dataLength);
                                log.error("Server received the message: Hex {}", msgHex);
                            }
                            if (!initData && dataAVL != null && dataAVL.length > 0) {
                                int codecID = dataAVL[8];
                                int numberOfData1 = dataAVL[9];
                                int numberOfData2 = dataAVL[dataAVL.length - 5];
                                byte[] msgBytesLen = new byte[4];
                                System.arraycopy(dataAVL, dataAVL.length - 4, msgBytesLen, 0, 4);
                                int crc_16 = Integer.parseInt(Hex.toHexString(msgBytesLen), 16);
                                log.error("dataLength   {}", dataLength );
                                log.error("codecID  {}", codecID);
                                log.error("numberOfData1  {}", numberOfData1);
                                log.error("numberOfData2  {}", numberOfData2);
                                log.error("crc_16  {}", crc_16);
                                String payloadHex = Hex.toHexString(dataAVL);
                                log.error("payloadHex  {}", payloadHex);
                                if (numberOfData1 == numberOfData2) {   // !!! crc !!!
                                    ctx.writeAndFlush(numberOfData1);
                                    CustomResponse response = new CustomResponse();
                                    process(new CustomIntegrationMsg(payloadHex, response));
                                }
                                dataLength = 0;
                                initData = false;
                            }
                                // test
                            String test = "Hello to ThingsBoard! My name is [Device B]";
                            String testIn = new String(msgBytes);
                            if (test.equals(testIn)) {
                                String testOut = "Hello from ThingsBoard!";
                                byte[] bbOut = testOut.getBytes();
                                ctx.writeAndFlush(bbOut);
                            }

                        }
                    });
                }
            });
            int port = getBindPort(configuration);
            serverChannel = bootstrap.bind(port).sync().channel();
            client = new CustomClient(port, getMsgGeneratorIntervalMs(configuration));
        } catch (Exception e) {
            log.error("Failed to init TCP server!", e);
            throw new RuntimeException();
        }
    }

    protected String getUplinkContentType() {
        return "TEXT";
    }

    @Override
    public void destroy() {
        client.destroy();
        try {
            serverChannel.close().sync();
        } catch (Exception e) {
            log.error("Failed to close the channel!", e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    @Override
    public void process(CustomIntegrationMsg customIntegrationMsg) {
        CustomResponse response = customIntegrationMsg.getResponse();
        if (!this.configuration.isEnabled()) {
            response.setResult("Integration is disabled");
            return;
        }
        String status = "OK";
        Exception exception = null;
        try {
            response.setResult(doProcess(customIntegrationMsg.getMsg()));
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            exception = e;
            status = "ERROR";
            response.setResult(status);
        }
        if (status.equals("ERROR")) {
            integrationStatistics.incErrorsOccurred();
        }
        if (configuration.isDebugMode()) {
            try {
                persistDebug(context, "Uplink", getUplinkContentType(), customIntegrationMsg.getMsg().toString(), status, exception);
            } catch (Exception e) {
                log.warn("Failed to persist debug message!", e);
            }
        }
    }

    private String doProcess(String msg) throws Exception {
        byte[] data = mapper.writeValueAsBytes(msg);
        Map<String, String> metadataMap = new HashMap<>(metadataTemplate.getKvMap());
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, data, new UplinkMetaData(getUplinkContentType(), metadataMap));
        if (uplinkDataList != null && !uplinkDataList.isEmpty()) {
            for (UplinkData uplinkData : uplinkDataList) {
                UplinkData uplinkDataResult = UplinkData.builder()
//                        .deviceName(deviceName)
                        .deviceName(uplinkData.getDeviceName())
                        .deviceType(uplinkData.getDeviceType())
                        .telemetry(uplinkData.getTelemetry())
                        .attributesUpdate(uplinkData.getAttributesUpdate())
                        .customerName(uplinkData.getCustomerName())
                        .build();
                processUplinkData(context, uplinkDataResult);
            }
            return "OK";
        }
        return "No Content";
    }

    private int getBindPort(JsonNode configuration) {
        int port;
        if (configuration.has("port")) {
            port = configuration.get("port").asInt();
        } else {
            log.warn("Failed to find [port] field in integration config, default value [{}] is used!", bindPort);
            port = bindPort;
        }
        return port;
    }

    private long getMsgGeneratorIntervalMs(JsonNode configuration) {
        long msgIntervalMs;
        if (configuration.has("msgGenerationIntervalMs")) {
            msgIntervalMs = configuration.get("msgGenerationIntervalMs").asLong();
        } else {
            log.warn("Failed to find [msgGenerationIntervalMs] field in integration config, default value [{}] is used!", msgGenerationIntervalMs);
            msgIntervalMs = msgGenerationIntervalMs;
        }
        return msgIntervalMs;
    }

}
