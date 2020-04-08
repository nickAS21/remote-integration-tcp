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
package org.thingsboard.integration.custom.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.integration.api.data.*;
import org.thingsboard.integration.custom.client.TCPClient;
import org.thingsboard.integration.custom.message.CustomIntegrationMsg;
import org.thingsboard.integration.custom.message.CustomResponse;
import org.thingsboard.integration.custom.message.ResponseMsgUtils;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TCPIntegration extends AbstractIntegration<CustomIntegrationMsg> {

    private static final int bindPort = 1990;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final long msgGenerationIntervalMs = 60000;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workGroup;
    private Channel serverChannel;
    private TCPClient client1;
    private TCPClient client2;
    public Map<String, Map<String, byte[]>> sentRequestByte;
    private String codecId22 = "16";
    private String codecId22Status = "Requests sent, pending session status";

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        sentRequestByte = new HashMap<>();
        Integration inter = this.configuration;
//        JsonNode configuration = mapper.readTree(params.getConfiguration().getConfiguration().get("configuration").asText());
        try {
            bossGroup = new NioEventLoopGroup();
            workGroup = new NioEventLoopGroup();
            ServerBootstrap bootstrap = new ServerBootstrap();
            TCPIntegration tcpIntegration = this;
            bootstrap.group(bossGroup, workGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast("encoder", new ByteArrayEncoder());
                    socketChannel.pipeline().addLast("decoder", new ByteArrayDecoder());
                    socketChannel.pipeline().addLast(new TCPSimpleChannelInboundHandler(tcpIntegration));
                }
            });
            int port = getBindPort();
            serverChannel = bootstrap.bind(port).sync().channel();
            // for the test with  client
            String client_imev1 = "868204005647838";
            String client_imev2 = "359633100458592";
            client1 = new TCPClient(port, msgGenerationIntervalMs, client_imev1, tcpIntegration.getTypeDevice());
//            client2 = new TCPClient(port, getMsgGeneratorIntervalMs(configuration), client_imev2);

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
        client1.destroy();
        client2.destroy();
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
            response.setResult(doProcessUplink(customIntegrationMsg.getMsg(), customIntegrationMsg.getImev(), customIntegrationMsg.getCommandCur()));
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

    private String doProcessUplink(String msg, String imei, String commandCur) throws Exception {
        byte[] data = mapper.writeValueAsBytes(msg);
        Map<String, String> metadataMap = new HashMap<>(metadataTemplate.getKvMap());
        metadataMap.put("imei", imei);
        metadataMap.put("commandQuantity", "1");
        if (commandCur != null && !commandCur.isEmpty()) {
            metadataMap.put("request", commandCur);
        } else metadataMap.put("request", "1");
        List<UplinkData> uplinkDataList = convertToUplinkDataList(context, data, new UplinkMetaData(getUplinkContentType(), metadataMap));
        if (uplinkDataList != null && !uplinkDataList.isEmpty()) {
            for (UplinkData uplinkData : uplinkDataList) {
                UplinkData uplinkDataResult = UplinkData.builder()
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

    private int getBindPort() {
        int port = 0;
        try {
            JsonNode configuration = mapper.readTree(this.configuration.getConfiguration().get("configuration").asText());
            if (configuration.has("bindPort")) {
                port = configuration.get("bindPort").asInt();
            } else {
                log.warn("Failed to find [port] field in integration config, default value [{}] is used!", this.bindPort);
                port = this.bindPort;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return port;
    }

    public String getTypeDevice() {
        String typeDevice = null;
        try {
            JsonNode configuration = mapper.readTree(this.configuration.getConfiguration().get("configuration").asText());
            if (configuration.has("typeDevice")) {
                typeDevice = configuration.get("typeDevice").asText();
            } else {
                log.warn("Failed to find [typeDevice] field in integration config, default value [{}] is used!", typeDevice);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return typeDevice;
    }

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg msg) {
        TbMsg msgTb = msg.getTbMsg();
        logDownlink(context, msgTb.getType(), msgTb);
        if (downlinkConverter != null) {
            processDownLinkMsg(context, msgTb);
        }
    }

    private void processDownLinkMsg(IntegrationContext context, TbMsg msg) {
        Map<String, String> mdMap = new HashMap<>(metadataTemplate.getKvMap());
        try {
            List<DownlinkData> result = downlinkConverter.convertDownLink(
                    context.getDownlinkConverterContext(),
                    Collections.singletonList(msg),
                    new IntegrationMetaData(mdMap));

            if (!result.isEmpty()) {
                for (DownlinkData downlink : result) {
                    if (downlink.isEmpty()) {
                        continue;
                    }
                    Map<String, String> metadata = downlink.getMetadata();
                    if (!metadata.containsKey("serialNumber")) {
                        throw new RuntimeException("SerialNumber is missing in the downlink metadata!");
                    }
                    String dataStr = new String(downlink.getData(), StandardCharsets.UTF_8);
                    if (dataStr != null && !dataStr.isEmpty()) {
                        ResponseMsgUtils responseMsg = new ResponseMsgUtils();
                        List<byte[]> dataBytes = new ArrayList<>();
                        List<String> datalists = Stream.of(dataStr.split(",")).collect(Collectors.toList());
                        datalists.forEach(datalist -> dataBytes.add(responseMsg.getCommandMsgByteOne(datalist)));
                        List<String> sentValues = Stream.of(metadata.get("payload").split(",")).collect(Collectors.toList());
                        String serialNumber = metadata.get("serialNumber");
                        Map<String, byte[]> sentMsgValue;
                        if (sentRequestByte.containsKey(serialNumber) && sentRequestByte.get(serialNumber).size() > 0) {
                            sentMsgValue = sentRequestByte.get(serialNumber);
                        } else {
                            sentMsgValue = new HashMap<>();
                        }
                        for (int i = 0; i < datalists.size(); i++) {
                            sentMsgValue.put(sentValues.get(i), dataBytes.get(i));
                        }
                        sentRequestByte.put(serialNumber, sentMsgValue);
                        sentUplinkPendingRequests(metadata.get("payload"), metadata.get("serialNumber"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process downLink message", e);
            reportDownlinkError(context, msg, "ERROR", e);
        }
    }

    private void sentUplinkPendingRequests(String payload, String imeiHex) {
        CustomResponse response = new CustomResponse();
        this.process(new CustomIntegrationMsg(this.codecId22 + payload, response, imeiHex, codecId22Status));
    }
}
