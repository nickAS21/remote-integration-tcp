var payload = "0811000000f9d0b30d6000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4c180000430fb844000002f10000639d100000000000000000f9d0ae798000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d0a9e5a000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f50180000430fb844000002f10000639d100000000000000000f9d0a551c000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d0a0bde000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d09c2a0000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f50180000430fb844000002f10000639d100000000000000000f9d097962000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4b180000430fb844000002f10000639d100000000000000000f9d093024000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f50180000430fb844000002f10000639d100000000000000000f9d08e6e6000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f55180000430fb844000002f10000639d100000000000000000f9d089da8000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f46180000430fb844000002f10000639d100000000000000000f9d08546a000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f49180000430fb844000002f10000639d100000000000000000f9d080b2c000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f53180000430fb844000002f10000639d100000000000000000f9d07c1ee000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f50180000430fb844000002f10000639d100000000000000000f9d0778b0000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f1e180000430fb744000002f10000639d100000000000000000f9d072f72000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f4e180000430fb844000002f10000639d100000000000000000f9d06e634000000000000000000000000000000000000f07ef01f00150011504c8004502715b06b50000b60000422f54180000430fb844000002f10000639d100000000000000000f9d069cf6000000000000000000000000000000000000f07ef01f00150011503c8004502715b06b50000b60000422f52180000430fb844000002f10000639d100000000000";
// 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00     00 00 0F
//    010101010202020203030404050606
//     15  n1                                                  n2                                                                       n4                            n8
//  00 0f  07  ef 01 f0 01  50 01  15 04  c8 00  45 02  71 5a  06  b5 00  00 b6  00 00   42 2f 45    18  00 00  43 0f b3   44  00  00   02  f1 00 00 63 9d   10 00 00 00 00
//  00  00 00 00f9cf2d4f   f000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f47180000430fb344000002f10000639d100000000000000000f9cf28bc1000000000000000000000000000000000000f07ef01f00150011504c8004502715a06b50000b60000422f49180000430fb344000002f10000639d100000000000000000f9cf242830000000000000000000
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
    var numberData = payloadB[1];
    var avlDataStrLast;
    var avlDataBLast;
    var ioElementLast;



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
        var tel = [];
        var y = 0;
        var len = 0;
        avlDataStrLast = payloadStr.substr(4);
        avlDataBLast = hexStringToBytes(avlDataStrLast);
        for (var i = 0; i < 1; i++ ) {
            var timeMS = parseInt(avlDataStrLast.substr(0, 16), 16);
            var time = (new Date(timeMS)).toLocaleString();
            var priority = constants.priority[avlDataStrLast[8]];
            var gpsElementStr = avlDataStrLast.substr(22, 30);
            ioElementLast = payloadStr.substr(52, payloadStr.length-54);

            var avlData = {
                timeMS: timeMS,
                time: time,
                numberOfData: payloadB[1],
                priority: priority,
                gpsElement: getGpsElement (gpsElementStr),
                eventIO: getEvent_IO ()
            };
            tel.push(avlData);
        }
        return tel;
    }

    function getGpsElement (gpsElementStr) {
        return {
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

    function getEvent_IO() {
        var y = 0;
        var len = 2;
        var rez = {};
        rez["eventIoId"] = parseInt(ioElementLast.substr(y, len), 16);
        y +=len;
        rez["totalIo"] = parseInt(ioElementLast.substr(y, len), 16);
        y +=len;
        var oneByteIo = parseInt(ioElementLast.substr(y, len), 16);
        y +=len;
        console.log("oneByteIo", oneByteIo);
        rez["oneByteIo"] = oneByteIo;
        if (oneByteIo > 0) {
           for (var i=0; i < oneByteIo*2; i+=2) {
               rez["oneByteIoId_" + i/2] = parseInt(ioElementLast.substr(y, len), 16);
               y += len;
               rez["oneByteIoVal_" + i/2] = parseInt(ioElementLast.substr(y, len), 16);
               y += len;
           }
        }
        // console.log("y", y);
        // console.log("next", parseInt(ioElementLast.substr(y, len), 16));
        // console.log("ioElement", ioElementLast);
        return rez;

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