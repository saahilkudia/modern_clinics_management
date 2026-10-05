package com.modernclinic.patientmanagement.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {
    private String id;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String appointmentDate;  // Format: YYYY-MM-DD
    private String appointmentTime;  // Format: HH:mm
    private String status;           // SCHEDULED, COMPLETED, CANCELLED
    private String reason;
    private String deviceToken;      // Client FCM registration token for Push Notification
    private String createdAt;
}