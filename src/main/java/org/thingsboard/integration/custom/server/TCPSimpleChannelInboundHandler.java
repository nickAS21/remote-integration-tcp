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
                    log.error("sessionId + msgBytes {}", sessionId + " " + Hex.toHexString(msgBytes));
        if (msgBytes.length > 1 && msgBytes[0] == 0 && msgBytes[1] == 0xF) {
            byte[] imeiB = new byte[msgBytes.length - 2];
            System.arraycopy(msgBytes, 2, imeiB, 0, imeiB.length);
            this.imeiHex = new String(imeiB);
//            byte[] bb = {0x01};
//            ctx.writeAndFlush(bb);
//            int paramId = 100;
//            byte [] sentData = {0x03};
            int paramId = 1;
//            byte [] sentData = {0x73, 0x65, 0x74, 0x64, 0x69, 0x67, 0x6f, 0x75, 0x74, 0x20, 0x31, 0x20, 0x36, 0x30};    // s  e  t  d  i  g  o  u  t     1     6 0
            byte [] sentData = {0x67, 0x65, 0x74, 0x76, 0x65, 0x72};    // g   e t  v  e  r
//            EventLoopGroup-4-3] ERROR o.t.i.c.s.TCPSimpleChannelInboundHandler - sessionId + msgBytes fcff7682-475e-407d-a705-5167f98d6b65 000f333539363333313030343538353930
//            2020-03-17 19:40:13,339 [nioEventLoopGroup-4-3] ERROR o.t.i.c.s.TCPSimpleChannelInboundHandler - sessionId + msgBytes fcff7682-475e-407d-a705-5167f98d6b65 000000000000009f0c0106000000975665723a30332e32352e31345f3035204750533a41584e5f352e31305f333333332048773a464d42393230204d6f643a313320494d45493a33353936333331303034353835393020496e69743a313937302d312d3120303a3020557074696d653a38383832204d41433a303031453432424430364645205350433a312830292041584c3a31204f42443a3020424c3a312e372042543a34010000543b
//            2020-03-17 19:40:13,340 [nioEventLoopGroup-4-3] ERROR o.t.i.c.s.TCPSimpleChannelInboundHandler - sessionId + payloadHe+ sent  fcff7682-475e-407d-a705-5167f98d6b65 000000000000009f0c0106000000975665723a30332e32352e31345f3035204750533a41584e5f352e31305f333333332048773a464d42393230204d6f643a313320494d45493a33353936333331303034353835393020496e69743a313937302d312d3120303a3020557074696d653a38383832204d41433a303031453432424430364645205350433a312830292041584c3a31204f42443a3020424c3a312e372042543a34010000543b 1
//            2020-03-17 19:42:15,751 [nioEventLoopGroup-4-4] ERROR o.t.i.c.s.TCPSimpleChannelInboundHandler - sessionId + msgBytes a36a39e1-c1f8-44c9-974a-22f472ecfd21 000f333539363333313030343538353930
//            2020-03-17 19:42:16,213 [nioEventLoopGroup-4-4] ERROR o.t.i.c.s.TCPSimpleChannelInboundHandler - sessionId + msgBytes a36a39e1-c1f8-44c9-974a-22f472ecfd21 000000000000009f0c0106000000975665723a30332e32352e31345f3035204750533a41584e5f352e31305f333333332048773a464d42393230204d6f643a313320494d45493a33353936333331303034353835393020496e69743a313937302d312d3120303a3020557074696d653a39303034204d41433a303031453432424430364645205350433a312830292041584c3a31204f42443a3020424c3a312e372042543a34010000035d
//            2020-03-17 19:42:16,214 [nioEventLoopGroup-4-4] ERROR o.t.i.c.s.TCPSimpleChannelInboundHandler - sessionId + payloadHe+ sent  a36a39e1-c1f8-44c9-974a-22f472ecfd21 000000000000009f0c0106000000975665723a30332e32352e31345f3035204750533a41584e5f352e31305f333333332048773a464d42393230204d6f643a313320494d45493a33353936333331303034353835393020496e69743a313937302d312d3120303a3020557074696d653a39303034204d41433a303031453432424430364645205350433a312830292041584c3a31204f42443a3020424c3a312e372042543a34010000035d 1

            int cmdType = 5;
            int codec = 12;
            SentMsg sentMsg = new SentMsg();
            byte [] sentBB = sentMsg.getNewMsg(1,  codec, cmdType,  sentData,  paramId);
            ctx.writeAndFlush(sentBB);

        } else {
            int msgLen = msgBytes.length;
//            log.error("sessionId + msgBytes {}", sessionId + " " + Hex.toHexString(msgBytes) + " " + posLast + " " + msgLen);
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
                if (bb[0] > 1) {
                    ctx.writeAndFlush(bb);
                }
//                ctx.writeAndFlush(bb1);
                log.error("sessionId + payloadHe+ sent  {}", (sessionId + " "  + Hex.toHexString(dataAVL)) + " " + bb[0]);
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