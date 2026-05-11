package com.modernclinic.patientmanagement.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private String patientId;
    private String date;
    private String toothNo;
    private String treatment;
    private double charges;
    private double received;
    private double balance;
    private long timestamp;
}