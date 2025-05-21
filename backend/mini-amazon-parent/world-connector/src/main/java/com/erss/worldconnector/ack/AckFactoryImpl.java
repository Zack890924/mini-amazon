package com.erss.worldconnector.ack;


import com.erss.common.proto.ACommands;
import com.erss.common.proto.World;
import org.springframework.stereotype.Component;

@Component
public class AckFactoryImpl implements AckFactory {
    @Override
    public AckEntry createAckEntry(ACommands command){

        return new AckEntry(command, 0);
    }
}
