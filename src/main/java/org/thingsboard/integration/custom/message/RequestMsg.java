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

import lombok.Data;
import org.thingsboard.integration.custom.server.TCPIntegration;

import static org.thingsboard.integration.util.Utils.hexStringToByteArray;

@Data
public abstract class RequestMsg {

    byte[] msgBytes;
    byte[] msgAllBytes;
    boolean initData;
    int dataPacketLength;
    String serialNumber;
    byte[] commandSend;
    byte[] payload;

    public void startConnect(byte[] msgBytes) {}

    public boolean analysisMsg (byte[] msgAllBytes,  String serialNumber,  TCPIntegration chTCPIntegration, int dataPacketLength) {return false;}

    public byte[] getMsgAllBytes() {
        return msgAllBytes;
    }

    public boolean getInitData() {
        return initData;
    }

    public String isResponse(String response) {
        return null;
    }

    public int getDataPacketLength() {
        return dataPacketLength;
    }

    public byte[] getCommandMsgByteOne(String command) {
        return hexStringToByteArray(command);
    }


}
