package com.erss.warehouse.service;

import com.erss.warehouse.dto.*;
import com.erss.warehouse.entity.PackageOperation;
import com.erss.warehouse.entity.Product;
import com.erss.warehouse.dto.LoadedRequestDTO;
import com.erss.warehouse.entity.TrackingInfo;
import com.erss.warehouse.repository.PackageOperationRepository;
import com.erss.warehouse.repository.ProductRepository;

import com.erss.warehouse.repository.TrackingInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.erss.warehouse.dto.PackReadyRequestDTO;

import java.util.*;
import java.util.stream.Collectors;

import com.erss.warehouse.client.UpsClient;




@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WarehouseService {

    private final PackageOperationRepository packageOperationRepository;
    private final ProductRepository productRepository;
    private final WarehouseWorldFacade facade;
    private final UpsClient upsClient;
    private final TrackingInfoRepository trackingInfoRepository;
    private final MetricsService metricsService;

    @Value("${warehouse.id}")
    private int warehouseId;

    public PackageOperationResponseDTO packOrder(PackageOperationRequestDTO dto){
        log.info("process pack request: packageId={}", dto.getPackageId());

        Optional<PackageOperation> purchaseOpt = packageOperationRepository.findByPackageIdAndOperation(dto.getPackageId(), "PURCHASE");
        if(purchaseOpt.isPresent()){
            try{
                ObjectMapper mapper = new ObjectMapper();
                List<OrderItemDTO> orderItems = mapper.readValue(purchaseOpt.get().getOrderItems(),
                        mapper.getTypeFactory().constructCollectionType(List.class, OrderItemDTO.class));
                if(!matchItems(dto.getItems(), orderItems)){
                    log.warn("packOrder did not match orderItems");
                    PackageOperationResponseDTO resp = new PackageOperationResponseDTO();
                    resp.setMessage("packOrder did not match orderItems");
                    return resp;
                }
            }catch(Exception e){
                log.error("packOrder did not match orderItems", e);

            }
        }

        PackageOperation op = buildOp(dto, "PACK");
        packageOperationRepository.save(op);
        facade.pack(op.getPackageId(), dto.getItems());
        return buildOpResp(op, "PACK");
    }




    @Transactional
    public PurchaseResponseDTO processPurchase(PurchaseRequestDTO dto){

        log.info("process purchase request，items : {}", dto.getItems());

        Integer userId = null;
        if (dto.getUpsAccount() != null && dto.getUpsAccount().matches("\\d+")) {
            userId = Integer.valueOf(dto.getUpsAccount());
        }

        boolean needToPurchase = false;
        List<OrderItemDTO> itemsToPurchase = new ArrayList<>();

        for (OrderItemDTO item : dto.getItems()){
            Optional<Product> productOpt = productRepository.findByWorldProductId(item.getProductId());

            if (productOpt.isPresent()){
                Product product = productOpt.get();
                int availableStock = product.getInventory();

                if (availableStock < item.getQuantity()){

                    log.info("product {} stock={} < needed={}, will purchase",
                            product.getWorldProductId(), availableStock, item.getQuantity());
                    needToPurchase = true;
                    itemsToPurchase.add(item);
                } else{

                    log.info("product {} has enough stock={}, deduct {}",
                            product.getWorldProductId(), availableStock, item.getQuantity());
                    product.setInventory(availableStock - item.getQuantity());
                    productRepository.save(product);
                }
            }
            else{
                log.info("product {} not found in DB, will purchase", item.getProductId());
                needToPurchase = true;
                itemsToPurchase.add(item);
            }
        }

        long pkgId = System.nanoTime();
        PackageOperation op = new PackageOperation();
        op.setPackageId(pkgId);
        op.setOperation("PURCHASE");
        op.setUserId(userId);
        op.setDestinationY(dto.getDeliveryAddress().getY());
        op.setDestinationX(dto.getDeliveryAddress().getX());

        if (!dto.getItems().isEmpty()){

            try {
                ObjectMapper mapper = new ObjectMapper();
                op.setOrderItems(mapper.writeValueAsString(dto.getItems()));
            } catch (Exception e){

                log.error("Cannot serialize items", e);
            }
        }

        if (needToPurchase){

            op.setStatus("PENDING");
            metricsService.trackStateTransition("operation", null, "PENDING");
            packageOperationRepository.save(op);
            log.info("Create PURCHASE operation: packageId={}, status=PENDING", pkgId);


            facade.buy(itemsToPurchase, pkgId);

        } else {

            op.setStatus("COMPLETED");
            metricsService.trackStateTransition("operation", null, "COMPLETED");
            packageOperationRepository.save(op);
            log.info("Create PURCHASE operation: packageId={}, status=COMPLETED", pkgId);


            createArrivedOperation(pkgId);
        }

        PurchaseResponseDTO resp = new PurchaseResponseDTO();
        resp.setOrderId(pkgId);
        resp.setStatus("PLACED");
        resp.setMessage("Order placed successfully");

        upsClient.requestPickup(
                pkgId,
                warehouseId,
                userId,
                dto.getDeliveryAddress().getX(),
                dto.getDeliveryAddress().getY(),
                null,
                dto.getItems().stream().map(item -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", item.getProductId().toString());
                    m.put("description", item.getDescription());
                    m.put("quantity", item.getQuantity());
                    return m;
                }).collect(Collectors.toList())
        );

        return resp;
    }








    public PackageOperationResponseDTO processLoading(PackageOperationRequestDTO dto){

        log.info("handle pack request: packageId={}, truckId={}", dto.getPackageId(), dto.getTruckId());
        //create load op
        PackageOperation op = buildOp(dto, "LOAD");
        op.setTruckId(dto.getTruckId());
        packageOperationRepository.save(op);

        facade.load(op.getPackageId(), dto.getTruckId());
        log.info("World load request completed: packageId={}, truckId={}", op.getPackageId(), dto.getTruckId());


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


        try{
            upsClient.notifyLoadingPackage(op.getPackageId(), dto.getTruckId(), warehouseId);
            log.info("notify UPS loading: packageId={}, truckId={}", op.getPackageId(), dto.getTruckId());
        } catch (Exception e) {
            log.error("notify UPS Error", e);
        }





//        facade.load(op.getPackageId(), dto.getTruckId());
        return buildOpResp(op, "Load request sent to UPS");
    }



    @Transactional
    public RedirectResponseDTO handleRedirect(RedirectPackageDTO dto){
        log.info("handle redirect_package: newX={}, newY={}, userId={}",
                dto.getNewDestinationX(),
                dto.getNewDestinationY(),
                dto.getUserId());


        Integer userId = dto.getUserId();



        PackageOperation op = new PackageOperation();
        op.setPackageId(dto.getPackageId());
        op.setOperation("REDIRECT");
        op.setStatus("COMPLETED");

        op.setDestinationX(dto.getNewDestinationX());
        op.setDestinationY(dto.getNewDestinationY());
        packageOperationRepository.save(op);



        RedirectResponseDTO resp = new RedirectResponseDTO();
        resp.setInResponseTo("ups");
        resp.setStatus("success");
        resp.setMessage("Package redirected to (" + dto.getNewDestinationX() + "," + dto.getNewDestinationY() + ")");
        return resp;
    }








    public List<PackageOperation> getOperationsByPackage(Long pkgId){

        return packageOperationRepository.findByPackageId(pkgId);
    }

    public ProductDTO getProduct(Long worldProductId){

        return productRepository.findByWorldProductId(worldProductId)
                .map(this::convertToProductDTO)
                .orElse(null);
    }

    public List<ProductDTO> getAllProducts(){

        return productRepository.findAll().stream()
                .map(this::convertToProductDTO)
                .toList();
    }

    private ProductDTO convertToProductDTO(Product p){

        ProductDTO dto = new ProductDTO();
        dto.setWorldProductId(p.getWorldProductId());
        dto.setDescription(p.getDescription());
        dto.setQuantityAvailable(p.getInventory());
        return dto;
    }


