package com.erss.warehouse.service;

import com.erss.common.proto.ACommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorldConnectorClient {

    private final RestTemplate restTemplate;


    @Value("${world.connector.url}")
    private String connectorUrl;

    public void send(ACommands cmd){

        try {
            // Protobuf → byte[] → HTTP POST
            restTemplate.postForLocation(connectorUrl, cmd.toByteArray());
            log.info("Command sent successfully to World system");
        } catch (Exception e){

            log.error("Error sending command to World system", e);
            throw e;
        }
    }
}