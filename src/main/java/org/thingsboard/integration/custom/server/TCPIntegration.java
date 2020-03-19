/**
 * Copyright © 2020-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.fasterxml.jackson.databind.node.TextNode;
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
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.custom.client.TCPClient;
import org.thingsboard.integration.custom.message.CustomIntegrationMsg;
import org.thingsboard.integration.custom.message.CustomResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Slf4j
public class TCPIntegration extends AbstractIntegration<CustomIntegrationMsg> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int bindPort = 1994;
    private static final long msgGenerationIntervalMs = 60000;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workGroup;
    private Channel serverChannel;
    private TCPClient client1;
    private TCPClient client2;
    public Map<String, HashSet<String>> sentRequest;

//    private String deviceName;
//
//    private boolean initData = false;
//    private int dataLength = 0;
//    private int posLast = 0;
//    byte[] dataAVL;
//    String imeiHex;


    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        sentRequest = new HashMap<>();
        JsonNode configuration = mapper.readTree(params.getConfiguration().getConfiguration().get("configuration").asText());
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
            int port = getBindPort(configuration);
            serverChannel = bootstrap.bind(port).sync().channel();
            // for the test
            String client_imev1 = "359633100458591";
            String client_imev2 = "359633100458592";
            client1 = new TCPClient(port, getMsgGeneratorIntervalMs(configuration), client_imev1);
            client2 = new TCPClient(port, getMsgGeneratorIntervalMs(configuration), client_imev2);

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
//            log.error("process  {}", (customIntegrationMsg.getImev()));
            response.setResult(doProcessUplink(customIntegrationMsg.getMsg(), customIntegrationMsg.getImev(), customIntegrationMsg.getCommands()));
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

    private String doProcessUplink(String msg, String imei, List<String> commands) throws Exception {
        byte[] data = mapper.writeValueAsBytes(msg);
        Map<String, String> metadataMap = new HashMap<>(metadataTemplate.getKvMap());
        metadataMap.put("imei", imei);
        metadataMap.put("commandQuantity", String.valueOf(commands.size()));
        if (commands.size() > 0) {
            String key = "command";
            for (int i = 0; i < commands.size(); i++) {
                String key_i = key + i;
                metadataMap.put(key_i, commands.get(i));
            }

        }
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


    /**
     * 2020-03-19 16:08:14,231 [grpc-default-executor-1] ERROR o.t.i.custom.server.TCPIntegration -  --------------------------- Downlink message: DefaultIntegrationDownlinkMsg(tenantId=a5e10260-5e21-11ea-9e70-8b648d7f643d, integrationId=null, tbMsg=TbMsg(id=12d17790-69eb-11ea-a26b-3fd0b6941ed2, type=ATTRIBUTES_UPDATED, originator=dfd89f60-6221-11ea-8ca0-011cd5ae8bc0, metaData=TbMsgMetaData(data={originatorName=FMB920_359633100458592, scope=SERVER_SCOPE, userName=tenant@thingsboard.org, userId=af8b5fe0-5e21-11ea-9e70-8b648d7f643d, originatorType=teltonik}), dataType=JSON, data={"payload":"ver25"}, transactionData=TbMsgTransactionData(transactionId=12d17790-69eb-11ea-a26b-3fd0b6941ed2, originatorId=dfd89f60-6221-11ea-8ca0-011cd5ae8bc0), ruleChainId=a63def70-5e21-11ea-9e70-8b648d7f643d, ruleNodeId=null, clusterPartition=0))
     *
     * @param msg
     */

    @Override
    public void onDownlinkMsg(IntegrationDownlinkMsg msg) {
        log.error(" --------------------------- Downlink message: {}", msg);
        try {
            String sentMsg =  mapper.readTree(msg.getTbMsg().getData()).get("payload").asText();
            String imei =  mapper.readTree(msg.getTbMsg().getMetaData().getData().get("cs_serialNumber")).asText();
            HashSet commands = (sentRequest.containsKey(imei)) ? sentRequest.get(imei) : new HashSet();
            commands.add(sentMsg);
            sentRequest.put(imei, commands);
            log.error ("sentRequest {}", sentRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
