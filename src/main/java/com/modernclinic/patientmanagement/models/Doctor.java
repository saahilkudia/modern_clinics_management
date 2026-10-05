package com.modernclinic.patientmanagement.models;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {
    private String id;
    private String name;
    private String specialization;
}