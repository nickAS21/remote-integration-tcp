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
package org.thingsboard.integration.custom.message;


import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.custom.server.TCPIntegration;
import org.thingsboard.integration.util.Crc16_IBM;

import java.nio.ByteBuffer;

import static org.thingsboard.integration.util.Utils.hexStringToByteArray;

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


    String[] getCommandListExample = {
            "getinfo",
            "getver",
            "getstatus",
            "getgps",
            "getio",
            "ggps",
            "cpureset",
            "getparam 133",
            "getparam 102",
            "setparam 133:0",
            "setparam 102:2",
            "readio 21",
            "readio 66",
            "getparam 2004",                        // Server settings domen: my.org.ua;  his.thingsboard.io or ifconfig.co
            "setparam 2004:his.thingsboard.io",
            "setparam 2004:my.org.ua",
            "getparam 2005",                        //  Server settings port: 1994
            "getparam 2006"                         //  Server settings pototokol: TCP - 0, UDP - 1
    };

    /**
     *
     sent getver :  000000000000000e0c010500000006676574766572010000a4c2
     sent getinfo : 000000000000000f0c010500000007676574696e666f0100004312
     */
    @Override
    public byte[] getCommandMsgByteOne(String command) {
        byte[] bytesPacket = hexStringToByteArray(command);
        // CRC
        Crc16_IBM crc16_IBM = new Crc16_IBM(0xA001, false);
        int crc_16_val = crc16_IBM.calculate(bytesPacket, 0);
        byte[] bytesCrc_16_val = ByteBuffer.allocate(4).putInt(crc_16_val).array();
        byte[] bytes = new byte[bytesPacket.length + 12];
        int pos = 4;
        int len = 4;
        byte[] bytesParamValueLength = ByteBuffer.allocate(4).putInt(bytesPacket.length).array();
        System.arraycopy(bytesParamValueLength, 0, bytes, pos, len);
        pos += len;
        len = bytesPacket.length;
        System.arraycopy(bytesPacket, 0, bytes, pos, len);
        pos += len;
        len = 4;
        System.arraycopy(bytesCrc_16_val, 0, bytes, pos, len);
        return bytes;
    }
}
