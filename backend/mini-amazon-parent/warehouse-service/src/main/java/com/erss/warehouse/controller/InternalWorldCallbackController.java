package com.erss.warehouse.controller;

import com.erss.warehouse.dto.ArrivedRequestDTO;
import com.erss.warehouse.dto.PackReadyRequestDTO;
import com.erss.warehouse.service.WarehouseService;

import com.erss.warehouse.dto.LoadedRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/world")
@RequiredArgsConstructor
public class InternalWorldCallbackController {

    private final WarehouseService warehouseService;

    @PostMapping("/arrived")
    public ResponseEntity<Void> arrived(@RequestBody ArrivedRequestDTO dto){

        warehouseService.handleArrived(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pack-ready")
    public ResponseEntity<Void> packReady(@RequestBody PackReadyRequestDTO dto){

//        log.info("pack-ready callback received: {}", dto);
        warehouseService.handlePackReady(dto);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/loaded")
    public ResponseEntity<Void> loaded(@RequestBody LoadedRequestDTO dto){

        warehouseService.handleLoaded(dto);
        return ResponseEntity.ok().build();
    }


}
