package com.erss.warehouse.dto;

import lombok.Data;

@Data
public class RedirectResponseDTO {
    private String action = "redirect_package_response";
    private String inResponseTo;
    private String status;
    private String message;
}
