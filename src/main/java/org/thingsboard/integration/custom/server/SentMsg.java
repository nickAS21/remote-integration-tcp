package org.thingsboard.integration.custom.server;

import org.thingsboard.integration.util.Crc16_IBM;

import java.nio.ByteBuffer;

public class SentMsg {

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

    public byte [] getNewMsg (int paramCount,  int codec, int commandType, byte[] paramValue,  int paramId ) {
//        int packetLength;   //     == 4 bytes  with data size ({from vyte[8] to byte [len - CRC])
//        int codec = 0x08;   //     == 1 bytes
//        int paramCount;    //      == 1 bytes 				(Количество параметров конфигурации)
//        int commandType;    //     == 1  (request 05, response 06)
//        int paramValueLength; //   == 4 bytes 	(Length of parameter value (BE byte order).
//        int paramValue; //         == ParamValueLength bytes 		(Parameter value (UTF-8 encoded string)).
//        int paramId ;       //     == 1 bytes 				(Configuration parameter id (BE byte order)).
//        int crc16IBM;       //     == 4 bytes

            // Packet
        int paramValueLength = paramValue.length;
        int packetLength =  3 + 4*paramCount + paramValueLength + 1*paramCount;
        int pos = 0;
        int len = 1;
        byte [] bytesPacket = new byte [packetLength];
        bytesPacket [pos] = (byte)codec;
        pos += len;
        bytesPacket [pos] = (byte)paramCount;
        pos += len;
        bytesPacket [pos] = (byte)commandType;
        pos += len;
        len = 4;
        byte[] bytesParamValueLength = ByteBuffer.allocate(4).putInt(paramValueLength).array();
        System.arraycopy(bytesParamValueLength, 0, bytesPacket, pos, len);
        pos += len;
        len = paramValue.length;
        System.arraycopy(paramValue, 0, bytesPacket, pos, len);
        pos += len;
        len = 1;
        bytesPacket [pos] = (byte)paramId;
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
}
