package org.thingsboard.integration.custom.server;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.thingsboard.integration.util.Crc16_IBM;

import java.nio.ByteBuffer;
import java.util.List;

/**
 *Preamble - the packet starts with four zero bytes.
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
 */
@Slf4j
public class SentMsg {

    String [] getCommandList = {"getinfo",
                                "getver",
                                "getstatus",
                                 "getgps",
                                 "getio",
                                 "ggps",
//                                 "cpureset",
                                 "getparam 133",
                                 "getparam 102",
                                 "setparam 133:0",
                                 "setparam 102:2",
                                 "readio 21",
                                 "readio 66"}  ;

    public byte [] getCommandMsg(List<String> commands ) {
//        int packetLength;   //     == 4 bytes  with data size ({from vyte[8] to byte [len - CRC])
        int codecId = 0x0C;   //     == 1 bytes
//        int paramCount;    //      == 1 bytes 				(Количество параметров конфигурации)
        int commandType = 0x05;    //     == 1  (request 05, response 06)
//        int paramValueLength; //   == 4 bytes 	(Length of parameter value (BE byte order).
//        int paramValue; //         == ParamValueLength bytes 		(Parameter value (UTF-8 encoded string)).
//        int paramId ;       //     == 1 bytes 				(Configuration parameter id (BE byte order)).
//        int crc16IBM;       //     == 4 bytes

            // Packet
        int quantity = commands.size();
        int commandSizes = commands.stream().mapToInt(String::length).sum();
        int packetLength =  2 + 1 + 4*quantity + commandSizes + 1;
        int pos = 0;
        int len = 1;
        byte [] bytesPacket = new byte [packetLength];
        bytesPacket [pos] = (byte)codecId;
        pos += len;
        bytesPacket [pos] = (byte)quantity;
        pos += len;
        len = 1;
        bytesPacket [pos] = (byte)commandType;
        pos += len;
        for (String command : commands) {

            len = 4;
            byte[] bytesParamValueLength = ByteBuffer.allocate(4).putInt(command.length()).array();
            System.arraycopy(bytesParamValueLength, 0, bytesPacket, pos, len);
            pos += len;
            len = command.length();
            byte [] commandB = command.getBytes();  // 67 65 74 69 6E 66 6F
            System.arraycopy(commandB, 0, bytesPacket, pos, len);
            pos += len;
        }
        len = 1;
        bytesPacket [pos] = (byte)quantity;
            // CRC
        Crc16_IBM crc16_IBM = new Crc16_IBM( 0xA001, false);
        int crc_16_val= crc16_IBM.calculate ( bytesPacket, 0);
        byte[] bytesCrc_16_val=  ByteBuffer.allocate(4).putInt(crc_16_val).array();
            // All

        int lenSentBB = 4 + 4 + packetLength + 4;
        byte [] bytes = new byte [lenSentBB];
        pos = 4;
        len = 4;
        byte[] bytesPacketLength = ByteBuffer.allocate(4).putInt(packetLength).array();
        System.arraycopy(bytesPacketLength, 0, bytes, pos, len);
        pos += len;
        len =  bytesPacket.length;
        System.arraycopy(bytesPacket, 0, bytes, pos, len);
        pos += len;
        len =4;
        System.arraycopy(bytesCrc_16_val, 0, bytes, pos, len);
        return  bytes;
    }

