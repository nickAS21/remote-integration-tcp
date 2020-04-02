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
package org.thingsboard.integration.custom.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.DatatypeConverter;

@Slf4j
public class TCPClientSimpleChannelInboundHandler extends SimpleChannelInboundHandler<Object> {

    private TCPClient tcpClient;
    private String client_imev;

    public TCPClientSimpleChannelInboundHandler(TCPClient tcpClient, String client_imev) {
        this.tcpClient = tcpClient;
        this.client_imev = client_imev;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        byte[] msgBytes = (byte[]) msg;
        byte[] sentB = sentMsg(msgBytes);
        this.tcpClient.clientChannel.writeAndFlush(sentB);
    }


    private byte[] sentMsg(byte[] msgInBytes) {
        String msg = "00000000000004cb0811000000f9d0e26a6a00000000000000000000000000000000f00f07ef00f00050001503c8004502715306b50000b600004208e7180000430f7044000002f10000639d100000000000000000f9d0e26a6000000000000000000000000000000000ef0f07ef00f00050001503c8004502715306b50000b600004208e7180000430f7044000002f10000639d100000000000000000f9d0e2667800000000000000000000000000000000000f07ef01f00150001503c8004502715606b50000b60000420baa180000430f8444000002f10000639d100000000000000000f9d0e0b0f800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4c180000430fb844000002f10000639d100000000000000000f9d0dc1d1800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d0d7893800000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4c180000430fb844000002f10000639d100000000000000000f9d0d2f55800000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4d180000430fb844000002f10000639d100000000000000000f9d0ce617800000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4a180000430fb844000002f10000639d100000000000000000f9d0c9cd9800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d0c539b800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d0c0a5d800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f50180000430fb844000002f10000639d100000000000000000f9d0bc11f800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d0b30d6000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4c180000430fb844000002f10000639d100000000000000000f9d0ae798000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d0a9e5a000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f50180000430fb844000002f10000639d100000000000000000f9d0a551c000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d0a0bde000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d1000000000001100005249";
        switch (this.client_imev) {
            case "359633100458591":
                if (msgInBytes.length == 1 && msgInBytes[0] == 1) {
                    msg = "00000000000004cb0811000000f9d0e26a6a00000000000000000000000000000000f00f07ef00f00050001503c8004502715306b50000b600004208e7180000430f7044000002f10000639d100000000000000000f9d0e26a6000000000000000000000000000000000ef0f07ef00f00050001503c8004502715306b50000b600004208e7180000430f7044000002f10000639d100000000000000000f9d0e2667800000000000000000000000000000000000f07ef01f00150001503c8004502715606b50000b60000420baa180000430f8444000002f10000639d100000000000000000f9d0e0b0f800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4c180000430fb844000002f10000639d100000000000000000f9d0dc1d1800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d0d7893800000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4c180000430fb844000002f10000639d100000000000000000f9d0d2f55800000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4d180000430fb844000002f10000639d100000000000000000f9d0ce617800000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4a180000430fb844000002f10000639d100000000000000000f9d0c9cd9800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d0c539b800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d0c0a5d800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f50180000430fb844000002f10000639d100000000000000000f9d0bc11f800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d0b30d6000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4c180000430fb844000002f10000639d100000000000000000f9d0ae798000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d0a9e5a000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f50180000430fb844000002f10000639d100000000000000000f9d0a551c000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d0a0bde000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d1000000000001100005249";
                } else {
                    msg = "000000000000009f0c0106000000975665723a30332e32352e31345f3035204750533a41584e5f352e31305f333333332048773a464d42393230204d6f643a313320494d45493a33353936333331303034353835393020496e69743a313937302d312d3120303a3020557074696d653a38383832204d41433a303031453432424430364645205350433a312830292041584c3a31204f42443a3020424c3a312e372042543a34010000543b";
                }
                break;
            case "359633100458592":
                if (msgInBytes.length == 1 && msgInBytes[0] == 1) {
                    msg = "00000000000004cb0811000000f9cfb442d000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4c180000430fb344000002f10000639d100000000000000000f9cfafaef000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f51180000430fb344000002f10000639d100000000000000000f9cfab1b1000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f52180000430fb344000002f10000639d100000000000000000f9cfa6873000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f51180000430fb344000002f10000639d100000000000000000f9cfa1f35000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f50180000430fb344000002f10000639d100000000000000000f9cf9d5f7000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f53180000430fb344000002f10000639d100000000000000000f9cf98cb9000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4f180000430fb344000002f10000639d100000000000000000f9cf9437b000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4c180000430fb344000002f10000639d100000000000000000f9cf8fa3d000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4a180000430fb344000002f10000639d100000000000000000f9cf8b0ff000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4b180000430fb344000002f10000639d100000000000000000f9cf867c1000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4d180000430fb344000002f10000639d100000000000000000f9cf81e83000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f52180000430fb344000002f10000639d100000000000000000f9cf7d545000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4c180000430fb344000002f10000639d100000000000000000f9cf78c07000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4e180000430fb344000002f10000639d100000000000000000f9cf742c9000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4c180000430fb344000002f10000639d100000000000000000f9cf6f98b000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4c180000430fb344000002f10000639d100000000000000000f9cf6b04d000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f4c180000430fb344000002f10000639d100000000000110000df35";
                } else {
                    msg = "000000000000009f0c0106000000975665723a30332e32352e31345f3035204750533a41584e5f352e31305f333333332048773a464d42393230204d6f643a313320494d45493a33353936333331303034353835393020496e69743a313937302d312d3120303a3020557074696d653a39303034204d41433a303031453432424430364645205350433a312830292041584c3a31204f42443a3020424c3a312e372042543a34010000035d";

                }
                break;
            default:
        }
        return DatatypeConverter.parseHexBinary(msg);
    }
}
