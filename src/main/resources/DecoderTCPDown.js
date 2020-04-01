/*
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
//                  len = 15 PacketId Param count – 2 Param id – 128 Param value length – 1 Param value – ‘0’ Param id – 106 Param value length – 1 Param value – ‘1’
var sentMoreOne = "000F         8c       0002             008A             0001                  30                006A             0001                   31 "
 //               "000F8c0002008A000130006A000131"

var msg =
    {
        // "payload": "setparam 102:1"
        // "payload": "getver"
        "payload": "getver,  getgps , setparam 133:0"
    };

var metadata =
    {
        "cs_serialNumber": "359633100458590",
        "originatorName": "FMB920_359633100458590"
    };

function Decoder(msg, metadata) {
    var codec = 12;
    var quantity = 1;
    var commandType = 5;

    var result = setPayload();

    function setPayload() {
        if (msg.hasOwnProperty('payload') && metadata['payload'] !== null) {
            return getPayload();
        }
        return null;
    }

    function getPayload() {
        var rez = {
            contentType: "JSON",
            data:  getDataHexMany(),
            metadata: {
                serialNumber: metadata['cs_serialNumber'],
                deviceName: metadata['originatorName'],
                payload:  getPayloadTrim(),
                codec: codec,
                quantity: quantity,
                commandType: commandType
            }
        };
        return rez;
    }

    function convertToHex(str) {
        var hex = '';
        for(var i=0;i<str.length;i++) {
            hex += ''+str.charCodeAt(i).toString(16);
        }
        return hex;
    }

    function convertToHexFixLen(str, len){
        var strHex = len +str.toString(16);
        return strHex.substring(strHex.length - len.length);
    }

    function getDataHexMany() {
        var dataArrays = msg.payload.split(",");
        var data = "";
        for (var i = 0; i < dataArrays.length; i ++) {
            data += (getDataHexOneForMany(dataArrays[i].trim()) + ",")
        }
        data = data.substring(0, data.lastIndexOf(","));

        return data;

    }

    function getDataHexOneForMany(str) {
        var codecHex = convertToHexFixLen(codec, "00");
        var quantityHex = convertToHexFixLen(quantity, "00");
        var typeHex = convertToHexFixLen(commandType, "00");
        var commandSizeHex = convertToHexFixLen(str.length,"00000000");
        var commandHex = convertToHex(str);
        var dataHex = codecHex + quantityHex  + typeHex + commandSizeHex + commandHex + quantityHex;
        return dataHex;
    }

    function getPayloadTrim () {
        var dataArrays = msg.payload.split(",");
        var data = "";
        for (var i = 0; i < dataArrays.length; i ++) {
            data += (dataArrays[i].trim() + ",")
        }
        data = data.substring(0, data.lastIndexOf(","));
        return data;
    }

    return result;
}

var result = Decoder(msg, metadata);
console.log(JSON.stringify(result));