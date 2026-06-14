package com.modernclinic.patientmanagement.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data // <--- This annotation generates getters and setters for all fields
@NoArgsConstructor
public class Transaction {
    private String id;
    private String patientId;
    private String date;
    private String toothNo;
    private String treatment;
    private Double charges = 0.0;
    private Double received = 0.0;
    private Double balance = 0.0;
    private Long timestamp;

    // ADD THIS FIELD: This is what the frontend sends and the backend needs
    private String assetAccountId;
}