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


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.custom.message.*;

import java.util.*;

@Slf4j
public class TCPSimpleChannelInboundHandler extends SimpleChannelInboundHandler<Object> {
    private final UUID sessionId;
    private String serialNumber;
    private String commandCur;
    private boolean initData = false;
    private int dataPacketLength = 0;
    private int posLast = 0;
    private byte[] msgAllBytes;
    private TCPIntegration TCPIntegration;
    private String typeDevice;

    TCPSimpleChannelInboundHandler(TCPIntegration TCPIntegration) {
        this.sessionId = UUID.randomUUID();
        this.TCPIntegration = TCPIntegration;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        byte[] msgBytes = (byte[]) msg;
        int msgLen = msgBytes.length;
        if (msgLen > 1) {
//            System.out.println("input1: " + Hex.toHexString(msgBytes));

            TCPIntegration chTCPIntegration = this.TCPIntegration;
            this.typeDevice = chTCPIntegration.getTypeDevice();
            /**
             * Start read msg from buffer
             */
            if (!initData) {
                RequestMsg requestMsg = RequestMsgFabrica.getRequestMsg(this.typeDevice);
                requestMsg.startConnect(msgBytes);
                this.msgAllBytes = requestMsg.getMsgAllBytes();
                this.initData = requestMsg.getInitData();
                this.dataPacketLength = requestMsg.getDataPacketLength();
                System.arraycopy(msgBytes, 0, msgAllBytes, posLast, msgLen);
                posLast += msgLen;
                if (msgAllBytes.length > posLast) {
                    initData = true;
                } else {
                    posLast = 0;
                    initData = false;
                }
            }
            /**
             * read msg from buffer next packet
             */
            else {
                System.arraycopy(msgBytes, 0, msgAllBytes, posLast, msgLen);
                posLast += msgLen;
                if (msgAllBytes.length <= posLast) {
                    posLast = 0;
                    initData = false;
                }
            }

            /**
             * analysis msgAllBytes and sent data to UpLink
             */
            if (!initData && msgAllBytes != null && msgAllBytes.length > 0) {
                RequestMsg requestMsg = RequestMsgFabrica.getRequestMsg(this.typeDevice);
                boolean rez = requestMsg.analysisMsg(this.msgAllBytes,  this.serialNumber, chTCPIntegration, this.dataPacketLength);
                if (rez) {
                    this.serialNumber = requestMsg.getSerialNumber();
                    /**
                     * sent response ti device
                     */
                    byte[] commandSend = requestMsg.getCommandSend();
                    if (commandSend != null && commandSend.length > 0) {
                        sentMsgToDivice(ctx, requestMsg.getCommandSend(), msg);
                    }
                    /**
                     * sent data to UpLink
                     */
                    byte[] payload = requestMsg.getPayload();
                    if (payload != null && payload.length > 0) {
                        CustomResponse response = new CustomResponse();
                        chTCPIntegration.process(new CustomIntegrationMsg(Hex.toHexString(payload), response, this.serialNumber, this.commandCur));
                    }
                }

                /**
                 * After Start session if there are requests in the queue for this device from DownLink
                 *  Example: getinfo, getver, getstatus, getgps, getio, ggps, getparam 2004, getparam 2005, readio 21, readio 66
                 */
                if (chTCPIntegration.sentRequestByte.size() > 0 && chTCPIntegration.sentRequestByte.containsKey(this.serialNumber) && chTCPIntegration.sentRequestByte.get(this.serialNumber).size() > 0) {
                    Map<String, byte[]> commands = chTCPIntegration.sentRequestByte.get(this.serialNumber);
                    chTCPIntegration.sentRequestByte.put(this.serialNumber, commands);
                    if (chTCPIntegration.sentRequestByte.get(this.serialNumber).size() > 0) {
                        sentMsgToDiviceFromUpLink(ctx, chTCPIntegration, msg);
                        commands.remove(this.commandCur);
                    }
                }
            }
        }
    }

    private void sentMsgToDiviceFromUpLink(ChannelHandlerContext ctx, TCPIntegration chTCPIntegration, Object msg) {
        Map<String, byte[]> commands = chTCPIntegration.sentRequestByte.get(this.serialNumber);
        commandCur = (String) commands.keySet().toArray()[0];
        byte[] commandSend = commands.get(commandCur);
        sentMsgToDivice(ctx, commandSend, msg);
    }

    private void sentMsgToDivice(ChannelHandlerContext ctx, byte[] commandSend, Object msg) {
        ReferenceCountUtil.retain(msg);
        ctx.writeAndFlush(commandSend);
    }

}