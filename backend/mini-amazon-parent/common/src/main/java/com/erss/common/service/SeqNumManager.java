package com.erss.common.service;

import com.erss.common.dto.OperationContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;

@Component
@Slf4j
public class SeqNumManager {

    private final Map<Long, OperationContext> pendingSeqNums = new ConcurrentHashMap<>();
    private final Map<String, Long> processedMessageKeys = new ConcurrentHashMap<>();
    private final Set<Long> processedSeqNums = new ConcurrentSkipListSet<>();

    public boolean isProcessed(long seqNum) {
        return processedSeqNums.contains(seqNum);
    }

    public boolean isProcessed(long seqNum, long packageId) {
        String key = packageId + "-" + seqNum;
        return processedMessageKeys.containsKey(key);
    }

    public void markAsProcessed(long seqNum) {
        processedSeqNums.add(seqNum);
    }

    public void markAsProcessed(long seqNum, long packageId) {
        String key = packageId + "-" + seqNum;
        processedMessageKeys.put(key, System.currentTimeMillis());
        markAsProcessed(seqNum);
    }

    public void handleAck(long seqNum) {
        OperationContext context = pendingSeqNums.remove(seqNum);
        if (context != null) {
            log.debug("seqNum: {}, operation: {}, packageId: {}",
                    seqNum, context.getOperationType(), context.getPackageId());


            if (context.getPackageId() > 0) {
                markAsProcessed(seqNum, context.getPackageId());
            } else {
                markAsProcessed(seqNum);
            }
        } else {
            log.warn("received ack for unknown seqnum: {}", seqNum);
            markAsProcessed(seqNum);
        }
    }

    public long generateAndRegister(String operationType, long packageId, Integer truckId) {
        long seqNum = generateSeqNum();
        OperationContext context = new OperationContext(operationType, packageId, truckId);
        pendingSeqNums.put(seqNum, context);
        return seqNum;
    }

    public boolean isSeqNumPending(long seqNum) {
        return pendingSeqNums.containsKey(seqNum);
    }

    public OperationContext getOperationContext(long seqNum) {
        return pendingSeqNums.get(seqNum);
    }

    private long generateSeqNum() {
        return System.nanoTime() % 1000000;
    }


    public void cleanupOldRecords() {
        long now = System.currentTimeMillis();
        long threshold = now - (24 * 60 * 60 * 1000); // 24小時前

        processedMessageKeys.entrySet().removeIf(entry -> entry.getValue() < threshold);


        if (processedSeqNums.size() > 1000) {
            int toRemove = processedSeqNums.size() - 1000;
            processedSeqNums.stream()
                    .sorted()
                    .limit(toRemove)
                    .forEach(processedSeqNums::remove);
        }
    }
}