@Transactional
public void handleArrived(ArrivedRequestDTO req){

    log.info(" Handling arrived request: seqNum={}, packageId={}, items={}", req.getSeqNum(), req.getPackageId(), req.getItems());


    List<PackageOperation> pendingPurchases = packageOperationRepository.findByStatusAndOperation("PENDING", "PURCHASE");


    // match logic
    PackageOperation matchedOperation = null;
    for (PackageOperation op : pendingPurchases){

        if (op.getOrderItems() != null){

            try {
                ObjectMapper mapper = new ObjectMapper();
                List<OrderItemDTO> orderItems =
                        mapper.readValue(op.getOrderItems(),
                                mapper.getTypeFactory()
                                        .constructCollectionType(List.class, OrderItemDTO.class)
                        );


                boolean matches = true;
                for (ArrivedItemDTO arrivedItem : req.getItems()){

                    boolean itemMatched = false;
                    for (OrderItemDTO orderItem : orderItems){

                        // productId and quantity match
                        if (arrivedItem.getProductId() == orderItem.getProductId() && arrivedItem.getCount() == orderItem.getQuantity()){

                            itemMatched = true;
                            break;
                        }
                    }
                    if (!itemMatched){

                        matches = false;
                        break;
                    }
                }

                if (matches){

                    matchedOperation = op;
                    log.info("matched the operation based on order info: packageId={}", op.getPackageId());
                    break;
                }
            } catch (Exception e){

                log.error("parse order info error", e);
            }
        }
    }
    //matches
    if (matchedOperation != null){


        Long pId = matchedOperation.getPackageId();

        matchedOperation.setStatus("COMPLETED");
        log.info("update PURCHASE operation: packageId={}, status=COMPLETED", pId);
        packageOperationRepository.save(matchedOperation);



        updateProductInventory(req.getItems());

        createArrivedOperation(pId);

        // Auto pack after arrival
        autoPackAfterArrival(pId, req.getItems());
    }
    //not match
    else{
        // first pending purchase
        log.warn("not match use the first pending purchase");


        Long pkgId = req.getPackageId();

        if (!pendingPurchases.isEmpty()){

            PackageOperation latestPurchase = pendingPurchases.get(0);
            log.info("id={}, packageId={}", latestPurchase.getId(), latestPurchase.getPackageId());


            Long pId = latestPurchase.getPackageId();

            latestPurchase.setStatus("COMPLETED");
            packageOperationRepository.save(latestPurchase);
            log.info("updated PENDING PURCHASE operation: packageId={}, status=COMPLETED", pId);


            updateProductInventory(req.getItems());
            createArrivedOperation(pId);

            // Auto pack after arrival
            autoPackAfterArrival(pId, req.getItems());
        }
        // no pending purchase, create one
        else{
            PackageOperation newPurchaseOp = new PackageOperation();
            newPurchaseOp.setPackageId(pkgId);
            newPurchaseOp.setOperation("PURCHASE");
            newPurchaseOp.setStatus("COMPLETED");
            packageOperationRepository.save(newPurchaseOp);
            log.info("Created a new PURCHASE operation: packageId={}, status=COMPLETED", pkgId);

            updateProductInventory(req.getItems());

            createArrivedOperation(pkgId);

            // Auto pack after arrival
            autoPackAfterArrival(pkgId, req.getItems());
        }
    }
}


    private void autoPackAfterArrival(Long packageId, List<ArrivedItemDTO> arrivedItems) {
        log.info("Automatically packing after arrival: packageId={}", packageId);


        List<OrderItemDTO> packItems = arrivedItems.stream()
                .map(item -> {
                    OrderItemDTO orderItem = new OrderItemDTO();
                    orderItem.setProductId(item.getProductId());
                    orderItem.setDescription(item.getDescription());
                    orderItem.setQuantity(item.getCount());
                    return orderItem;
                })
                .collect(Collectors.toList());

        PackageOperationRequestDTO packRequest = new PackageOperationRequestDTO();
        packRequest.setPackageId(packageId);
        packRequest.setItems(packItems);


        PackageOperation op = buildOp(packRequest, "PACK");
        packageOperationRepository.save(op);

        try {

            facade.pack(packageId, packItems);
            log.info("Auto-packing sent directly to World: packageId={}", packageId);
        } catch (Exception e) {
            log.error("Error during direct auto-packing: packageId={}", packageId, e);
        }
    }


    private void updateProductInventory(List<ArrivedItemDTO> items){

        if (items != null && !items.isEmpty()){

            for (ArrivedItemDTO item : items){

                Product p = productRepository.findByWorldProductId(item.getProductId())
                        .orElseGet(() -> {
                            Product np = new Product();
                            np.setWorldProductId(item.getProductId());
                            np.setDescription(item.getDescription());
                            np.setInventory(0);
                            return np;
                        });
                p.setInventory(p.getInventory() + item.getCount());
                productRepository.save(p);
                log.info("updated product inventory: productId={}, description={}, newInventory={}",
                        item.getProductId(), item.getDescription(), p.getInventory());
            }
        }
        else{
            log.warn("items is null or empty");
        }
    }



    private void createArrivedOperation(Long pkgId){


        //set status to COMPLETED
        PackageOperation arrivedOp = new PackageOperation();
        arrivedOp.setPackageId(pkgId);
        arrivedOp.setOperation("ARRIVED");
        arrivedOp.setStatus("COMPLETED");

        packageOperationRepository.findByPackageIdAndOperation(pkgId, "PURCHASE")
                .ifPresent(purchaseOp -> {
                    if (purchaseOp.getOrderItems() != null){

                        arrivedOp.setOrderItems(purchaseOp.getOrderItems());
                    }
                });

        packageOperationRepository.save(arrivedOp);
        log.info("Created a new ARRIVED operation: packageId={}, status=COMPLETED", pkgId);
    }


    @Transactional
    public void handlePackReady(PackReadyRequestDTO dto){

        log.info("received pack ready request: packageId={}, seqNum={}", dto.getPackageId(), dto.getSeqNum());

        // find PACK operation
        packageOperationRepository
                .findByPackageIdAndOperation(dto.getPackageId(), "PACK")
                .ifPresentOrElse(
                        op -> {
                            op.setStatus("COMPLETED");
                            packageOperationRepository.save(op);
                            log.info("Update PACK operation: packageId={}, status=COMPLETED", dto.getPackageId());
                            upsClient.notifyPackageReady(dto.getPackageId());
                        },
                        () -> {
                            //not found
                            PackageOperation op = new PackageOperation();
                            op.setPackageId(dto.getPackageId());
                            op.setOperation("PACK");
                            op.setStatus("COMPLETED");
                            packageOperationRepository.save(op);
                            log.info("Create new PACK operation: packageId={}, status=COMPLETED", dto.getPackageId());
                        }
                );
    }

    @Transactional
    public void handleLoaded(LoadedRequestDTO dto){

        log.info("Received loaded request: packageId={}, seqNum={}", dto.getPackageId(), dto.getSeqNum());

        packageOperationRepository
                .findByPackageIdAndOperation(dto.getPackageId(), "LOAD")
                .ifPresentOrElse(
                        op -> {

                            op.setStatus("COMPLETED");
                            packageOperationRepository.save(op);
                            packageOperationRepository.flush();
                            log.info("Updated LOAD operation: packageId={}, status=COMPLETED", dto.getPackageId());



                            Integer truckId = getTruckIdFromLoadOperation(op);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            packageOperationRepository.flush();

                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }

                            upsClient.notifyLoaded(dto.getPackageId(), truckId);



                        },
                        () -> {
                            //not found
                            log.debug("Not found, create new LOAD operation: packageId={}", dto.getPackageId());
                            PackageOperation op = new PackageOperation();
                            op.setPackageId(dto.getPackageId());
                            op.setOperation("LOAD");
                            op.setStatus("COMPLETED");
                            packageOperationRepository.save(op);
                            log.info("Created new LOAD operation: packageId={}, status=COMPLETED", dto.getPackageId());
                        }
                );
    }

    private PackageOperation buildOp(PackageOperationRequestDTO dto, String opType){

        PackageOperation op = new PackageOperation();
        op.setPackageId(dto.getPackageId() == null ? System.nanoTime() : dto.getPackageId());
        op.setOperation(opType);
        op.setStatus("PENDING");

        // turn items into JSON
        if (dto.getItems() != null && !dto.getItems().isEmpty()){

            try {
                ObjectMapper mapper = new ObjectMapper();
                op.setOrderItems(mapper.writeValueAsString(dto.getItems()));
            } catch (Exception e){

                log.error("cannot serialize items", e);
            }
        }

        return op;
    }

    private PackageOperationResponseDTO buildOpResp(PackageOperation saved, String msg){

        PackageOperationResponseDTO resp = new PackageOperationResponseDTO();
        resp.setPackageOperationId(saved.getId());
        resp.setMessage(msg);
        return resp;
    }


    private Integer getTruckIdFromLoadOperation(PackageOperation op){

        try {
            return op.getTruckId();
        } catch (Exception e){

            log.error("cannot get truckId", e);
            return null;
        }
    }


    private boolean matchItems(List<OrderItemDTO> originalItems, List<OrderItemDTO> packItems){

        if(originalItems == null || packItems == null){
            return false;
        }
        if(originalItems.size() != packItems.size()){
            return false;
        }
        Map<String, Integer> origialMap = originalItems.stream()
                .collect(Collectors.toMap(
                        item -> item.getProductId() + "|" + item.getDescription(),
                        OrderItemDTO::getQuantity,
                        Integer::sum
                ));
        Map<String, Integer> packMap = packItems.stream()
                .collect(Collectors.toMap(
                        item -> item.getProductId() + "|" + item.getDescription(),
                        OrderItemDTO::getQuantity,
                        Integer::sum
                ));

        return origialMap.equals(packMap);
    }



    public List<PackageOperation> getRedirectsByUser(Integer userId) {
        return packageOperationRepository.findByUserIdAndOperation(userId, "REDIRECT");
    }
}