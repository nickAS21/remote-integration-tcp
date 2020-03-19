package org.thingsboard.integration.custom.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.custom.message.CustomIntegrationMsg;
import org.thingsboard.integration.custom.message.CustomResponse;
import org.thingsboard.integration.util.CRC16;
import org.thingsboard.integration.util.Crc16_IBM;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class TCPSimpleChannelInboundHandler extends SimpleChannelInboundHandler<Object> {
    private final UUID sessionId;
    private String imeiHex;
    private List<String> commands;
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
//        log.error("sessionId + msgBytes {}:{}", sessionId, Hex.toHexString(msgBytes));
        if (msgBytes.length > 1 && msgBytes[0] == 0 && msgBytes[1] == 0xF) {
            byte[] imeiB = new byte[msgBytes.length - 2];
            System.arraycopy(msgBytes, 2, imeiB, 0, imeiB.length);
            this.imeiHex = new String(imeiB);
            if (chTCPIntegration.sentRequest.size() > 0 && chTCPIntegration.sentRequest.containsKey(this.imeiHex)) {
//            log.error("sessionId + sentBytes {}:{}", sessionId, Hex.toHexString(bb));
                Set sentMsgs = chTCPIntegration.sentRequest.get(this.imeiHex);
                SentMsg sentMsg = new SentMsg();
                sentMsgs.forEach(command ->  ctx.writeAndFlush(sentMsg.getCommandMsgOne((String) command)));

//                this.commands = new ArrayList<String>();
//                String setCommand = sentMsg.getCommandList[4];
//            String setCommand = sentMsg.getCommandList[8] + " 133";
//            String setCommand = sentMsg.getCommandList[9] + " 133:1";
//            log.error("setCommand {} : {}", sessionId, setCommand);
//                commands.add(setCommand);
//                byte[] sentBB = sentMsg.getCommandMsg(commands);
//                ctx.writeAndFlush(sentBB);
//            log.error("sessionId + sentBytes {}:{}", sessionId, Hex.toHexString(sentBB));

//            this.commands = new ArrayList<String>();
//            setCommand = sentMsg.getCommandList[9] + " 102:4";
//            log.error("setCommand {} : {}", sessionId, setCommand);
//            commands.add(setCommand);
//            sentBB = sentMsg.getCommandMsg(commands);
//            ctx.writeAndFlush(sentBB);
//            log.error("sessionId + sentBytes {}:{}", sessionId, Hex.toHexString(sentBB));
            } else {
                byte[] bb = {0x01};
                ctx.writeAndFlush(bb);
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
            CRC16 crc16 = new CRC16();
            int crc_16_val = crc16.getValue(bytesCRC);
            Crc16_IBM crc16_IBM = new Crc16_IBM(0xA001, false);
            int crc_16_val_IBM = crc16_IBM.calculate(bytesCRC, 0);
//            if (numberOfData1 == numberOfData2 && crc_16 == crc_16_val) {
            if (numberOfData1 == numberOfData2 && crc_16 == crc_16_val_IBM) {
                if (bytesCRC[0] == 8) {
                    byte[] bb = new byte[1];
                    bb[0] = (byte) numberOfData1;
                    ctx.writeAndFlush(bb);
                }
//                log.error("sessionId + payloadHe+ sent  {} : {}", sessionId, Hex.toHexString(dataAVL));
                CustomResponse response = new CustomResponse();
                byte[] payload = new byte[bytesCRC.length - 1];
                System.arraycopy(bytesCRC, 0, payload, 0, bytesCRC.length - 1);
                chTCPIntegration.process(new CustomIntegrationMsg(Hex.toHexString(payload), response, this.imeiHex, this.commands));
            }
            dataLength = 0;
            initData = false;
        }


    }

}