    public byte [] getCommandMsgOne(String command ) {
//        int packetLength;   //     == 4 bytes  with data size ({from vyte[8] to byte [len - CRC])
        int codecId = 0x0C;   //     == 1 bytes
//        int paramCount;    //      == 1 bytes 				(Количество параметров конфигурации)
        int commandType = 0x05;    //     == 1  (request 05, response 06)
//        int paramValueLength; //   == 4 bytes 	(Length of parameter value (BE byte order).
//        int paramValue; //         == ParamValueLength bytes 		(Parameter value (UTF-8 encoded string)).
//        int paramId ;       //     == 1 bytes 				(Configuration parameter id (BE byte order)).
//        int crc16IBM;       //     == 4 bytes
        // Packet
        log.error("command {}", command);
        int quantity = 1;
        int commandSizes = command.length();
        int packetLength =  3 + 4*quantity + commandSizes + 1;
        int pos = 0;
        int len = 1;
        byte [] bytesPacket = new byte [packetLength];
        bytesPacket [pos] = (byte)codecId;
        pos += len;
        bytesPacket [pos] = (byte)quantity;
        pos += len;
        bytesPacket [pos] = (byte)commandType;
        pos += len;
//        for (String command : commands) {
            len = 4;
            byte[] bytesParamValueLength = ByteBuffer.allocate(4).putInt(command.length()).array();
            System.arraycopy(bytesParamValueLength, 0, bytesPacket, pos, len);
            pos += len;
            len = command.length();
            byte [] commandB = command.getBytes();  // 67 65 74 69 6E 66 6F
            System.arraycopy(commandB, 0, bytesPacket, pos, len);
            pos += len;
//        }
        len = 1;
        bytesPacket [pos] = (byte)quantity;
        // CRC
        log.error("sentB_Old     {}",  Hex.toHexString(bytesPacket));

        Crc16_IBM crc16_IBM = new Crc16_IBM( 0xA001, false);
        int crc_16_val= crc16_IBM.calculate ( bytesPacket, 0);
        byte[] bytesCrc_16_val=  ByteBuffer.allocate(4).putInt(crc_16_val).array();
        // All

        int lenSentBB = 4 + 4 + packetLength + 4;
        byte [] bytes = new byte [lenSentBB];
        pos = 4;
        len = 4;
        byte[] bytesPacketLength = ByteBuffer.allocate(4).putInt(packetLength).array();
        System.arraycopy(bytesPacketLength, 0, bytes, pos, len);
        pos += len;
        len =  bytesPacket.length;
        System.arraycopy(bytesPacket, 0, bytes, pos, len);
        pos += len;
        len =4;
        System.arraycopy(bytesCrc_16_val, 0, bytes, pos, len);
        return  bytes;
    }


    /**
     *
     sent getver :  000000000000000e0c010500000006676574766572010000a4c2
     sent getinfo : 000000000000000f0c010500000007676574696e666f0100004312
     */
    public byte [] getCommandMsgByteOne( String command) {
        byte [] bytesPacket =  hexStringToByteArray(command);
        // CRC
        log.error("sentB_New     {}",  Hex.toHexString(bytesPacket));
        Crc16_IBM crc16_IBM = new Crc16_IBM( 0xA001, false);
        int crc_16_val= crc16_IBM.calculate (bytesPacket, 0);
        byte[] bytesCrc_16_val=  ByteBuffer.allocate(4).putInt(crc_16_val).array();
        byte [] bytes = new byte [bytesPacket.length + 12];
        int pos = 4;
        int len = 4;
        byte[] bytesParamValueLength = ByteBuffer.allocate(4).putInt(bytesPacket.length).array();
        System.arraycopy(bytesParamValueLength, 0, bytes, pos, len);
        pos += len;
        len =  bytesPacket.length;
        System.arraycopy(bytesPacket, 0, bytes, pos, len);
        pos += len;
        len = 4;
        System.arraycopy(bytesCrc_16_val, 0, bytes, pos, len);
//
//        int commandSize = command.length();
//        int packetLength =  3 + 4*quantity + commandSize + 1;
//        int pos = 0;
//        int len = 1;
//        byte [] bytesPacket = new byte [packetLength];
//        bytesPacket [pos] = (byte)codecId;
//        pos += len;
//        bytesPacket [pos] = (byte)quantity;
//        pos += len;
//        bytesPacket [pos] = (byte)commandType;
//        pos += len;
//        len = 4;
//        byte[] bytesParamValueLength = ByteBuffer.allocate(4).putInt(command.length()).array();
//        System.arraycopy(bytesParamValueLength, 0, bytesPacket, pos, len);
//        pos += len;
//        len = command.length();
//        byte [] commandB = command.getBytes();
//        System.arraycopy(commandB, 0, bytesPacket, pos, len);
//        pos += len;
//        len = 1;
//        bytesPacket [pos] = (byte)quantity;
//        // CRC
//        Crc16_IBM crc16_IBM = new Crc16_IBM( 0xA001, false);
//        int crc_16_val= crc16_IBM.calculate ( bytesPacket, 0);
//        byte[] bytesCrc_16_val=  ByteBuffer.allocate(4).putInt(crc_16_val).array();
//        // All
//
//        int lenSentBB = 4 + 4 + packetLength + 4;
//        byte [] bytes = new byte [lenSentBB];
//        pos = 4;
//        len = 4;
//        byte[] bytesPacketLength = ByteBuffer.allocate(4).putInt(packetLength).array();
//        System.arraycopy(bytesPacketLength, 0, bytes, pos, len);
//        pos += len;
//        len =  bytesPacket.length;
//        System.arraycopy(bytesPacket, 0, bytes, pos, len);
//        pos += len;
//        len =4;
//        System.arraycopy(bytesCrc_16_val, 0, bytes, pos, len);
        return  bytes;
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
