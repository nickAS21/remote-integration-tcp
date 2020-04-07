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

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.util.Crc16_IBM;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Preamble - the packet starts with four zero bytes.
 * Data Size - size is calculated from Codec ID field to the second command or response quantity field.
 * Codec ID - in Codec12 it is always 0x0C.
 * Command/Response Quantity 1 - it is ignored when parsing the message.
 * Type - it can be 0x05 to denote command or 0x06 to denote response.
 * Command/Response Size – command or response length.
 * Command/Response – command or response in HEX.
 * Command/Response Quantity 2 - a byte which defines how many records (commands or responses) is in the packet. This byte will not be parsed but it’s recommended that it should contain same value as Command/Response Quantity 1.
 * CRC-16 – calculated from Codec ID to the Command Quantity 2. CRC (Cyclic Redundancy Check) is an error-detecting code using for detect accidental changes to RAW data. For calculation we are using CRC-16/IBM.
 */

/**
 *    General Codec12 message structure
 *
 * The following diagram shows basic structure of Codec12 messages.
 *
 * Command message structure:
 * 0x00000000 (Preamble) 	Data Size 	Codec ID 	Command Quantity 1 	Type (0x05) 	Command Size 	Command 	Command Quantity 2 	CRC-16
 *    4 bytes 	             4 bytes 	1 byte   	1 byte  	          1 byte 	     4 bytes 	   X bytes 	     1 byte          	4 bytes
 *
 *
 * Response message structure:
 * 0x00000000 (Preamble) 	Data Size 	Codec ID 	Response Quantity 1 	Type (0x06) 	Response Size 	Response 	Response Quantity 2 	CRC-16
 * 4 bytes                	4 bytes 	1 byte   	1 byte  	              1 byte  	    4 bytes       	X bytes    	1 byte              	4 bytes
 *
 * Saving/Sending without time synchronization (ID=107)
 * When this feature is enabled (value = 1), then records can be saved and sent to server
 * without time synchronization.
 */
@Slf4j
public class ResponseMsgUtils {

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

    public byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

}