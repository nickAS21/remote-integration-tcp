package org.thingsboard.integration.custom.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.custom.message.CustomIntegrationMsg;
import org.thingsboard.integration.custom.message.CustomResponse;
import org.thingsboard.integration.util.CRC16;

import java.util.UUID;

@Slf4j
public class TCPSimpleChannelInboundHandler extends SimpleChannelInboundHandler<Object> {
    private final UUID sessionId;
    private String imeiHex;
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
//            log.error("sessionId + msgBytes {}", sessionId + " " + Hex.toHexString(msgBytes));
            byte[] imeiB = new byte[msgBytes.length - 2];
            System.arraycopy(msgBytes, 2, imeiB, 0, imeiB.length);
            this.imeiHex = new String(imeiB);
            byte[] bb = {0x01};
            ctx.writeAndFlush(bb);
        } else {
            int msgLen = msgBytes.length;
            if (msgBytes.length > 4 && (msgBytes[0] == 0 && msgBytes[1] == 0 && msgBytes[2] == 0 && msgBytes[3] == 0)) {
//                log.error("sessionId + msgBytes {}", sessionId + " " + Hex.toHexString(msgBytes));
                initData = false;
                byte[] msgBytesLen = new byte[4];
                System.arraycopy(msgBytes, 4, msgBytesLen, 0, 4);
                dataLength = Integer.parseInt(Hex.toHexString(msgBytesLen), 16);
                dataAVL = new byte[dataLength + 12];
                System.arraycopy(msgBytes, 0, dataAVL, posLast, msgLen);
                posLast += msgLen;
                if (dataLength > msgLen) {
                    initData = true;
                } else {
                    initData = false;
                }
            } else if (initData) {
                System.arraycopy(msgBytes, 0, dataAVL, posLast, msgLen);
                posLast = 0;
                initData = false;
            }
        }
        if (!initData && dataAVL != null && dataAVL.length > 0) {
            int numberOfData1 = dataAVL[9];
            int numberOfData2 = dataAVL[dataAVL.length - 5];
            byte[] msgBytesLen = new byte[4];
            System.arraycopy(dataAVL, dataAVL.length - 4, msgBytesLen, 0, 4);
            int crc_16 = Integer.parseInt(Hex.toHexString(msgBytesLen), 16);
            CRC16 crc16 = new CRC16();
            byte[] bytesCRC = new byte[dataLength];
            System.arraycopy(dataAVL, 8, bytesCRC, 0, dataLength);
            int crc_16_val = crc16.getValue(bytesCRC);
            if (numberOfData1 == numberOfData2 && crc_16 == crc_16_val) {
                byte[] bb =  {(byte) numberOfData1};
                ctx.writeAndFlush(bb);
                log.error("sessionId + imeiHex + payloadHex  {}", (sessionId + " " + imeiHex + " " + Hex.toHexString(dataAVL)));
                CustomResponse response = new CustomResponse();
                byte[] payload = new byte[bytesCRC.length-1];
                System.arraycopy(bytesCRC, 0, payload, 0, bytesCRC.length-1);
                chTCPIntegration.process(new CustomIntegrationMsg(Hex.toHexString(payload), response, this.imeiHex));
                log.error("chTCPIntegration.process  {}", this.imeiHex);
            }
            dataLength = 0;
            initData = false;
        }
        // test
//        String test = "Hello to ThingsBoard! My name is [Device B]";
//        String testIn = new String(msgBytes);
//        if (test.equals(testIn)) {
//            String testOut = "Hello from ThingsBoard!";
//            log.error("testIn  {}", testIn);
//            byte[] bytesCRC = testOut.getBytes();
//            byte[] bbOut = testOut.getBytes();
//            ctx.writeAndFlush(bbOut);
//            imeiHex = "Device B";
//            CustomResponse response = new CustomResponse();
//            chTCPIntegration.process(new CustomIntegrationMsg(Hex.toHexString(bytesCRC), response));
//        }

    }

}