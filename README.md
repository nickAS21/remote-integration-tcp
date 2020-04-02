# Remote-integration-tcp
##ThingsBoard 
[![Join the chat at https://gitter.im/thingsboard/chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/thingsboard/chat?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/thingsboard/thingsboard.svg?branch=master)](https://travis-ci.org/thingsboard/thingsboard)

ThingsBoard is an open-source IoT platform for data collection, processing, visualization, and device management.

<img src="./img/logo.png?raw=true" width="100" height="100">

## Documentation

ThingsBoard documentation is hosted on [thingsboard.io](https://thingsboard.io/docs).

## IoT use cases

[**Smart metering**](https://thingsboard.io/smart-metering/)
[![Smart metering](https://user-images.githubusercontent.com/8308069/31455788-6888a948-aec1-11e7-9819-410e0ba785e0.gif "Smart metering")](https://thingsboard.io/smart-metering/)

[**IoT Rule Engine**](https://thingsboard.io/docs/user-guide/rule-engine-2-0/re-getting-started/)
[![IoT Rule Engine](https://thingsboard.io/images/demo/send-email-rule-chain.gif "IoT Rule Engine")](https://thingsboard.io/docs/user-guide/rule-engine-2-0/re-getting-started/)

[**Smart energy**](https://thingsboard.io/smart-energy/)
[![Smart energy](https://cloud.githubusercontent.com/assets/8308069/24495682/aebd45d0-153e-11e7-8de4-7360ed5b41ae.gif "Smart energy")](https://thingsboard.io/smart-energy/)

[**Smart farming**](https://thingsboard.io/smart-farming/)
[![Smart farming](https://cloud.githubusercontent.com/assets/8308069/24496824/10dc1144-1542-11e7-8aa1-5d3a281d5a1a.gif "Smart farming")](https://thingsboard.io/smart-farming/)

[**Fleet tracking**](https://thingsboard.io/fleet-tracking/)
[![Fleet tracking](https://cloud.githubusercontent.com/assets/8308069/24497169/3a1a61e0-1543-11e7-8d55-3c8a13f35634.gif "Fleet tracking")](https://thingsboard.io/fleet-tracking/)

## Getting Started (Teltonika FMB920)

Collect and Visualize your IoT data in minutes by following this [guide](https://thingsboard.io/docs/user-guide/integrations/teltonika/).

1. #### After install
    
    [tb-remote-integration-tcp.deb](/target/tb-remote-integration-tcp.deb)
    
    <i><b>Note !!!</b></i> <p>
    Warn: The version of the ThingsBoard should be written in the pom.xml file:

        <thingsboard.version>2.4.3PE</thingsboard.version>

2. #### start thimgsboard_pe
3. #### sudo service tb-remote-integration-tcp start



To test the passage of requests and responses, as well as to check the connection from the device to the dashboard using <b>"tb-remote-integration-tcp"</b> service:
- ##### you can use the following example

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
            "getparam 2004",                        // Server settings domen (example: "my.org.ua";  "his.thingsboard.io" or "IP address" from ifconfig.co
            "setparam 2004:his.thingsboard.io",     // Server settings domen new Value: "his.thingsboard.io" 
            "setparam 2004:my.org.ua",              // Server settings domen new Value: "my.org.ua" 
            "setparam 2004:192.168.1.79",           // Server settings domen new Value: "192.168.1.79" 
            "getparam 2005",                        // Server settings bindPort new Value: 1994
            "getparam 2006"                         // Server settings pototokol: TCP - 0, UDP - 1

## Support

 - [Community chat](https://gitter.im/thingsboard/chat)
 - [Q&A forum](https://groups.google.com/forum/#!forum/thingsboard)
 - [Stackoverflow](http://stackoverflow.com/questions/tagged/thingsboard)

## Licenses

This project is released under [Apache 2.0 License](./LICENSE).













