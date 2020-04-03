package org.thingsboard.integration.custom.server;

public class TeltonikaRequestMsg<T> implements RequestMsg <T> {
    @Override
    public int getLenMsg() {
        return 0;
    }

    @Override
    public int getCRC() {
        return 0;
    }
}
