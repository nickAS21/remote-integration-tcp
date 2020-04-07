package org.thingsboard.integration.custom.message;


import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.custom.server.TCPIntegration;
import org.thingsboard.integration.util.Crc16_IBM;

public class TeltonikaRequestMsg extends RequestMsg {

    int headerConnectLen = 2;
    int dataPacketOff = 8;
    int crcLen = 4;

    @Override
    public void startConnect(byte[] msgBytes) {
        this.msgBytes = msgBytes;
//        int msgLen = super.msgBytes.length;
        /**
         * Start read msg from buffer
         */
        if (!initData) {
            /**
             * find the length of the Data
             * - connect and authorization
             */
            if (msgBytes[1] != 0x0) {
                this.initData = false;
                byte[] msgBytesLen = new byte[2];
                System.arraycopy(msgBytes, 0, msgBytesLen, 0, headerConnectLen);
                this.dataPacketLength = Integer.parseInt(Hex.toHexString(msgBytesLen), 16);
                this.msgAllBytes = new byte[headerConnectLen + dataPacketLength];
            }
            /**
             * - Response after connect and authorization
             */
            else if (msgBytes.length > dataPacketOff && (msgBytes[0] == 0 && msgBytes[1] == 0 && msgBytes[2] == 0 && msgBytes[3] == 0)) {
                this.initData = false;
                byte[] msgBytesLen = new byte[4];
                System.arraycopy(msgBytes, (dataPacketOff - 4), msgBytesLen, 0, 4);
                this.dataPacketLength = Integer.parseInt(Hex.toHexString(msgBytesLen), 16);
                this.msgAllBytes = new byte[dataPacketLength + dataPacketOff + crcLen];
            }
        }
    }

    @Override
    public  boolean analysisMsg (byte[] msgAllBytes,  String serialNumber,  TCPIntegration chTCPIntegration, int dataPacketLength){
        this.msgAllBytes = msgAllBytes;
        this.serialNumber = serialNumber;
        this.dataPacketLength = dataPacketLength;

        /**
         * - after connect and authorization -> Response type Protocol: 1 - TCP; 0 - UDP.
         */
        boolean result = false;
        if (msgAllBytes[1] != 0x0) {
            byte[] imeiB = new byte[msgAllBytes.length - headerConnectLen];
            System.arraycopy(this.msgAllBytes, headerConnectLen, imeiB, 0, imeiB.length);
            this.serialNumber = new String(imeiB);
            result = true;
            /**
             *  If not request from DownLink -> commandSend == 0x01 (type Protocol: 1 - TCP)
             */
            if (chTCPIntegration.sentRequestByte.size() == 0 || !chTCPIntegration.sentRequestByte.containsKey(this.serialNumber) || chTCPIntegration.sentRequestByte.get(this.serialNumber).size() == 0) {
                /**
                 * - after connect and authorization -> Response type Protocol: 1 - TCP; 0 - UDP. and Response data I/O
                 */
               this.commandSend = new byte[]{0x01};
            }
        }

        /**
         * analysis of conditions: CRC
         * - next after connect and authorization if  (numberOfData1 == numberOfData2 and CRC - ok: sent all msg to UpLink 00 00 00 00 00 00 04 c5 08 07
         */
        else if (msgAllBytes.length > 4 && (msgAllBytes[0] == 0 && msgAllBytes[1] == 0 && msgAllBytes[2] == 0 && msgAllBytes[3] == 0)) {
            int numberOfData1 = msgAllBytes[dataPacketOff + 1];
            int numberOfData2 = msgAllBytes[msgAllBytes.length - 5];
            byte[] msgBytesLen = new byte[crcLen];
            System.arraycopy(msgAllBytes, msgAllBytes.length - crcLen, msgBytesLen, 0, msgBytesLen.length);
            int crc_16 = Integer.parseInt(Hex.toHexString(msgBytesLen), 16);
            byte[] payload = new byte[dataPacketLength];
            System.arraycopy(msgAllBytes, 8, payload, 0, dataPacketLength);
            Crc16_IBM crc16_IBM = new Crc16_IBM(0xA001, false);
            int crc_16_val_IBM = crc16_IBM.calculate(payload, 0);
            if (numberOfData1 == numberOfData2 && crc_16 == crc_16_val_IBM) {
                result = true;
                this.payload = payload;
                /**
                 * if Codec ID == 8 sent to device Number of Data
                 */
                if (payload[0] == 8) {
                    this.commandSend = new byte[]{(byte) numberOfData1};
                }
            }
        }
        return result;
    }
}
