package com.erss.warehouse.service;

import com.erss.common.service.SeqNumManager;
import com.erss.warehouse.dto.OrderItemDTO;
import com.erss.common.proto.*;
import com.erss.warehouse.repository.CommandHistoryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseWorldFacade {

    private final RestTemplate restTemplate;
    private final SeqNumManager seqNumManager;
    private final CommandHistoryRepository commandHistoryRepository;
    private final MeterRegistry meterRegistry;



    @Value("${world.connector.url}")
    private String connectorUrl;

    @Value("${warehouse.id}")
    private int warehouseId;

    private final WorldConnectorClient worldClient;


    public void buy(List<OrderItemDTO> items, long pkgId){

        if(commandHistoryRepository.existsByPackageIdAndCommandType(pkgId, "BUY")){
            log.info("Buy command for package {} already sent, skipping duplicate", pkgId);
            meterRegistry.counter("dedup.hit", "command_type", "BUY").increment();
            return;
        }
        meterRegistry.counter("dedup.miss", "command_type", "BUY").increment();

        List<AProduct> things = items.stream().map(i ->
                AProduct.newBuilder()
                        .setId(i.getProductId())
                        .setDescription(
                                i.getDescription() != null ? i.getDescription() : "N/A"
                        )
                        .setCount(i.getQuantity())
                        .build()
        ).collect(Collectors.toList());


        long seqNum = seqNumManager.generateAndRegister("BUY", pkgId, null);

        APurchaseMore buy = APurchaseMore.newBuilder()
                .setWhnum(warehouseId)
                .addAllThings(things)
                .setSeqnum(seqNum)
                .build();

        ACommands cmd = ACommands.newBuilder()
                .addBuy(buy)
                .build();

        log.info("send buy request: worldSeqNum={}, ourPackageId={}", seqNum, pkgId);
        worldClient.send(cmd);
    }


    public void pack(long packageId, List<OrderItemDTO> items){

        if(commandHistoryRepository.existsByPackageIdAndCommandType(packageId, "PACK")){
            log.info("Pack command for package {} already sent, skipping duplicate", packageId);
            meterRegistry.counter("dedup.hit", "command_type", "PACK").increment();
            return;
        }
        meterRegistry.counter("dedup.miss", "command_type", "PACK").increment();

        List<AProduct> things = items.stream()
                .map(i -> AProduct.newBuilder()
                        .setId(i.getProductId())
                        .setDescription(
                                i.getDescription() != null ? i.getDescription() : "N/A")
                        .setCount(i.getQuantity())
                        .build())
                .collect(Collectors.toList());

        long seqNum = seqNumManager.generateAndRegister("PACK", packageId, null);

        APack pack = APack.newBuilder()
                .setWhnum(warehouseId)
                .addAllThings(things)
                .setShipid(packageId)
                .setSeqnum(seqNum)
                .build();

        ACommands cmd = ACommands.newBuilder()
                .addTopack(pack)
                .build();

        log.info("send pack request: shipid={}, seqNum={}", packageId, seqNum);
        worldClient.send(cmd);
    }


    public void load(long packageId, int truckId){


        if(commandHistoryRepository.existsByPackageIdAndTruckIdAndCommandType(packageId, truckId, "LOAD")){
            log.info("Load command for package {} to truck {} already sent, skipping duplicate", packageId, truckId);
            meterRegistry.counter("dedup.hit", "command_type", "LOAD").increment();
            return;
        }
        meterRegistry.counter("dedup.miss", "command_type", "LOAD").increment();
        long seqNum = seqNumManager.generateAndRegister("LOAD", packageId, truckId);
        APutOnTruck put = APutOnTruck.newBuilder()
                .setWhnum(warehouseId)
                .setTruckid(truckId)
                .setShipid(packageId)
                .setSeqnum(seqNum)
                .build();

        ACommands cmd = ACommands.newBuilder()
                .addLoad(put)
                .build();

        log.info("send load request: shipid={}, truckId={}, seqNum={}", packageId, truckId, seqNum);
        worldClient.send(cmd);
    }
}