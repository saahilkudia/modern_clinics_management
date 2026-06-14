package com.modernclinic.patientmanagement.controllers;

import com.modernclinic.patientmanagement.models.*;
import com.modernclinic.patientmanagement.models.FinanceModels.*;
import com.modernclinic.patientmanagement.services.AccountingEngine;
import com.modernclinic.patientmanagement.services.Services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class ClinicController {

    @Autowired
    private Services services;

    @Autowired
    private AccountingEngine accountingEngine;

    // ==========================================
    // 1. DASHBOARD & UI ROUTING
    // ==========================================
    @GetMapping
    public String index(Model model) throws Exception {
        return "index";
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody User req) {
        try {
            User u = services.authenticate(req.getUsername(), req.getPassword());
            return u != null ? ResponseEntity.ok(u) : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login Error: " + e.getMessage());
        }
    }

    // THE MISSING DASHBOARD SUMMARY ENDPOINT
    @GetMapping("/api/summary")
    @ResponseBody
    public ResponseEntity<?> getSummary() {
        try {
            List<Patient> all = services.getPatients();
            List<ChartOfAccount> coa = services.getCOA();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPatients", all.size());
            stats.put("dentalCount", all.stream().filter(p -> "DENTAL".equals(p.getType())).count());
            stats.put("aestheticCount", all.stream().filter(p -> "AESTHETIC".equals(p.getType())).count());

            double ar = coa.stream().filter(c -> "ASSET".equalsIgnoreCase(c.getType()) && c.getName() != null && c.getName().toLowerCase().contains("receivable")).mapToDouble(ChartOfAccount::getBalance).sum();
            double cash = coa.stream().filter(c -> "ASSET".equalsIgnoreCase(c.getType()) && c.getName() != null && (c.getName().toLowerCase().contains("cash") || c.getName().toLowerCase().contains("bank"))).mapToDouble(ChartOfAccount::getBalance).sum();
            double rev = coa.stream().filter(c -> "REVENUE".equalsIgnoreCase(c.getType())).mapToDouble(ChartOfAccount::getBalance).sum();
            double exp = coa.stream().filter(c -> "EXPENSE".equalsIgnoreCase(c.getType())).mapToDouble(ChartOfAccount::getBalance).sum();

            stats.put("totalAR", ar);
            stats.put("liquidReserves", cash);
            stats.put("totalRevenue", rev);
            stats.put("totalExpense", exp);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ==========================================
    // 2. PATIENT CARD & LEDGER API
    // ==========================================

    // THE MISSING GET PATIENTS ENDPOINT
    @GetMapping("/api/patients")
    @ResponseBody
    public ResponseEntity<?> getAllPatients() {
        try {
            return ResponseEntity.ok(services.getPatients());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/api/patients")
    @ResponseBody
    public String saveP(@RequestBody Patient p) throws Exception {
        return services.savePatient(p);
    }

    @GetMapping("/api/patients/{id}")
    @ResponseBody
    public ResponseEntity<?> getPatient(@PathVariable String id) {
        try {
            Patient p = services.getPatientById(id);
            if (p != null) return ResponseEntity.ok(p);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Patient not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/api/patients/{id}")
    @ResponseBody
    public void deleteP(@PathVariable String id) {
        services.deletePatient(id);
    }

    // --- NEW: ENDPOINT TO PATCH REGISTRY HUMAN DATA-ENTRY ERRORS ---
    @PutMapping("/api/patients/{id}")
    @ResponseBody
    public ResponseEntity<?> updatePatientData(@PathVariable String id, @RequestBody Map<String, String> payload) {
        try {
            services.updatePatientFields(id, payload);
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Patient metadata successfully altered."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Correction Failed: " + e.getMessage());
        }
    }

    @PostMapping("/api/patients/{id}/notes")
    @ResponseBody
    public ResponseEntity<?> saveNotes(@PathVariable String id, @RequestBody Map<String, String> notes) {
        try {
            services.saveClinicalNotes(id, notes);
            return ResponseEntity.ok("Saved Successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ==========================================
    // 3. DOUBLE-ENTRY ERP FINANCE API
    // ==========================================

    @GetMapping("/api/finance/coa")
    @ResponseBody
    public ResponseEntity<?> getCOA() {
        try { return ResponseEntity.ok(services.getCOA()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); }
    }

    @PostMapping("/api/finance/coa")
    @ResponseBody
    public ResponseEntity<?> saveCOA(@RequestBody ChartOfAccount acc) {
        try {
            // Force clean ID assignment as a structural string if null or numerical
            if (acc.getId() != null) {
                acc.setId(String.valueOf(acc.getId()).trim());
            }
            return ResponseEntity.ok(services.saveAccount(acc));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/api/finance/coa/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCOA(@PathVariable String id) {
        try {
            services.deleteAccount(id);
            return ResponseEntity.ok("Account Deleted");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/api/finance/vouchers")
    @ResponseBody
    public ResponseEntity<?> getVouchers() {
        try { return ResponseEntity.ok(services.getVouchers()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); }
    }

    @PostMapping("/api/finance/vouchers")
    @ResponseBody
    public ResponseEntity<?> postVoucher(@RequestBody JournalVoucher jv) {
        try {
            return ResponseEntity.ok(accountingEngine.postVoucher(jv));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/api/finance/stats")
    @ResponseBody
    public ResponseEntity<?> getFinanceStats() {
        try {
            List<ChartOfAccount> coa = services.getCOA();
            double revenue = coa.stream().filter(c -> "REVENUE".equalsIgnoreCase(c.getType())).mapToDouble(ChartOfAccount::getBalance).sum();
            double expense = coa.stream().filter(c -> "EXPENSE".equalsIgnoreCase(c.getType())).mapToDouble(ChartOfAccount::getBalance).sum();
            double cash = coa.stream().filter(c -> "ASSET".equalsIgnoreCase(c.getType()) && c.getName() != null && c.getName().toLowerCase().contains("cash")).mapToDouble(ChartOfAccount::getBalance).sum();

            Map<String, Double> stats = new HashMap<>();
            stats.put("revenue", revenue);
            stats.put("expense", expense);
            stats.put("netProfit", revenue - expense);
            stats.put("cash", cash);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ==========================================
    // 4. DOCTORS & UTILITIES API
    // ==========================================

    // THE MISSING GET DOCTORS ENDPOINT
    @GetMapping("/api/doctors")
    @ResponseBody
    public ResponseEntity<?> getAllDoctors() {
        try {
            return ResponseEntity.ok(services.getDoctors());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/api/doctors")
    @ResponseBody
    public String saveD(@RequestBody Doctor d) throws Exception {
        return services.saveDoctor(d);
    }

    @DeleteMapping("/api/doctors/{id}")
    @ResponseBody
    public void deleteD(@PathVariable String id) {
        services.deleteDoctor(id);
    }

    @PostMapping("/api/patients/import")
    @ResponseBody
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) {
        try { services.importFromExcel(file); return ResponseEntity.ok("Success"); }
        catch (Exception e) { return ResponseEntity.status(500).body("Error"); }
    }

    @GetMapping("/api/patients/export")
    public ResponseEntity<byte[]> exportExcel() throws Exception {
        byte[] data = services.exportToExcel();
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        h.setContentDisposition(ContentDisposition.attachment().filename("Clinic_ERP_Data.xlsx").build());
        return new ResponseEntity<>(data, h, HttpStatus.OK);
    }

    // ==========================================
    // 5. TREATMENT CATALOG API
    // ==========================================
    @GetMapping("/api/treatments")
    @ResponseBody
    public ResponseEntity<?> getTreatments() {
        try { return ResponseEntity.ok(services.getTreatments()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); }
    }

    @PostMapping("/api/treatments")
    @ResponseBody
    public ResponseEntity<?> saveTreatment(@RequestBody Treatment t) {
        try { return ResponseEntity.ok(services.saveTreatment(t)); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); }
    }

    @DeleteMapping("/api/treatments/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteTreatment(@PathVariable String id) {
        try { services.deleteTreatment(id); return ResponseEntity.ok("Deleted"); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); }
    }

    // ==========================================
    // SYSTEM & SETTINGS API
    // ==========================================
    @DeleteMapping("/api/system/reset-coa")
    @ResponseBody
    public ResponseEntity<?> resetCOA() {
        try {
            services.clearCOA();
            return ResponseEntity.ok("COA Wiped and Regenerated");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/api/expenses/pay-now")
    @ResponseBody
    public ResponseEntity<?> payNow(@RequestBody Map<String, Object> payload) {
        try {
            Expense e = new Expense();
            e.setAccountId((String)payload.get("accountId"));
            e.setAmount(Double.valueOf(payload.get("amount").toString()));
            e.setDate((String)payload.get("date"));
            e.setDescription((String)payload.get("description"));

            String assetAccountId = (String)payload.get("assetAccountId");

            services.payExpenseDirect(e, assetAccountId);
            return ResponseEntity.ok("Paid Successfully");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }

    // ==========================================
    // EXPENSE & SETTLEMENT API
    // ==========================================
    @GetMapping("/api/expenses")
    @ResponseBody
    public ResponseEntity<?> getExpenses() {
        try { return ResponseEntity.ok(services.getExpenses()); }
        catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); }
    }

    @PostMapping("/api/expenses/{id}/settle")
    @ResponseBody
    public ResponseEntity<?> settleExpense(@PathVariable String id, @RequestParam String assetAccountId) {
        try { services.settleExpense(id, assetAccountId); return ResponseEntity.ok("Settled"); }
        catch (Exception ex) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage()); }
    }

    // ==========================================
    // PATIENT DOSSIER & TRANSACTIONS API
    // ==========================================
    @PostMapping("/api/transactions")
    @ResponseBody
    public ResponseEntity<?> saveTransaction(@RequestBody Transaction t) {
        try {
            return ResponseEntity.ok(services.saveTransaction(t));
        } catch (Exception e) {
            e.printStackTrace(); // Logs the exact error line to your IDE console

            // Safety Net: Prevents Spring from returning a raw JSON block if the message is null
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = "System Error: " + e.getClass().getSimpleName() + " occurred.";
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMsg);
        }
    }

    @GetMapping("/api/patients/{id}/transactions")
    @ResponseBody
    public ResponseEntity<?> getPatientTransactions(@PathVariable String id) {
        try {
            return ResponseEntity.ok(services.getPatientTransactions(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/api/system/migrate-mr")
    @ResponseBody
    public ResponseEntity<?> migrateMR() {
        try {
            return ResponseEntity.ok(services.migrateToMRNumbers());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}