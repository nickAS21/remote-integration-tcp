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


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.custom.message.CustomIntegrationMsg;
import org.thingsboard.integration.custom.message.CustomResponse;
import org.thingsboard.integration.util.Crc16_IBM;

import java.util.*;

@Slf4j
public class TCPSimpleChannelInboundHandler extends SimpleChannelInboundHandler<Object> {
    private final UUID sessionId;
    private String imeiHex;
    private String commandCur;
    private boolean initData = false;
    private int dataLength = 0;
    private int posLast = 0;
    private byte[] dataAVL;
    private TCPIntegration TCPIntegration;

    TCPSimpleChannelInboundHandler(TCPIntegration TCPIntegration) {
        this.sessionId = UUID.randomUUID();
        this.TCPIntegration = TCPIntegration;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        byte[] msgBytes = (byte[]) msg;
        TCPIntegration chTCPIntegration = this.TCPIntegration;
        if (msgBytes.length > 1 && msgBytes[0] == 0 && msgBytes[1] == 0xF) {
            byte[] imeiB = new byte[msgBytes.length - 2];
            System.arraycopy(msgBytes, 2, imeiB, 0, imeiB.length);
            this.imeiHex = new String(imeiB);
            if (chTCPIntegration.sentRequestByte.size() > 0 && chTCPIntegration.sentRequestByte.containsKey(this.imeiHex) && chTCPIntegration.sentRequestByte.get(this.imeiHex).size() > 0) {
                sentMsgToDivice (ctx, chTCPIntegration, msg);
            } else {
                byte[] bb = {0x01};
                ReferenceCountUtil.retain(msg);
                ctx.writeAndFlush(bb);
                commandCur = null;
            }
        } else {
            int msgLen = msgBytes.length;
            if (msgBytes.length > 4 && (msgBytes[0] == 0 && msgBytes[1] == 0 && msgBytes[2] == 0 && msgBytes[3] == 0) && !initData) {
                initData = false;
                byte[] msgBytesLen = new byte[4];
                System.arraycopy(msgBytes, 4, msgBytesLen, 0, 4);
                dataLength = Integer.parseInt(Hex.toHexString(msgBytesLen), 16);
                dataAVL = new byte[dataLength + 12];
                System.arraycopy(msgBytes, 0, dataAVL, posLast, msgLen);
                posLast += msgLen;
                if (dataLength > posLast) {
                    initData = true;
                } else {
                    posLast = 0;
                    initData = false;
                }
            } else if (initData) {
                System.arraycopy(msgBytes, 0, dataAVL, posLast, msgLen);
                posLast += msgLen;
                if (dataLength <= posLast) {
                    posLast = 0;
                    initData = false;
                }
            }
        }
        if (!initData && dataAVL != null && dataAVL.length > 0) {
            int numberOfData1 = dataAVL[9];
            int numberOfData2 = dataAVL[dataAVL.length - 5];
            byte[] msgBytesLen = new byte[4];
            System.arraycopy(dataAVL, dataAVL.length - 4, msgBytesLen, 0, 4);
            int crc_16 = Integer.parseInt(Hex.toHexString(msgBytesLen), 16);
            byte[] bytesCRC = new byte[dataLength];
            System.arraycopy(dataAVL, 8, bytesCRC, 0, dataLength);
             Crc16_IBM crc16_IBM = new Crc16_IBM(0xA001, false);
            int crc_16_val_IBM = crc16_IBM.calculate(bytesCRC, 0);
            if (numberOfData1 == numberOfData2 && crc_16 == crc_16_val_IBM) {
                if (bytesCRC[0] == 8) {
                    byte[] bb = new byte[1];
                    bb[0] = (byte) numberOfData1;
                    ctx.writeAndFlush(bb);
                }
                CustomResponse response = new CustomResponse();
                byte[] payload = new byte[bytesCRC.length - 1];
                System.arraycopy(bytesCRC, 0, payload, 0, bytesCRC.length - 1);
                chTCPIntegration.process(new CustomIntegrationMsg(Hex.toHexString(payload), response, this.imeiHex, this.commandCur));
                if (chTCPIntegration.sentRequestByte.size() > 0 && chTCPIntegration.sentRequestByte.containsKey(this.imeiHex) && chTCPIntegration.sentRequestByte.get(this.imeiHex).size() > 0) {
                    Map<String, byte[]> commands = chTCPIntegration.sentRequestByte.get(this.imeiHex);
                    commands.remove(this.commandCur);
                    chTCPIntegration.sentRequestByte.put(this.imeiHex, commands);
                    if (chTCPIntegration.sentRequestByte.get(this.imeiHex).size() > 0) {
                        sentMsgToDivice(ctx, chTCPIntegration, msg);
                    }
                }
            }
            dataLength = 0;
            initData = false;
        }
    }

    private  void sentMsgToDivice (ChannelHandlerContext ctx, TCPIntegration chTCPIntegration, Object msg) {
        Map<String, byte []> commands = chTCPIntegration.sentRequestByte.get(this.imeiHex);
        commandCur = (String)commands.keySet().toArray()[0];
        byte [] commandSend = commands.get(commandCur);
        ReferenceCountUtil.retain(msg);
        ctx.writeAndFlush(commandSend);
    }

}