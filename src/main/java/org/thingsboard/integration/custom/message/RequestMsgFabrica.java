package org.thingsboard.integration.custom.message;

public class RequestMsgFabrica {

    public static RequestMsg getRequestMsg(String typeDevice) {
        if (typeDevice.equals("teltonika")) {
            return new TeltonikaRequestMsg();
        } else if (typeDevice.equals("galileo")) {
            return new GalileoRequestMsg();
        }
        return null;
    }

}
