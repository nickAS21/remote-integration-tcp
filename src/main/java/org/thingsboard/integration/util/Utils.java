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
package org.thingsboard.integration.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {

    public static final long unsignedBytesToLongRev(byte[] b, int off)  {
        long l = 0;
        l |= b[off + 3] & 0xFF;
        l <<= 8;
        l |= b[off + 2] & 0xFF;
        l <<= 8;
        l |= b[off + 1] & 0xFF;
        l <<= 8;
        l |= b[off] & 0xFF;
        return l;
    }

    public static final int unsignedBytesToIntRev(byte [] buf, int pos) {
        int i = 0;
        i += unsignedByteToInt(buf[pos+1]) << 8;
        i += unsignedByteToInt(buf[pos]) << 0;
        return  i;
    }

    public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    public static byte[] hexStringToByteArray(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

//    public static long getBytesToLong(byte[] bufIn, int order, int off, int len) {
//        long value = 0;
//        byte [] buf = new byte [len];
//        System.arraycopy(bufIn, off, buf, 0, len);
//        if   (buf != null) {
//            if (order == 0) { // обратка, litleEndian
//                for (int i = 0; i < buf.length; i++) {
//                    value += ((long) (buf[i] & 0xff)) << (long) (8 * i);
//                }
//            } else if (order == 1) { // прямая, bigEndian
//                for (int i = 0; i < buf.length; i++) {
//                    value = (value << (long) 8) + ((long) (buf[i] & 0xff));
//                }
//            }
//        }
//        else {
//            value = -1;
//        }
//        return value;
//    }
//
//    public static int bytesToIntLitle(byte[] bytes) {
//        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
//    }
}
