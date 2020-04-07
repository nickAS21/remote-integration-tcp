package org.thingsboard.integration.custom.message;

import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.custom.server.TCPIntegration;

public class GalileoRequestMsg extends RequestMsg {

    int headerConnectLen = 1;
    int dataPacketOff = 3;
    int crcLen = 2;

    /**
     * init:
     * byte_01 -> signature == 0x02; signature == 0x01 - FirstPacket
     * byte_02_03 - CRC == CRC_16
     *  FirstPacket:
     * byte_01 -> signature == 0x01;
     * byte_2_3 -> Packet length
     * byte_4...n -> data
     * - byte_4 - Id Tag 1
     * - byte_5 - Tag 1 data
     * byte_02_03 - CRC == CRC_16
     * @param msgBytes
     */
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
            if (msgBytes[0] != 0x2) {

            }
            else if  (msgBytes[0] != 0x1) {
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

    /**
     *
     * @param msgAllBytes
     * @param serialNumber
     * @param chTCPIntegration
     * @param dataPacketLength
     * @return
     */
    @Override
    public  boolean analysisMsg (byte[] msgAllBytes, String serialNumber, TCPIntegration chTCPIntegration, int dataPacketLength){
        boolean result = false;

        return result;
    }

    /**
     * First Packet:
     * 01    = 0х01     signatura
     * 02-03 = 17 80    length, high-order bit – there are unsent data, in case of masking it, 23 bytes length is received
     * 04    = 01       tag01 id       (device type)
     * 05    = 11       tag01 value == 17  (Galileosky GPS/GLONASS 5.0.11)
     * 06    = 02       tag02 id      (firmware version)
     * 07    = DF       tag02 value == 223
     * 08    = 03       tag03 id      (IMEI)
     * 09-23 = 38 36 38 32 30 34 30 30 35 36 34 37 38 33 38     tag03 value == «868204005647838» => len = 15
     * 24    = 04       tag04 id      (device number, can be set in settings)
     * 25-26 = 32 00    tag04 value == 50
     * 27-28 = 86 9c    CRC
     */

}
