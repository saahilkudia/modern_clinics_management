package com.modernclinic.patientmanagement.services;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.modernclinic.patientmanagement.models.FinanceModels.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AccountingEngine {

    @Autowired
    private Firestore firestore;

    private static final String COA = "coa";
    private static final String VOUCHERS = "vouchers";

    /**
     * CORE DOUBLE-ENTRY POSTING ENGINE (FIRESTORE NATIVE)
     */
    public JournalVoucher postVoucher(JournalVoucher jv) throws Exception {
        if (jv.getIsLocked() != null && jv.getIsLocked()) {
            throw new RuntimeException("Period is locked. Cannot post voucher.");
        }

        double totalDr = 0;
        double totalCr = 0;

        for (JournalLine line : jv.getLines()) {
            totalDr += (line.getDebit() != null ? line.getDebit() : 0.0);
            totalCr += (line.getCredit() != null ? line.getCredit() : 0.0);
        }

        if (Math.abs(totalDr - totalCr) > 0.01) {
            throw new RuntimeException("ACCOUNTING ERROR: Debits (" + totalDr + ") do not equal Credits (" + totalCr + ").");
        }

        if (jv.getLines().size() < 2) {
            throw new RuntimeException("ACCOUNTING ERROR: Valid Journal Voucher requires at least two lines.");
        }

        for (JournalLine line : jv.getLines()) {
            if (line.getAccount() == null || line.getAccount().getId() == null) {
                throw new RuntimeException("ACCOUNTING ERROR: Missing Account reference in line.");
            }

            DocumentSnapshot doc = firestore.collection(COA).document(line.getAccount().getId()).get().get();
            if (doc.exists()) {
                ChartOfAccount acc = doc.toObject(ChartOfAccount.class);

                // NULL-SAFE EXTRACTION
                double dr = line.getDebit() != null ? line.getDebit() : 0.0;
                double cr = line.getCredit() != null ? line.getCredit() : 0.0;
                double currentBal = acc.getBalance() != null ? acc.getBalance() : 0.0;
                String accType = acc.getType() != null ? acc.getType() : "";

                // Normal Balance Rules (Including SUB-EXPENSE fix!)
                if ("ASSET".equalsIgnoreCase(accType) ||
                        "EXPENSE".equalsIgnoreCase(accType) ||
                        "SUB-EXPENSE".equalsIgnoreCase(accType)) {

                    acc.setBalance(currentBal + dr - cr);
                } else {
                    acc.setBalance(currentBal + cr - dr);
                }

                firestore.collection(COA).document(acc.getId()).set(acc);
                line.setAccount(acc);

            } else {
                throw new RuntimeException("ACCOUNTING ERROR: Invalid Account ID -> " + line.getAccount().getId());
            }
        }

        if (jv.getId() == null || jv.getId().isEmpty()) {
            jv.setId(UUID.randomUUID().toString());
        }

        jv.setTotalAmount(totalDr);
        jv.setTimestamp(System.currentTimeMillis());

        if (jv.getVoucherDate() == null) {
            jv.setVoucherDate(LocalDate.now().toString());
        }

        if (jv.getVoucherNo() == null || jv.getVoucherNo().isEmpty()) {
            String prefix = jv.getVoucherType() != null ? jv.getVoucherType() : "JV";
            jv.setVoucherNo(prefix + "-" + (System.currentTimeMillis() % 100000));
        }

        firestore.collection(VOUCHERS).document(jv.getId()).set(jv);
        return jv;
    }

    /**
     * REVERSAL ENGINE FOR ERROR CORRECTION
     */
    public JournalVoucher reverseVoucher(String originalVoucherId) throws Exception {
        DocumentSnapshot doc = firestore.collection(VOUCHERS).document(originalVoucherId).get().get();
        if (!doc.exists()) throw new RuntimeException("Voucher not found.");

        JournalVoucher original = doc.toObject(JournalVoucher.class);

        if (original.getIsReversed() != null && original.getIsReversed()) {
            throw new RuntimeException("This voucher has already been reversed.");
        }
        if (original.getIsLocked() != null && original.getIsLocked()) {
            throw new RuntimeException("Period is locked. Cannot reverse.");
        }

        JournalVoucher reversal = new JournalVoucher();
        reversal.setVoucherType("REV");
        reversal.setVoucherDate(LocalDate.now().toString());
        reversal.setMemo("Reversal of " + original.getVoucherNo());

        for(JournalLine line : original.getLines()) {
            JournalLine revLine = new JournalLine();
            revLine.setAccount(line.getAccount());
            revLine.setDebit(line.getCredit() != null ? line.getCredit() : 0.0);
            revLine.setCredit(line.getDebit() != null ? line.getDebit() : 0.0);
            revLine.setLineMemo("Reversal of Line: " + (line.getLineMemo() != null ? line.getLineMemo() : ""));
            reversal.getLines().add(revLine);
        }

        original.setIsReversed(true);
        firestore.collection(VOUCHERS).document(original.getId()).set(original);

        return postVoucher(reversal);
    }

    public JournalLine createLine(ChartOfAccount acc, Double dr, Double cr, String memo) {
        JournalLine line = new JournalLine();
        line.setAccount(acc);
        line.setDebit(dr);
        line.setCredit(cr);
        line.setLineMemo(memo);
        return line;
    }
}