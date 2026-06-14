package com.modernclinic.patientmanagement.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.modernclinic.patientmanagement.models.*;
import com.modernclinic.patientmanagement.models.FinanceModels.*;
import com.modernclinic.patientmanagement.models.Transaction;
import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class Services {

    @Autowired
    private Firestore firestore;

    @Autowired
    private AccountingEngine accountingEngine;

    // Database Collections
    private static final String PATIENTS = "patients";
    private static final String USERS = "users";
    private static final String DOCTORS = "doctors";
    private static final String TRANSACTIONS = "transactions";
    private static final String COA = "coa";
    private static final String VOUCHERS = "vouchers";
    private static final String PARAMETERS = "parameters";
    private static final String TREATMENTS = "treatments";
    private static final String EXPENSES = "expenses";

    private static final Map<String, String> ALIASES = new HashMap<>();
    static {
        ALIASES.put("KAREEM", "Dr. Abdul Kareem Padhiar");
        ALIASES.put("KARIM", "Dr. Abdul Kareem Padhiar");

        ALIASES.put("BASIT", "Dr. Abdul Basit Padhiar");

        ALIASES.put("JAINE", "Dr. Jainee Watt");
        ALIASES.put("JAINEE", "Dr. Jainee Watt");
        ALIASES.put("JAINI", "Dr. Jainee Watt");

        ALIASES.put("TAYYEBA", "Dr. Tayyeba Fatima");
        ALIASES.put("TAYYABA", "Dr. Tayyeba Fatima");

        ALIASES.put("RUAAZ", "Dr. Ruaaz Aziz");
        ALIASES.put("RUAAZ AZIZ", "Dr. Ruaaz Aziz");

        ALIASES.put("HADIQA", "Dr. Hadiqa");

        ALIASES.put("ZUBIA", "Dr. Zubia Razzak");
        ALIASES.put("ZOOBIA", "Dr. Zubia Razzak");
        ALIASES.put("ZOBIA", "Dr. Zubia Razzak");
    }

    @PostConstruct
    public void initSystem() {
        try {
            if (getUsers().stream().noneMatch(u -> "admin".equalsIgnoreCase(u.getUsername()))) {
                User admin = new User();
                admin.setUsername("modern");
                admin.setPassword("Clinic@2026");
                admin.setRole("ADMIN");
                saveUser(admin);
            }
            initParameters();
            initDefaultCOA(); // <-- ADD THIS LINE
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void initParameters() throws Exception {
        DocumentSnapshot doc = firestore.collection(PARAMETERS).document("system_config").get().get();
        if (!doc.exists()) {
            Map<String, Object> params = new HashMap<>();
            params.put("isFinancialPeriodLocked", false);
            params.put("clinicName", "Modern Clinic ERP");
            firestore.collection(PARAMETERS).document("system_config").set(params);
        }
    }

    // Smart Auto-Gen: Only creates accounts if the ledger is totally empty
    // Smart Auto-Gen: Takmeel-Style Simplified Clinic Ledger
    // Smart Auto-Gen: Aligned Document IDs with numeric codes for bulletproof mapping
    private void initDefaultCOA() throws Exception {
        if (getCOA().isEmpty()) {
            // 1. ASSETS (What the clinic owns)
            ChartOfAccount c1 = new ChartOfAccount(); c1.setId("1001"); c1.setCode("1001"); c1.setName("Cash Drawer (Front Desk)"); c1.setType("ASSET"); c1.setBalance(0.0); saveAccount(c1);
            ChartOfAccount c2 = new ChartOfAccount(); c2.setId("1002"); c2.setCode("1002"); c2.setName("Clinic Bank Account"); c2.setType("ASSET"); c2.setBalance(0.0); saveAccount(c2);
            ChartOfAccount c3 = new ChartOfAccount(); c3.setId("1200"); c3.setCode("1200"); c3.setName("Accounts Receivable (Unpaid Bills)"); c3.setType("ASSET"); c3.setBalance(0.0); saveAccount(c3);

            // 2. LIABILITIES (What the clinic owes vendors)
            ChartOfAccount c4 = new ChartOfAccount(); c4.setId("2000"); c4.setCode("2000"); c4.setName("Accounts Payable"); c4.setType("LIABILITY"); c4.setBalance(0.0); saveAccount(c4);

            // 3. EQUITY
            ChartOfAccount c5 = new ChartOfAccount(); c5.setId("3000"); c5.setCode("3000"); c5.setName("Owner's Equity / Capital"); c5.setType("EQUITY"); c5.setBalance(0.0); saveAccount(c5);

            // 4. REVENUE (Money generated)
            ChartOfAccount c6 = new ChartOfAccount(); c6.setId("4001"); c6.setCode("4001"); c6.setName("Dental Revenue"); c6.setType("REVENUE"); c6.setBalance(0.0); saveAccount(c6);
            ChartOfAccount c7 = new ChartOfAccount(); c7.setId("4002"); c7.setCode("4002"); c7.setName("Aesthetic Revenue"); c7.setType("REVENUE"); c7.setBalance(0.0); saveAccount(c7);

            // 5. EXPENSES
            ChartOfAccount c8 = new ChartOfAccount(); c8.setId("5000"); c8.setCode("5000"); c8.setName("General Operating Expenses"); c8.setType("EXPENSE"); c8.setBalance(0.0); saveAccount(c8);
        }
    }

    // --- 1. FINANCE DASHBOARD READERS & WRITERS ---
    public List<ChartOfAccount> getCOA() throws Exception {
        List<ChartOfAccount> list = new ArrayList<>();
        for (QueryDocumentSnapshot d : firestore.collection(COA).get().get().getDocuments()) {
            list.add(d.toObject(ChartOfAccount.class));
        }
        list.sort(Comparator.comparing(ChartOfAccount::getCode));
        return list;
    }

    public String saveAccount(ChartOfAccount acc) throws Exception {
        if (acc.getId() == null || acc.getId().isEmpty()) acc.setId(UUID.randomUUID().toString());
        if (acc.getBalance() == null) acc.setBalance(0.0);
        if (acc.getCode() == null || acc.getCode().isEmpty()) acc.setCode("-");
        firestore.collection(COA).document(acc.getId()).set(acc);
        return acc.getId();
    }

    public void deleteAccount(String id) throws Exception {
        firestore.collection(COA).document(id).delete();
    }

    public void clearCOA() throws Exception {
        for (QueryDocumentSnapshot doc : firestore.collection(COA).get().get().getDocuments()) {
            doc.getReference().delete();
        }
        // Give the database a moment to clear, then instantly regenerate!
        Thread.sleep(1000);
        initDefaultCOA();
    }

    public List<JournalVoucher> getVouchers() throws Exception {
        List<JournalVoucher> list = new ArrayList<>();
        for (QueryDocumentSnapshot d : firestore.collection(VOUCHERS).get().get().getDocuments()) {
            list.add(d.toObject(JournalVoucher.class));
        }
        list.sort(Comparator.comparing(JournalVoucher::getTimestamp).reversed());
        return list;
    }

    // --- 2. PATIENT CLINICAL CORE ---
    public Patient getPatientById(String id) throws Exception {
        DocumentSnapshot doc = firestore.collection(PATIENTS).document(id).get().get();
        return doc.exists() ? doc.toObject(Patient.class) : null;
    }

    public List<Patient> getPatients() throws Exception {
        List<Patient> list = new ArrayList<>();
        for (QueryDocumentSnapshot d : firestore.collection(PATIENTS).get().get().getDocuments()) {
            list.add(d.toObject(Patient.class));
        }
        return list;
    }

    public String savePatient(Patient p) throws Exception {
        if (p.getId() == null || p.getId().isEmpty()) {
            p.setId(UUID.randomUUID().toString());
            p.setTotalBalance(0.0);
        } else {
            Patient existing = getPatientById(p.getId());
            if (existing != null) {
                p.setTotalBalance(existing.getTotalBalance());
                p.setChiefComplaint(existing.getChiefComplaint());
                p.setMedicalHistory(existing.getMedicalHistory());
                p.setClinicalFindings(existing.getClinicalFindings());
                p.setDiagnosis(existing.getDiagnosis());
                p.setTreatmentPlan(existing.getTreatmentPlan());
                p.setAge(existing.getAge());
                p.setGender(existing.getGender());
                p.setAddress(existing.getAddress());
            }
        }
        p.setFullName(p.getFullName().trim().toUpperCase());
        p.setPhoneNumber(formatPhone(p.getPhoneNumber()));
        p.setConsultantName(cleanConsultantName(p.getConsultantName()));
        firestore.collection(PATIENTS).document(p.getId()).set(p);
        return p.getId();
    }

    public void saveClinicalNotes(String id, Map<String, String> notes) throws Exception {
        Patient p = getPatientById(id);
        if (p == null) throw new Exception("Patient not found in database.");

        p.setAge(notes.getOrDefault("age", p.getAge()));
        p.setGender(notes.getOrDefault("gender", p.getGender()));
        p.setAddress(notes.getOrDefault("address", p.getAddress()));
        p.setChiefComplaint(notes.getOrDefault("chiefComplaint", p.getChiefComplaint()));
        p.setMedicalHistory(notes.getOrDefault("medicalHistory", p.getMedicalHistory()));
        p.setClinicalFindings(notes.getOrDefault("clinicalFindings", p.getClinicalFindings()));
        p.setDiagnosis(notes.getOrDefault("diagnosis", p.getDiagnosis()));
        p.setTreatmentPlan(notes.getOrDefault("treatmentPlan", p.getTreatmentPlan()));
        firestore.collection(PATIENTS).document(p.getId()).set(p);
    }

    // --- UPDATED DELETE PATIENT WITH FIRESTORE MEMORY CLEARANCE ---
    public void deletePatient(String id) {
        firestore.collection(PATIENTS).document(id).delete();
    }

    // --- NEW: DYNAMIC FIELD RE-MAPPING FOR HUMAN-ERROR EDITS ---
    public void updatePatientFields(String id, Map<String, String> data) throws Exception {
        Patient p = getPatientById(id);
        if (p == null) throw new Exception("Patient profile not found for the requested correction.");

        // Re-map mutable demographic traits cleanly without altering ledger sheets
        if (data.containsKey("fullName") && data.get("fullName") != null) {
            p.setFullName(data.get("fullName").trim().toUpperCase());
        }
        if (data.containsKey("phoneNumber")) {
            p.setPhoneNumber(formatPhone(data.get("phoneNumber")));
        }
        if (data.containsKey("consultantName")) {
            p.setConsultantName(cleanConsultantName(data.get("consultantName")));
        }
        if (data.containsKey("type")) {
            p.setType(data.get("type").trim().toUpperCase());
        }

        // Commit modifications back to Firestore document collection path
        firestore.collection(PATIENTS).document(p.getId()).set(p);
    }

    // --- 3. SMART DOUBLE-ENTRY BRIDGE ---
    public List<Transaction> getPatientTransactions(String patientId) throws Exception {
        List<Transaction> list = new ArrayList<>();
        ApiFuture<QuerySnapshot> query = firestore.collection(TRANSACTIONS).whereEqualTo("patientId", patientId).get();

        for (QueryDocumentSnapshot doc : query.get().getDocuments()) {
            list.add(doc.toObject(Transaction.class));
        }

        // BULLETPROOF SORTING: Handles null timestamps without crashing
        list.sort((t1, t2) -> {
            Long time1 = t1.getTimestamp() != null ? t1.getTimestamp() : 0L;
            Long time2 = t2.getTimestamp() != null ? t2.getTimestamp() : 0L;
            return time1.compareTo(time2); // Ascending order (oldest to newest)
        });

        return list;
    }

    public String saveTransaction(Transaction t) throws Exception {
        if (t.getId() == null || t.getId().isEmpty()) t.setId(java.util.UUID.randomUUID().toString());
        t.setTimestamp(System.currentTimeMillis());

        Patient p = getPatientById(t.getPatientId());
        if (p == null) throw new Exception("Cannot process transaction: Patient ID not found.");

        // Update Patient's Ledger Balance
        double newBalance = p.getTotalBalance() + t.getCharges() - t.getReceived();
        t.setBalance(newBalance);
        p.setTotalBalance(newBalance);

        List<ChartOfAccount> coaList = getCOA();

        // 1. Find A/R Account (1200)
        ChartOfAccount arAcc = coaList.stream().filter(c -> "1200".equals(c.getCode())).findFirst().orElse(null);

        // 2. Find Revenue Account (4001 or 4002 based on Department)
        String revCode = "AESTHETIC".equalsIgnoreCase(p.getType()) ? "4002" : "4001";
        ChartOfAccount revAcc = coaList.stream().filter(c -> revCode.equals(c.getCode())).findFirst().orElse(null);

        // 3. ULTIMATE FIX: Dynamically resolve the Asset Account from the ID sent by the frontend
        ChartOfAccount assetAcc = coaList.stream()
                .filter(c -> c.getId().equals(t.getAssetAccountId()))
                .findFirst()
                .orElse(null);

        // Safety Checks
        if (arAcc == null) throw new Exception("Accounting Error: Accounts Receivable (1200) missing.");
        if (revAcc == null) throw new Exception("Accounting Error: Revenue Account (" + revCode + ") missing.");
        if (assetAcc == null) throw new Exception("Accounting Error: Selected Asset account not found.");

        // POST BILLING (JV)
        if (t.getCharges() > 0) {
            JournalVoucher billJv = new JournalVoucher();
            billJv.setVoucherType("JV");
            billJv.setVoucherDate(t.getDate());
            billJv.setMemo("Auto-Bill: " + p.getFullName() + " - " + t.getTreatment());

            billJv.getLines().add(accountingEngine.createLine(arAcc, t.getCharges(), 0.0, "Bill: " + t.getTreatment()));
            billJv.getLines().add(accountingEngine.createLine(revAcc, 0.0, t.getCharges(), "Revenue: " + p.getType()));
            accountingEngine.postVoucher(billJv);
        }

        // POST RECEIPT (BR) using the Dynamic Asset Account
        if (t.getReceived() > 0) {
            JournalVoucher recJv = new JournalVoucher();
            recJv.setVoucherType("BR");
            recJv.setVoucherDate(t.getDate());
            recJv.setMemo("Auto-Receipt: " + p.getFullName() + " - Payment");

            recJv.getLines().add(accountingEngine.createLine(assetAcc, t.getReceived(), 0.0, "Cash/Bank In"));
            recJv.getLines().add(accountingEngine.createLine(arAcc, 0.0, t.getReceived(), "A/R Settled"));
            accountingEngine.postVoucher(recJv);
        }

        // Commit state
        firestore.collection(PATIENTS).document(p.getId()).set(p);
        firestore.collection(TRANSACTIONS).document(t.getId()).set(t);
        return t.getId();
    }

    // --- UTILITIES ---
    private String cleanConsultantName(String name) {
        if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("null")) return "NOT ASSIGNED";
        String upper = name.toUpperCase().trim();
        String raw = upper.replaceAll("(?i)dr\\.?\\s*", "").trim();

        // If the name is in our dictionary, return the beautifully formatted official name
        if (ALIASES.containsKey(raw)) {
            return ALIASES.get(raw);
        }

        // Fallback for unlisted doctors (keeps them uppercase with DR.)
        boolean hasDr = upper.contains("DR") || upper.contains("DR.");
        return hasDr ? "DR. " + raw : raw;
    }

    public String formatPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return "N/A";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return "N/A";
        if (digits.startsWith("92")) return "+" + digits;
        if (digits.startsWith("0")) return "+92" + digits.substring(1);
        return digits.length() == 10 ? "+92" + digits : "+" + digits;
    }

    public List<Doctor> getDoctors() throws Exception {
        List<Doctor> list = new ArrayList<>();
        for (QueryDocumentSnapshot d : firestore.collection(DOCTORS).get().get().getDocuments()) list.add(d.toObject(Doctor.class));
        list.sort(Comparator.comparing(Doctor::getName));
        return list;
    }

    public String saveDoctor(Doctor d) throws Exception {
        if (d.getId() == null || d.getId().isEmpty()) d.setId(UUID.randomUUID().toString());

        // Removed the .toUpperCase() here so official names look pretty!
        d.setName(d.getName().trim());

        firestore.collection(DOCTORS).document(d.getId()).set(d);
        return d.getId();
    }

    public void deleteDoctor(String id) { firestore.collection(DOCTORS).document(id).delete(); }

    public void saveUser(User u) throws Exception {
        if (u.getId() == null) u.setId(UUID.randomUUID().toString());
        firestore.collection(USERS).document(u.getId()).set(u);
    }
    public List<User> getUsers() throws Exception {
        List<User> list = new ArrayList<>();
        for (DocumentSnapshot d : firestore.collection(USERS).get().get().getDocuments()) list.add(d.toObject(User.class));
        return list;
    }
    public User authenticate(String u, String p) throws Exception {
        return getUsers().stream().filter(user -> user.getUsername().equalsIgnoreCase(u) && user.getPassword().equals(p)).findFirst().orElse(null);
    }

    public void importFromExcel(MultipartFile file) throws Exception {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        DataFormatter fmt = new DataFormatter();
        Set<String> uniqueDocs = new HashSet<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String type = sheet.getSheetName().toUpperCase().contains("DENTAL") ? "DENTAL" : "AESTHETIC";
            int start = sheet.getSheetName().toUpperCase().contains("DENTAL") ? 6 : 4;

            for (Row row : sheet) {
                if (row == null || row.getRowNum() < start) continue;
                if (row.getCell(2) == null || row.getCell(2).getCellType() == CellType.BLANK) continue;

                Patient p = new Patient();

                // --- STRATEGIC FIX: BULLETPROOF MR-NUMBER CONVERSION ---
                String rawRegNo = fmt.formatCellValue(row.getCell(0)).trim();
                String formattedRegNo;

                if (rawRegNo.isEmpty()) {
                    // Fallback for missing entries: Generate a clean random 5-digit string
                    formattedRegNo = "MR-" + (10000 + new Random().nextInt(90000));
                } else {
                    // Clean up prefixes if they are present in the raw sheet
                    String numericPart = rawRegNo.replace("PT-", "").replace("MR-", "").trim();
                    try {
                        // Parse numerical string and explicitly pad out to 5 digits
                        int number = Integer.parseInt(numericPart);
                        formattedRegNo = "MR-" + String.format("%05d", number);
                    } catch (NumberFormatException e) {
                        // Keep alphanumeric structure safe if row has an irregular identifier
                        formattedRegNo = "MR-" + numericPart;
                    }
                }

                p.setRegNo(formattedRegNo);
                // -----------------------------------------------------

                p.setDate(fmt.formatCellValue(row.getCell(1)));
                p.setFullName(fmt.formatCellValue(row.getCell(2)));
                p.setPhoneNumber(fmt.formatCellValue(row.getCell(3)));
                p.setConsultantName(fmt.formatCellValue(row.getCell(4)));
                p.setType(type);

                savePatient(p);
                if (!p.getConsultantName().equals("NOT ASSIGNED")) uniqueDocs.add(p.getConsultantName());
            }
        }
        workbook.close();
        for (String dName : uniqueDocs) {
            if (getDoctors().stream().noneMatch(d -> d.getName().equals(dName))) saveDoctor(new Doctor(null, dName, "Consultant"));
        }
    }

    public byte[] exportToExcel() throws Exception {
        Workbook wb = new XSSFWorkbook();
        List<Patient> all = getPatients();
        createSheet(wb, "Dental", all.stream().filter(p -> "DENTAL".equals(p.getType())).collect(Collectors.toList()));
        createSheet(wb, "Aesthetics", all.stream().filter(p -> "AESTHETIC".equals(p.getType())).collect(Collectors.toList()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    private void createSheet(Workbook wb, String name, List<Patient> list) {
        Sheet s = wb.createSheet(name);
        String[] h = {"Reg #", "Date", "Patient Name", "Contact #", "Consultant Name", "Balance"};
        Row hr = s.createRow(0);
        for(int i=0; i<h.length; i++) hr.createCell(i).setCellValue(h[i]);
        int idx = 1;
        for(Patient p : list) {
            Row r = s.createRow(idx++);
            r.createCell(0).setCellValue(p.getRegNo());
            r.createCell(1).setCellValue(p.getDate());
            r.createCell(2).setCellValue(p.getFullName());
            r.createCell(3).setCellValue(p.getPhoneNumber());
            r.createCell(4).setCellValue(p.getConsultantName());
            r.createCell(5).setCellValue(p.getTotalBalance());
        }
    }

    // --- TREATMENT CATALOG ENGINE ---
    public List<Treatment> getTreatments() throws Exception {
        List<Treatment> list = new ArrayList<>();
        for (QueryDocumentSnapshot d : firestore.collection(TREATMENTS).get().get().getDocuments()) {
            list.add(d.toObject(Treatment.class));
        }
        return list;
    }

    public String saveTreatment(Treatment t) throws Exception {
        if (t.getId() == null || t.getId().isEmpty()) t.setId(java.util.UUID.randomUUID().toString());
        firestore.collection(TREATMENTS).document(t.getId()).set(t);
        return t.getId();
    }

    // ==========================================
    // OUTGOING EXPENSE & SETTLEMENT ENGINE
    // ==========================================
    public List<Expense> getExpenses() throws Exception {
        List<Expense> list = new ArrayList<>();
        for (QueryDocumentSnapshot d : firestore.collection(EXPENSES).get().get().getDocuments()) {
            list.add(d.toObject(Expense.class));
        }
        return list;
    }

    // REPLACEMENT FOR Services.java
    public String payExpenseDirect(Expense e, String assetAccountId) throws Exception {
        if (e.getId() == null || e.getId().isEmpty()) e.setId(UUID.randomUUID().toString());
        e.setStatus("PAID"); // Mark as paid immediately

        List<ChartOfAccount> coaList = getCOA();
        ChartOfAccount expAcc = coaList.stream().filter(c -> c.getId().equals(e.getAccountId())).findFirst().orElse(null);
        ChartOfAccount assetAcc = coaList.stream().filter(c -> c.getId().equals(assetAccountId)).findFirst().orElse(null);

        if (expAcc == null || assetAcc == null) throw new Exception("Invalid accounts (Expense or Asset) selected.");

        // POST BP VOUCHER (Bank Payment) immediately
        JournalVoucher bp = new JournalVoucher();
        bp.setVoucherType("BP");
        bp.setVoucherDate(e.getDate());
        bp.setMemo("Direct Payment: " + e.getDescription());

        // DR: Expense, CR: Asset (Cash/Bank)
        bp.getLines().add(accountingEngine.createLine(expAcc, e.getAmount(), 0.0, "Expense: " + e.getAccountName()));
        bp.getLines().add(accountingEngine.createLine(assetAcc, 0.0, e.getAmount(), "Direct Cash Out"));

        accountingEngine.postVoucher(bp);
        firestore.collection(EXPENSES).document(e.getId()).set(e);
        return e.getId();
    }

    public void settleExpense(String expenseId, String assetAccountId) throws Exception {
        Expense e = firestore.collection(EXPENSES).document(expenseId).get().get().toObject(Expense.class);
        if (e == null || "PAID".equals(e.getStatus())) return;

        List<ChartOfAccount> coaList = getCOA();
        ChartOfAccount assetAcc = coaList.stream().filter(c -> c.getId().equals(assetAccountId)).findFirst().orElse(null);
        ChartOfAccount apAcc = coaList.stream().filter(c -> "2000".equals(c.getCode())).findFirst().orElse(null);

        if (assetAcc == null || apAcc == null) throw new Exception("Required accounts (Asset/AP) missing.");

        JournalVoucher bp = new JournalVoucher();
        bp.setVoucherType("BP"); // Bank Payment
        bp.setVoucherDate(java.time.LocalDate.now().toString());
        bp.setMemo("Settlement: " + e.getDescription());

        bp.getLines().add(accountingEngine.createLine(apAcc, e.getAmount(), 0.0, "A/P Cleared"));
        bp.getLines().add(accountingEngine.createLine(assetAcc, 0.0, e.getAmount(), "Cash Out"));

        accountingEngine.postVoucher(bp);

        e.setStatus("PAID");
        firestore.collection(EXPENSES).document(e.getId()).set(e);
    }

    public void deleteTreatment(String id) {
        firestore.collection(TREATMENTS).document(id).delete();
    }

    // ==========================================
    // DATA MIGRATION ENGINE (WITH ZERO-PADDING)
    // ==========================================
    public String migrateToMRNumbers() throws Exception {
        List<Patient> patients = getPatients();
        int updatedCount = 0;
        Random rand = new Random();

        for (Patient p : patients) {
            String currentReg = p.getRegNo();
            boolean needsUpdate = false;

            // Rule 1: If it's completely empty, generate a brand new 5-digit MR- number
            if (currentReg == null || currentReg.trim().isEmpty()) {
                p.setRegNo("MR-" + (10000 + rand.nextInt(90000)));
                needsUpdate = true;
            }
            // Rule 2: If it has the old PT- format, swap the prefix and format the numeric tail
            else if (currentReg.startsWith("PT-")) {
                String numericPart = currentReg.replace("PT-", "").trim();
                try {
                    // Strip any accidental spacing, parse to integer, and pad with leading zeros up to 5 digits
                    int number = Integer.parseInt(numericPart);
                    String paddedNumber = String.format("%05d", number);
                    p.setRegNo("MR-" + paddedNumber);
                } catch (NumberFormatException e) {
                    // Fallback if the legacy tail contained non-numeric text modifications
                    p.setRegNo("MR-" + numericPart);
                }
                needsUpdate = true;
            }
            // Rule 3: Accommodate raw short numeric strings (e.g., "123" -> "MR-00123")
            else if (currentReg.matches("\\d+")) {
                int number = Integer.parseInt(currentReg);
                String paddedNumber = String.format("%05d", number);
                p.setRegNo("MR-" + paddedNumber);
                needsUpdate = true;
            }

            // Commit the structural correction directly back to your Firestore collection
            if (needsUpdate) {
                firestore.collection(PATIENTS).document(p.getId()).set(p);
                updatedCount++;
            }
        }
        return "Migration successfully completed! Formatted and updated " + updatedCount + " patient ledger accounts to the padded 'MR-XXXXX' framework.";
    }
}