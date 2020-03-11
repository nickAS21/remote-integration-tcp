var payload = "0811000000f9d0e26a6a00000000000000000000000000000000f00f07ef00f00050001503c8004502715306b50000b600004208e7180000430f7044000002f10000639d100000000000000000f9d0e26a6000000000000000000000000000000000ef0f07ef00f00050001503c8004502715306b50000b600004208e7180000430f7044000002f10000639d100000000000000000f9d0e2667800000000000000000000000000000000000f07ef01f00150001503c8004502715606b50000b60000420baa180000430f8444000002f10000639d100000000000000000f9d0e0b0f800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4c180000430fb844000002f10000639d100000000000000000f9d0dc1d1800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d0d7893800000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4c180000430fb844000002f10000639d100000000000000000f9d0d2f55800000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4d180000430fb844000002f10000639d100000000000000000f9d0ce617800000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4a180000430fb844000002f10000639d100000000000000000f9d0c9cd9800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d0c539b800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d0c0a5d800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f50180000430fb844000002f10000639d100000000000000000f9d0bc11f800000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d0b30d6000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4c180000430fb844000002f10000639d100000000000000000f9d0ae798000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d0a9e5a000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f50180000430fb844000002f10000639d100000000000000000f9d0a551c000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d0a0bde000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d1000000000001100005249";
//  0811        ts
//  00 00 00 f9 d0 e2 6a 6a
//  00
//  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
//  f0 0f 07 ef00f00050001503c8004502715306b50000b
//    010101010202020203030404050606
//     15  n1                                                  n2                                                                        n4                                    n8
//  00 0f  07  ef 01 f0 01  50 01  15 04  c8 00  45 02  71 5a  06  b5 00  00   b6 00 00   42 2f 45    18  00 00  43 0f b3   44  00  00   02  f1 00 00 63 9d   10 00 00 00 00   00
//    ts
//  00 00 00 f9 cf 2d 4f f0
//  00
//  00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
//  00 0f 07 ef01f00150011504c8004502715a06b50000b60000422f47180000430fb344000002f10000639d100000000000000000f9cf28bc1000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f49180000430fb344000002f10000639d100000000000000000f9cf242830000000000000000000
//              0    1      2      3      4      5      6          0          1           2          3           4          5                0             1
var metadata = {
    "integrationName": "CUSTOM",
    "imei": "359633100458590"
};

/** Decoder **/

// decode payload to string
// var payloadStr = decodeToString(payload);

// decode payload to JSON
// var data = decodeToJson(payload);


// var deviceType = 'teltonik';
// var customerName = 'customer';
// var groupName = 'thermostat devices';
// use assetName and assetType instead of deviceName and deviceType
// to automatically create assets instead of devices.
// var assetName = 'Asset A';
// var assetType = 'building';

// Result object with device/asset attributes/telemetry data

