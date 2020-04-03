package org.thingsboard.integration.custom.server;

import org.thingsboard.server.common.data.integration.Integration;

public interface RequestMsg <T> {

    int getLenMsg();
    int getCRC();

}
