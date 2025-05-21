package com.erss.worldconnector.service;
import com.erss.common.service.SeqNumManager;
import com.erss.common.proto.*;
import com.erss.worldconnector.service.WarehouseCallbackClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.erss.worldconnector.ack.AckManager;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Component
public class WorldMessageHandler {
    private final AckManager ackManager;
    private final WarehouseCallbackClient callbackClient;
    private final SeqNumManager seqNumManager;

    private final Set<Long> processedMessages = new ConcurrentSkipListSet<>();

    @Autowired
    public WorldMessageHandler(AckManager ackManager,
                               WarehouseCallbackClient callbackClient,
                               SeqNumManager seqNumManager){
        this.ackManager = ackManager;
        this.callbackClient = callbackClient;
        this.seqNumManager = seqNumManager;
    }

    private boolean isMessageProcessed(long seqNum) {
        return processedMessages.contains(seqNum);
    }

    private void markMessageAsProcessed(long seqNum) {
        processedMessages.add(seqNum);
    }

    public void handleResponses(AResponses responses){
        if(!responses.getAcksList().isEmpty()){
            for(Long ack : responses.getAcksList()){
                seqNumManager.handleAck(ack);
            }
            ackManager.handleAcks(responses.getAcksList());
        }

        if(!responses.getArrivedList().isEmpty()){
            for(APurchaseMore apm : responses.getArrivedList()){
                if (!isMessageProcessed(apm.getSeqnum())) {
                    log.info("receive arrived: seq={}, wh={}, items={}", apm.getSeqnum(), apm.getWhnum(), apm.getThingsList());
                    markMessageAsProcessed(apm.getSeqnum());
                    callbackClient.notifyArrived(apm);
                } else {
                    log.info("Ignoring duplicate arrived message: seqNum={}", apm.getSeqnum());
                }
            }
        }

        if(!responses.getReadyList().isEmpty()){
            for(APacked packed : responses.getReadyList()){
                if (!isMessageProcessed(packed.getSeqnum())) {
                    log.info("receivced ready: shipid={}, seq={}", packed.getShipid(), packed.getSeqnum());
                    markMessageAsProcessed(packed.getSeqnum());
                    callbackClient.notifyReady(packed);
                } else {
                    log.info("Ignoring duplicate ready message: seqNum={}", packed.getSeqnum());
                }
            }
        }

        if(!responses.getLoadedList().isEmpty()){
            for(ALoaded loaded : responses.getLoadedList()){
                if (!isMessageProcessed(loaded.getSeqnum())) {
                    log.info("received loaded: shipid={}, seq={}", loaded.getShipid(), loaded.getSeqnum());
                    markMessageAsProcessed(loaded.getSeqnum());
                    callbackClient.notifyLoaded(loaded);
                } else {
                    log.info("Ignoring duplicate loaded message: seqNum={}", loaded.getSeqnum());
                }
            }
        }

        if(!responses.getErrorList().isEmpty()){
            for(AErr err : responses.getErrorList()){
                if (!isMessageProcessed(err.getOriginseqnum())) {
                    log.error("received error (seq={}): {}", err.getOriginseqnum(), err.getErr());
                    markMessageAsProcessed(err.getOriginseqnum());
                }
            }
        }

        if(!responses.getPackagestatusList().isEmpty()){
            for(APackage pkg : responses.getPackagestatusList()){
                if (!isMessageProcessed(pkg.getSeqnum())) {
                    log.info("received package status: pkg={}, status={}, seq={}", pkg.getPackageid(), pkg.getStatus(), pkg.getSeqnum());
                    markMessageAsProcessed(pkg.getSeqnum());
                }
            }
        }

        if(responses.hasFinished() && responses.getFinished()){
            log.info("World response finished");
        }
    }
}