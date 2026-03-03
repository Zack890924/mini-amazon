package com.erss.worldconnector.ack;

import com.erss.common.proto.ACommands;
import com.erss.common.proto.World;

public interface AckFactory {
    AckEntry createAckEntry(ACommands command);
}
