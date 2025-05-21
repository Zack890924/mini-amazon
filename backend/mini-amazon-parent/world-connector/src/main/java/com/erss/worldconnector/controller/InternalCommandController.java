package com.erss.worldconnector.controller;

import com.erss.worldconnector.service.WarehouseCallbackClient;
import com.erss.worldconnector.service.WorldConnectionService;
import com.erss.common.proto.ACommands;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// world-connector-service/src/main/java/com/erss/worldconnector/controller/InternalCommandController.java
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalCommandController {
    private final WorldConnectionService worldConnectionService;

    @PostMapping("/command")
    public ResponseEntity<Void> accept(@RequestBody byte[] raw) throws Exception {
        ACommands cmd = ACommands.parseFrom(raw);
        worldConnectionService.sendCommand(cmd);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/context")
    public ResponseEntity<Void> setWorld(@RequestBody Map<String,Long> body){
        Long world_id = body.get("world_id");
        if (world_id == null) return ResponseEntity.badRequest().build();

        worldConnectionService.setConfigWorldIdAndReconnect(world_id);
        return ResponseEntity.ok().build();
    }
}
