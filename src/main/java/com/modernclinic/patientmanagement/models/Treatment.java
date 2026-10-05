package com.modernclinic.patientmanagement.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Treatment {
    private String id;
    private String name;
    private String category; // DENTAL or AESTHETIC
    private Double price;
}