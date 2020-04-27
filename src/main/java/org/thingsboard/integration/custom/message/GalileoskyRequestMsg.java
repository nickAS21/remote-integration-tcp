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
import org.thingsboard.integration.util.Crc16_Modbus;
import org.thingsboard.integration.util.Utils;

import java.nio.ByteBuffer;

import static org.thingsboard.integration.util.Utils.hexStringToByteArray;

public class GalileoskyRequestMsg extends RequestMsg {

    int imevOff = 8;
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
             * First Packet:
             * 0114800196021803383634333736303439323333393138 4b23
             * 00    = 01       signatura
             * 01    = 14       length, , 23 bytes length is received
             * 02    = 80       high-order bit  * - Indicator of unsent * data to the archive: * 0 – no; * 1 – yes.  – there are unsent data, in case of masking it (0x80 == 128)
             * 03    = 01       tag01 id       (device type)
             * 04    = 11       tag01 value == 17  (Galileosky GPS/GLONASS 5.0.11)
             * 05    = 02       tag02 id      (firmware version)
             * 06    = DF       tag02 value == 223
             * 07    = 03       tag03 id      (IMEI)
             * 08-22 = 38 36 38 32 30 34 30 30 35 36 34 37 38 33 38     tag03 value == «868204005647838» => len = 15
             * 23-24 = 86 9c    CRC
             *
             * Other
             * Byte     Len    Value
             * 01       1      = 06     signatura   Packet with Garmin FMI from device
             * 01       1      = 08     signatura   Main Packet with Compression
             * 02-03    2      = L      Packet length
             * 04-13    10              Minimal data set 1
             * 14       2-33            Tags list 1
             *
             * L+1-L+2  2               Checksum
             */
            this.initData = false;
//            if (msgBytes.length<30) {
//                this.dataPacketLength = msgBytes[1];
//            }
//            else {
            byte[] dataPacketLengthB = new byte[2];
//            dataPacketLengthB[0] = msgBytes[2];
//            dataPacketLengthB[1] = msgBytes[1];
            dataPacketLengthB[0] = msgBytes[1];
            dataPacketLengthB[1] = msgBytes[2];

//            int arh = dataPacketLengthB[0] & (1 << 7);
            int arh = dataPacketLengthB[1] & (1 << 7);
            System.out.println("arh " + arh);
//            dataPacketLengthB[0] &= ~(1 << 7);
            dataPacketLengthB[1] &= ~(1 << 7);
//            this.dataPacketLength = (dataPacketLengthB[1] & 0xFF | (dataPacketLengthB[0] & 0xFF) << 8);
//            this.dataPacketLength = Utils.getIntFormByte2Rev(dataPacketLengthB, 0);
            this.dataPacketLength = Utils.unsignedBytesToIntRev(dataPacketLengthB, 0);
//            this.dataPacketLength = Utils.bytesToIntLitle(dataPacketLengthB);
            System.out.println("this.dataPacketLength " + this.dataPacketLength);
//            }
            this.msgAllBytes = new byte[dataPacketOff + dataPacketLength + crcLen];
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
    public boolean analysisMsg(byte[] msgAllBytes, String serialNumber, TCPIntegration chTCPIntegration, int dataPacketLength) {
        this.msgAllBytes = msgAllBytes;
        this.serialNumber = serialNumber;
        this.dataPacketLength = dataPacketLength;
        boolean result = false;
        Crc16_Modbus crc16_modbus = new Crc16_Modbus();
        int[] crc_16_val_modbus = crc16_modbus.calculateCRC(msgAllBytes, 0, msgAllBytes.length - 2);
        if (msgAllBytes[msgAllBytes.length - 2] == (byte)crc_16_val_modbus[0] && msgAllBytes[msgAllBytes.length - 1] == (byte)crc_16_val_modbus[1]) {
            byte[] payload = new byte[msgAllBytes.length - 2];
            System.arraycopy(msgAllBytes, 0, payload, 0, payload.length);
            this.payload = payload;
            if (payload [0] == 1) {
                if (this.serialNumber == null) {
                    this.serialNumber = getIdentify(payload);
                }
            }
            result = true;
            /**
             * - after connect and authorization -> Response 0x02, CRC [2]
             */
            this.commandSend = new byte[3];
            this.commandSend[0] = 2;
            this.commandSend[1] = msgAllBytes[msgAllBytes.length - 2];
            this.commandSend[2] = msgAllBytes[msgAllBytes.length - 1];
            System.out.println("this.commandSend: " + Hex.toHexString(this.commandSend));
        }
        return result;
    }

    private String getIdentify(byte [] payload) {
        String identify = null;
        int off = 3;
        int tagId;
        while (identify == null && off < payload.length && off < 10) {
            tagId = payload[off];
            off ++;
            if (tagId == 3){
                byte[] imeiB = new byte[15];
                System.arraycopy(payload, off, imeiB, 0, imeiB.length);
                identify = new String(imeiB);
            }else if (tagId == 4) {
                int identInt = Utils.unsignedBytesToIntRev(payload, off);
                identify = Integer.toString(identInt);
            }
            else {
                off ++;
            }
        }
        return identify;
    }

    String[] getCommandListExample = {
            "HeadPack 1110",    // Ответ: HeadPack = 0000000000000000000000000000000000000000000000000000000000001110b
            "MainPack 1111000",    // Ответ: HeadPack = 0000000000000000000000000000000000000000000000000000000000001110b
            "status",
            "imei",
            "imsi", // код SIM-карты
            "inall",
            "insys",    // Ответ: INSYS: Pow=12438,Vbat=4196,Vant=2921,Vdc=4115,Temper=37
            "RS485",    // Ответ: RS485 100,0;100,1;100,2;100,3;100,4;100,5;100,6;100,7;100,8;100,9;100,10; 100,11;100,12;100,13;100,14;100,15;
            "statall",  // Ответ: StatAll: Dev=1,Ins=2,Outs=7,Mileage=152;
            "EFS 27042012,27042013",  // EFS: Uploading of archive has been scheduled (ДДММГГ[ЧЧ[ММ]])
            "LED 60",   // Ответ: LED:LED=60
    };

    @Override
    public byte[] getCommandMsgByteOne(String command) {
        byte[] bytesPacket = hexStringToByteArray(command);
        // CRC
        Crc16_Modbus crc16_modbus = new Crc16_Modbus();
        int[] crc_16_val = crc16_modbus.calculateCRC(bytesPacket, 0, bytesPacket.length);
        byte[] bytes = new byte[bytesPacket.length + 2];
        System.arraycopy(bytesPacket, 0, bytes, 0, bytesPacket.length);
        bytes[bytesPacket.length] = (byte)crc_16_val[0];
        bytes[bytesPacket.length+1] = (byte)crc_16_val[1];
        return bytes;
    }

    @Override
    public String isResponse (String response) {
       return (response.length() > 52 && response.substring(44, 46).toUpperCase().equals("E0")) ?  response.substring(46, 54) : null;

    }
}
