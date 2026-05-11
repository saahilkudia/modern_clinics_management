package com.modernclinic.patientmanagement.models;

import lombok.Data;

@Data
public class Patient {
    private String id;
    private String regNo;
    private String date;
    private String fullName;
    private String phoneNumber;
    private String consultantName;
    private String type; // DENTAL or AESTHETIC
    private long registeredAt;

    // Demographics
    private String age;
    private String gender;
    private String address;

    // Clinical Card Details
    private String chiefComplaint;
    private String medicalHistory;
    private String clinicalFindings;
    private String diagnosis;
    private String treatmentPlan;

    // Financials
    private double totalBalance;
}