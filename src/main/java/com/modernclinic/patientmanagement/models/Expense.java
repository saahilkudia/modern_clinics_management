package com.modernclinic.patientmanagement.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Expense {
    private String id;
    private String accountId;   // The EXPENSE account from CoA
    private String accountName; // The name (e.g., "Generator Fuel")
    private Double amount;
    private String date;
    private String description;
    private String status;      // "UNPAID" or "PAID"
}