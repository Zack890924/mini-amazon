package com.erss.worldconnector.dto;

import lombok.Getter;

public class CommandResult {
    private final boolean success;
    @Getter
    private final String message;

    public CommandResult(boolean success, String message){

        this.success = success;
        this.message = message;
    }


}
