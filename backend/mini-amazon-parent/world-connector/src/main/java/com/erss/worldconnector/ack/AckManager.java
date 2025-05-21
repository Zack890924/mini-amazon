package com.erss.worldconnector.ack;
import com.erss.common.proto.*;

import com.erss.worldconnector.service.WorldConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



//@Slf4j
@Component
public class AckManager {
    // tracking commends send but not ack
    private final Map<Long, AckEntry> pendingAcks;
    //received sequence ack, ready to response to world
    private final Set<Long> receivedAcks;
    // java schedule timer
    private final ScheduledExecutorService scheduler;

    @Value("${world.command.retry:3}")
    private int maxRetries;

    @Value("${world.server.command.retry.interval:3000}")
    private long retryIntervalMs;

    private Consumer<ACommands> commandSender;

    private static final Logger log = LoggerFactory.getLogger(WorldConnectionService.class);

    private AckFactory ackFactory;

    @Autowired
    public void setAckEntryFactory(AckFactory factory){

        this.ackFactory = factory;
    }

    public void setCommandSender(Consumer<ACommands> commandSender){

        this.commandSender = commandSender;
    }

    public AckManager(){

        this.pendingAcks = new ConcurrentHashMap<>();
        this.receivedAcks = new ConcurrentSkipListSet<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }


    @PreDestroy
    public void shutdown(){

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)){

                scheduler.shutdownNow();
            }
        } catch (InterruptedException e){

            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("AckManager shutdown");
    }





    public void trackCommand(long seqnum, ACommands command){


        AckEntry entry = ackFactory.createAckEntry(command);
        pendingAcks.put(seqnum, entry);

        scheduleRetry(seqnum);

    }


    public void trackAllCommands(ACommands command){

        for(APurchaseMore buy : command.getBuyList()){
            trackCommand(buy.getSeqnum(), command);
        }

        for(APack pack : command.getTopackList()){
            trackCommand(pack.getSeqnum(), command);
        }

        for(APutOnTruck load : command.getLoadList()){
            trackCommand(load.getSeqnum(), command);
        }

        for(AQuery query : command.getQueriesList()){
            trackCommand(query.getSeqnum(), command);
        }
    }


    public void handleAcks(List<Long> acks){
        for (Long ackSeqNum : acks){
            AckEntry removed = pendingAcks.remove(ackSeqNum);
            if (removed != null){

                log.debug("Received ack for seqnum: {}", ackSeqNum);
                receivedAcks.add(ackSeqNum);
            } else {
                log.warn("Received ACK for unknown seqnum: {}", ackSeqNum);
            }
        }
    }


//    public void handleErrors(List<AErr> errors){
//        for (AErr error : errors){

//            long originSeqNum = error.getOriginseqnum();
//            log.error("Error from World for seqnum {}: {}", originSeqNum, error.getErr());
//
//            pendingAcks.remove(originSeqNum);
//            receivedAcks.add(originSeqNum);
//        }
//    }

    private void scheduleRetry(long seqnum){

        scheduler.schedule(() -> {
            AckEntry entry = pendingAcks.get(seqnum);
            if (entry != null){

                if (entry.retryCount >= maxRetries){

                    log.warn("Max retries reached for command with seqnum: {}", seqnum);
                    pendingAcks.remove(seqnum);

                } else {
                    entry.retryCount++;

                    if (commandSender != null){

                        commandSender.accept(entry.command);
                        if (entry.retryCount < maxRetries){

                            scheduleRetry(seqnum);
                        }
                    } else {
                        log.error("Command sender not set, cannot retry command with seqnum: {}", seqnum);
                    }
                }
            }
        }, retryIntervalMs, TimeUnit.MILLISECONDS);
    }




}
