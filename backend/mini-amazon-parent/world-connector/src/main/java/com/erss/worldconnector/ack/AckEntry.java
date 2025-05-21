package com.erss.worldconnector.ack;


import com.erss.common.proto.ACommands;
import com.erss.common.proto.World;

public class AckEntry {
    final ACommands command;
    int retryCount;

    public AckEntry(ACommands command, int retryCount){

        this.command = command;
        this.retryCount = retryCount;
    }
}
