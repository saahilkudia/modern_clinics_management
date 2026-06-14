package com.modernclinic.patientmanagement.models;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

public class FinanceModels {

    @Data
    public static class ChartOfAccount {
        private String id;
        private String code;
        private String name;
        private String type; // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
        private Double balance = 0.0;
    }

    @Data
    public static class JournalLine {
        private ChartOfAccount account;
        private Double debit = 0.0;
        private Double credit = 0.0;
        private String lineMemo;
    }

    @Data
    public static class JournalVoucher {
        private String id;
        private String voucherNo;
        private String voucherDate;
        private String memo;
        private String voucherType;
        private Boolean isLocked = false;
        private Boolean isReversed = false;
        private Double totalAmount = 0.0;
        private List<JournalLine> lines = new ArrayList<>();
        private long timestamp;
    }
}