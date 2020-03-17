package org.thingsboard.integration.custom.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.custom.message.CustomIntegrationMsg;
import org.thingsboard.integration.custom.message.CustomResponse;
import org.thingsboard.integration.util.CRC16;
import org.thingsboard.integration.util.Crc16_IBM;

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
//        byte[] bbSent = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x13, 0x0C, 0x01, 0x05,
//                0x00, 0x00, 0x00, 0x0B, 0x73, 0x65, 0x74, 0x64, 0x69, 0x67, 0x6F, 0x75, 0x74, 0x20, 0x31, 0x01, 0x00, 0x00, (byte) 0x87, (byte) 0xA2};

        byte[] bbSent = {0x00, 0x00, 0x00, 0x00,
                         0x00, 0x00, 0x00, 0x16,
//                         0x0C,
                         0x08,
                         0x01,
                         0x05,
                         0x00, 0x00, 0x00, 0x0E,
                         0x73, 0x65, 0x74, 0x64, 0x69, 0x67, 0x6f, 0x75, 0x74, 0x20, 0x31, 0x20, 0x36, 0x30,
                         0x01,
//                         0x00, 0x00, (byte)0xB3, 0x3E};
                         0x00, 0x00, (byte)0x40, 0x7B};

//        byte [] sentData = {0x73, 0x65, 0x74, 0x64, 0x69, 0x67, 0x6f, 0x75, 0x74, 0x20, 0x31, 0x20, 0x36, 0x30};
        int paramId = 100;
        byte [] sentData = {0x03};
        SentMsg sentMsg = new SentMsg();
        byte [] sentBB = sentMsg.getNewMsg(1,  5,  sentData,  paramId);
        if (msgBytes.length > 1 && msgBytes[0] == 0 && msgBytes[1] == 0xF) {
            log.error("sessionId + msgBytes {}", sessionId + " " + Hex.toHexString(msgBytes));
            byte[] imeiB = new byte[msgBytes.length - 2];
            System.arraycopy(msgBytes, 2, imeiB, 0, imeiB.length);
            this.imeiHex = new String(imeiB);
//            byte[] bb = {0x01};
//            ctx.writeAndFlush(bb);
            ctx.writeAndFlush(sentBB);
        } else {
            int msgLen = msgBytes.length;
            log.error("sessionId + msgBytes {}", sessionId + " " + Hex.toHexString(msgBytes) + " " + posLast + " " + msgLen);
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
            CRC16 crc16 = new CRC16();
            byte[] bytesCRC = new byte[dataLength];
            System.arraycopy(dataAVL, 8, bytesCRC, 0, dataLength);
            int crc_16_val = crc16.getValue(bytesCRC);
            if (numberOfData1 == numberOfData2 && crc_16 == crc_16_val) {
                byte[] bb =  new byte [1];
//                String bbStr = "0707070707";
//                byte[] bb1 =  bbStr.getBytes();
                bb[0]  = (byte )numberOfData1 ;
                ctx.writeAndFlush(bb);
//                ctx.writeAndFlush(bb1);
                log.error("sessionId + payloadHex  {}", (sessionId + " "  + Hex.toHexString(dataAVL)));
                CustomResponse response = new CustomResponse();
                byte[] payload = new byte[bytesCRC.length-1];
                System.arraycopy(bytesCRC, 0, payload, 0, bytesCRC.length-1);
                chTCPIntegration.process(new CustomIntegrationMsg(Hex.toHexString(payload), response, this.imeiHex));
//                log.error("chTCPIntegration.process  {}", this.imeiHex);
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