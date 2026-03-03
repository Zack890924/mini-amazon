package com.erss.worldconnector.service;

import com.erss.worldconnector.ack.AckManager;
import com.erss.worldconnector.dto.CommandResult;

import com.erss.common.proto.*;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
@Slf4j
public class WorldConnectionService {

    @Value("${world.server.host:localhost}")
    private String worldServerHost;

    @Value("${world.server.port:23456}")
    private int worldServerPort;

    private volatile long currentWorldId = -1;

    @Value("${world.simulator.reconnect.interval:5000}")
    private long reconnectInterval;

    @Value("${world.id:-1}")
    private long configWorldId;

    private final WorldMessageHandler messageHandler;
    private final AckManager ackManager;
    private final ApplicationEventPublisher eventPublisher;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean shouldRun = new AtomicBoolean(true);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);

    private Socket socket;
    private Thread receiverThread;
    private CodedInputStream input;
    private CodedOutputStream output;

    private List<AInitWarehouse> warehouses = new ArrayList<>();

    @Autowired
    public WorldConnectionService(AckManager ackManager, ApplicationEventPublisher eventPublisher, WorldMessageHandler messageHandler){
        this.eventPublisher = eventPublisher;
        this.messageHandler = messageHandler;
        this.ackManager = ackManager;
    }

    @Synchronized
    public void setConfigWorldIdAndReconnect(long wid){
        this.configWorldId = wid;

        if (isConnected.get() && currentWorldId == wid) return;

        disconnectFromWorld();
        connectToWorld();
    }

    @PostConstruct
    private void init(){
        ackManager.setCommandSender(this::sendCommandInternal);

        warehouses.add(AInitWarehouse.newBuilder().setId(1).setX(100).setY(100).build());
        log.info("Initializing World Connection Service with configWorldId={}", configWorldId);
        connectToWorld();
    }

    @PreDestroy
    private void shutdown(){
        ackManager.shutdown();
        shouldRun.set(false);
        closeSocket();
    }

    @Synchronized
    private void disconnectFromWorld(){
        isConnected.set(false);

        if(receiverThread != null && receiverThread.isAlive()){
            receiverThread.interrupt();
            receiverThread = null;
        }

        if(socket != null && !socket.isClosed()){
            try{
                socket.close();
                socket = null;
            }catch(IOException e){
                log.error("Error closing socket", e);
            }
        }
        input = null;
        output = null;
    }

    @Synchronized
    public void connectToWorld(){
        if (isConnected.get()){
            return;
        }

        // 防止重複連線嘗試
        if (isReconnecting.getAndSet(true)) {
            log.info("Already attempting to reconnect, skipping this attempt");
            return;
        }

        try {
            log.info("Connecting to World server at {}:{}", worldServerHost, worldServerPort);
            socket = new Socket(worldServerHost, worldServerPort);
            input = CodedInputStream.newInstance(socket.getInputStream());
            output = CodedOutputStream.newInstance(socket.getOutputStream());

            AConnect.Builder connectionBuilder = AConnect.newBuilder()
                    .setIsAmazon(true);


            connectionBuilder.addAllInitwh(warehouses);

            if (configWorldId > 0){
                connectionBuilder.setWorldid(configWorldId);
                log.info("Connecting to configured world {}", configWorldId);
            } else if (currentWorldId > 0){
                connectionBuilder.setWorldid(currentWorldId);
                log.info("Reconnecting to current world {}", currentWorldId);
            } else {
                log.info("No world id provided, creating new world");
            }

            AConnect connectionMsg = connectionBuilder.build();
            sendMessage(connectionMsg);

            AConnected response = parseConnectionResponse();
            long responseWorldId = response.getWorldid();
            String responseResult = response.getResult();

            if((configWorldId > 0 || currentWorldId > 0) && responseWorldId != (configWorldId > 0 ? configWorldId : currentWorldId)){
                log.warn("Connected to a different World ID: {} (expected: {})",
                        responseWorldId, configWorldId > 0 ? configWorldId : currentWorldId);
            }

            currentWorldId = responseWorldId;
            log.info("Connected to world {} with result: {}", currentWorldId, responseResult);

            if("connected!".equals(responseResult)){
                isConnected.set(true);
                receiverThread = new Thread(this::receiveMessage);
                receiverThread.setDaemon(true);
                receiverThread.setName("WorldReceiver");
                receiverThread.start();
            } else {
                log.error("Failed to connect to World: {}", responseResult);


                if (responseResult.contains("warehouse_id") && responseResult.contains("already exists")) {
                    log.info("Retrying connection without warehouse initialization...");
                    retryConnectWithoutWarehouse();
                } else {
                    disconnectFromWorld();
                    scheduleReconnect();
                }
            }
        } catch (Exception e){
            log.error("Error connecting to World", e);
            disconnectFromWorld();
            scheduleReconnect();
        } finally {
            isReconnecting.set(false);
        }
    }

    @Synchronized
    private void retryConnectWithoutWarehouse() {
        try {
            if (socket != null) {
                socket.close();
            }

            socket = new Socket(worldServerHost, worldServerPort);
            input = CodedInputStream.newInstance(socket.getInputStream());
            output = CodedOutputStream.newInstance(socket.getOutputStream());

            AConnect.Builder connectionBuilder = AConnect.newBuilder()
                    .setIsAmazon(true);


            // connectionBuilder.addAllInitwh(warehouses);


            if (configWorldId > 0) {
                connectionBuilder.setWorldid(configWorldId);
            } else if (currentWorldId > 0) {
                connectionBuilder.setWorldid(currentWorldId);
            }

            AConnect connectionMsg = connectionBuilder.build();
            sendMessage(connectionMsg);

            AConnected response = parseConnectionResponse();
            currentWorldId = response.getWorldid();
            log.info("Retried connection to world {} with result: {}", currentWorldId, response.getResult());

            if ("connected!".equals(response.getResult())) {
                isConnected.set(true);
                receiverThread = new Thread(this::receiveMessage);
                receiverThread.setDaemon(true);
                receiverThread.setName("WorldReceiver");
                receiverThread.start();
            } else {
                log.error("Retry failed to connect to World: {}", response.getResult());
                disconnectFromWorld();
                scheduleReconnect();
            }
        } catch (Exception e) {
            log.error("Error in retry connection to World", e);
            disconnectFromWorld();
            scheduleReconnect();
        }
    }

    private AConnected parseConnectionResponse() throws IOException{
        if(input == null){
            throw new IOException("Not connected");
        }
        int length = input.readUInt32();
        byte[] data = input.readRawBytes(length);
        return AConnected.parseFrom(data);
    }

    private void scheduleReconnect(){
        log.info("Scheduling reconnect in {} ms", reconnectInterval);
        if(shouldRun.get()){
            CompletableFuture.delayedExecutor(reconnectInterval, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .execute(this::connectToWorld);
        }
    }

    public void receiveMessage(){
        log.info("Message receiver started");

        while (shouldRun.get() && isConnected.get()){
            try {
                int length = input.readUInt32();
                byte[] data = input.readRawBytes(length);
                AResponses responses = AResponses.parseFrom(data);

                log.debug("Received message from World");
                messageHandler.handleResponses(responses);
            } catch (IOException e){
                if(shouldRun.get()){
                    log.error("Error receiving message from World Simulator", e);
                    handleConnectionFailure();
                    break;
                } else {
                    log.info("Receiver thread stopping due to shutdown");
                    break;
                }
            }
        }
    }

    @Synchronized
    private void sendMessage(com.google.protobuf.Message message) throws IOException {
        if (output == null){
            throw new IOException("Not connected");
        }
        byte[] serialized = message.toByteArray();
        output.writeUInt32NoTag(serialized.length);
        output.writeRawBytes(serialized);
        output.flush();
    }

    private void sendCommandInternal(ACommands command){
        try {
            sendMessage(command);
            ackManager.trackAllCommands(command);
        } catch (IOException e){
            log.error("Error sending command to World", e);
            handleConnectionFailure();
            throw new RuntimeException("Failed to send command", e);
        }
    }

    public CompletableFuture<CommandResult> sendCommand(ACommands command){
        return CompletableFuture.supplyAsync(() -> {
            if(!isConnected.get()){
                return new CommandResult(false, "Not connected to World Simulator");
            }
            try{
                sendCommandInternal(command);
                return new CommandResult(true, "Command sent successfully");
            } catch (Exception e){
                log.error("Error sending command", e);
                return new CommandResult(false, "Error sending command: " + e.getMessage());
            }
        });
    }

    private void handleConnectionFailure(){
        disconnectFromWorld();
        scheduleReconnect();
    }

    private void closeSocket(){
        try {
            if (socket!=null) socket.close();
        }catch (IOException ignored){
        }
    }
}