function Decoder(payload, metadata) {
    var constants = {
        'priority':
            {
                0: "Low",
                1: "High",
                2: "Panic",
                3: "Security"
            }
    };

    var deviceName = getNameDevice('FMB920_');
    var deviceType = 'teltonik';
    var deviceModel = 'FMB920';
    var payloadStr = decodeToJson(payload);
    var payloadB = hexStringToBytes(payloadStr);
    var codec = payloadB[0];
    var numberData1 = payloadB[1];
    var avlDataStrLast;
    var avlDataBLast;
    var ioElementLastStr;
    var y = 0;
    var lenByte = 2;



    var result = setPayload();


    function setPayload() {
        if (payload !== null && payload.length > 0 && metadata.hasOwnProperty('imei')) {
            var payloadResult = getPayload();

            return payloadResult;
        }
        return null;
    }

    function getPayload() {
        var rez =  {
            deviceName: deviceName,
            deviceType: deviceType,
            attributes: {
                model:  deviceModel,
                serialNumber: metadata['imei'],
                integrationName: metadata['integrationName'],
                codec: codec
            },
            telemetry: getTelemetry ()
        };

        return rez;
    }

    function getTelemetry () {
        var telemetry = [];
        telemetry["numberOfData"] =  numberData1;
        y = 0;
        avlDataStrLast = payloadStr.substr(4);
        avlDataBLast = hexStringToBytes(avlDataStrLast);


        for (var i = 0; i < numberData1; i++ ) {
            var len = 16;
            var timeMS = parseInt(avlDataStrLast.substr(y, len), 16);
            y+=len;
            // var time = (new Date(timeMS)).toLocaleString();
            var priority = constants.priority[avlDataBLast[8]];
            y+=2;
            len = 30;
            var gpsElementStr = avlDataStrLast.substr(y, len);
            y += len;
            ioElementLastStr = avlDataStrLast.substr(y);
            var avlData = {
                ts: timeMS,
                values: getAVLData (priority, gpsElementStr, ioElementLastStr)
            };
            telemetry.push(avlData);
        }
        return telemetry;
    }

    function getAVLData (priority, gpsElementStr, ioElementLastStr) {
        var avlData = getGpsElement (priority, gpsElementStr);
        addIoElement (avlData, ioElementLastStr);
        return avlData;

    }

    function getGpsElement (priority, gpsElementStr) {
        return {
            priority: priority,
            longitude: parseInt(gpsElementStr.substr(0, 8), 16),
            latitude: parseInt(gpsElementStr.substr(8, 8), 16),
            altitude: parseInt(gpsElementStr.substr(16, 4), 16),
            angle: parseInt(gpsElementStr.substr(20, 4), 16),
            satellites: parseInt(gpsElementStr.substr(24, 2), 16),
            speed: parseInt(gpsElementStr.substr(26, 4), 16)
        }
    }

    function getNameDevice(deviceName) {
        if (metadata['imei']) {
            return deviceName + metadata['imei'];
        } else {
            deviceName;
        }
    }

    function addIoElement (avlData, ioElementLastStr) {
        // var nameIoId;
        // var rez = {};
        y = 0;
        avlData["eventIoId"] = parseInt(ioElementLastStr.substr(y, lenByte), 16);
        y +=lenByte;
        var totalIo = parseInt(ioElementLastStr.substr(y, lenByte), 16);
       // console.log("totalIo", totalIo);forByteIo
        y +=lenByte;
        var oneByteIo = parseInt(ioElementLastStr.substr(y, lenByte), 16);
        //console.log("oneByteIo", oneByteIo, y);
        y +=lenByte;
        if (oneByteIo > 0) {
            getByteIo (avlData, oneByteIo, 2);
           // for (var i=0; i < oneByteIo*2; i+=2) {
           //     nameIoId = parseInt(ioElementLastStr.substr(y, len), 16);
           //     y += len;
           //     avlData[nameIoId] = parseInt(ioElementLastStr.substr(y, len), 16);
           //     y += len;
           // }
        }
        var twoByteIo = parseInt(ioElementLastStr.substr(y, lenByte), 16);
        y +=lenByte;
       // console.log("twoByteIo", twoByteIo, y);
        if (twoByteIo > 0) {
            getByteIo (avlData,twoByteIo, 4);
        }
        var forByteIo = parseInt(ioElementLastStr.substr(y, lenByte), 16);
        y +=lenByte;
       // console.log("forByteIo", forByteIo, y);
         if (forByteIo > 0) {
            getByteIo (avlData,forByteIo, 8);
        }
        var eightByteIo = parseInt(ioElementLastStr.substr(y, lenByte), 16);
        y +=lenByte;
        // console.log("eightByteIo", eightByteIo, y);
        if (eightByteIo > 0) {
            getByteIo (avlData,forByteIo, 16);
        }
        //console.log("ioElement  ", y, ioElementLastStr);
        avlDataStrLast = ioElementLastStr.slice(y);
        y =0;
        //console.log("avlDataStrLast", y,avlDataStrLast);

    }

    function getByteIo (avlData, cnt, lenB) {
        var nameIoId;
        for (var i=0; i < cnt*2; i+=2) {
            nameIoId = parseInt(ioElementLastStr.substr(y, lenByte), 16);
            y += lenByte;
            avlData[nameIoId] = parseInt(ioElementLastStr.substr(y, lenB), 16);
            y += lenB;
        }
    }


    function decodeToJson(payload) {
        try {
            return JSON.parse(String.fromCharCode.apply(String, payload));
        } catch (e) {
            return JSON.parse(JSON.stringify(payload));
        }
    }

    function hexStringToBytes(str) {
        var array = str.match(/.{1,2}/g);
        var a = [];
        array.forEach(function (element) {
            a.push(parseInt(element, 16));
        });
        return new Uint8Array(a);
    }


    return result;

}

var result = Decoder(payload, metadata);
console.log(JSON.stringify(result));

// var result = {
// // Use deviceName and deviceType or assetName and assetType, but not both.
//     deviceName: deviceName,
//     deviceType: deviceType,
// // assetName: assetName,
// // assetType: assetType,
// //   customerName: customerName,
// //   groupName: groupName,
//     attributes: {
//         model: 'Teltonika',
//         serialNumber: metadata['imei'],
//         integrationName: metadata['integrationName']
//     },
//     telemetry: {
//         temperature: 42,
//         humidity: 80,
//         rawData: payloadStr
//     }
// };
//
// /** Helper functions **/
//
// function decodeToString(payload) {
//     return String.fromCharCode.apply(String, payload);
// }
//
//
// function getNameDevice(deviceName) {
//     if (metadata['imei']){
//         return deviceName + metadata['imei'];
//     }
//     else {
//         deviceName;
//     }
// }
//
// function decodeToJson(payload) {
//     // covert payload to string.
//     var str = decodeToString(payload);
//
//     // parse string to JSON
//     var data = JSON.parse(str);
//     return data;
// }
//
// return result;