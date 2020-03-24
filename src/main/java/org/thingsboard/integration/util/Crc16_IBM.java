package org.thingsboard.integration.util;

public class Crc16_IBM {

    // Generator polynom codes (the bits are in reverse order and the bit for x^16 is omitted):
    public static final int stdPoly = 0xA001;  // standard CRC-16 x^16 + x^15 + x^2 + 1 (CRC-16-IBM)
    public static final int stdRPoly = 0xC002;  // standard reverse x^16 + x^14 + x + 1 (CRC-16-IBM)
    public static final int ccittPoly = 0x8408;  // CCITT/SDLC/HDLC x^16 + x^12 + x^5 + 1 (CRC-16-CCITT)
    // The initial CRC value is usually 0xFFFF and the result is complemented.
    public static final int ccittRPoly = 0x8810;  // CCITT reverse x^16 + x^11 + x^4 + 1 (CRC-16-CCITT)
    public static final int lrcPoly = 0x8000;  // LRCC-16 x^16 + 1

    private short[] crcTable;
    private boolean bitOrder;

    /**
     * Creates a Crc16 instance and generates the internal lookup table;
     *
     * @param poly     a bit mask of the generator polynomial, in reversed bit order (LSB first).
     * @param bitOrder false for LSB first (which is normally used),
     *                 or true for MSB first (which is used e.g. for XMODEM/CRC).
     */
    public Crc16_IBM(int poly, boolean bitOrder) {
        this.bitOrder = bitOrder;
        if (bitOrder) {
            crcTable = genCrc16TableMsbFirst(reverseInt16(poly));
        } else {
            crcTable = genCrc16TableLsbFirst(poly);
        }
    }

    /**
     * Calculates the CRC code over a data block.
     */
    public int calculate(byte[] data, int initialCrcValue) {
        if (bitOrder) {
            return calculateCrcMsbFirst(data, initialCrcValue);
        } else {
            return calculateCrcLsbFirst(data, initialCrcValue);
        }
    }

    private int calculateCrcLsbFirst(byte[] data, int initialCrcValue) {
        int crc = initialCrcValue;
        for (int p = 0; p < data.length; p++) {
            crc = (crc >> 8) ^ (crcTable[(crc & 0xFF) ^ (data[p] & 0xFF)] & 0xFFFF);
        }
        return crc;
    }

    private int calculateCrcMsbFirst(byte[] data, int initialCrcValue) {
        int crc = initialCrcValue;
        for (int p = 0; p < data.length; p++) {
            crc = ((crc << 8) & 0xFF00) ^ (crcTable[(crc >> 8) ^ (data[p] & 0xFF)] & 0xFFFF);
        }
        return crc;
    }

    private static short[] genCrc16TableLsbFirst(int poly) {
        short[] table = new short[256];
        for (int x = 0; x < 256; x++) {
            int w = x;
            for (int i = 0; i < 8; i++) {
                if ((w & 1) != 0) {
                    w = (w >> 1) ^ poly;
                } else {
                    w = w >> 1;
                }
            }
            table[x] = (short) w;
        }
        return table;
    }

    private static short[] genCrc16TableMsbFirst(int poly) {
        short[] table = new short[256];
        for (int x = 0; x < 256; x++) {
            int w = x << 8;
            for (int i = 0; i < 8; i++) {
                if ((w & 0x8000) != 0) {
                    w = (w << 1) ^ poly;
                } else {
                    w = w << 1;
                }
            }
            table[x] = (short) w;
        }
        return table;
    }

    // Reverses the bits of a 16 bit integer.
    private static int reverseInt16(int i) {
        i = (i & 0x5555) << 1 | (i >>> 1) & 0x5555;
        i = (i & 0x3333) << 2 | (i >>> 2) & 0x3333;
        i = (i & 0x0F0F) << 4 | (i >>> 4) & 0x0F0F;
        i = (i & 0x00FF) << 8 | (i >>> 8);
        return i;
    }

}

