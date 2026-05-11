package com.modernclinic.patientmanagement.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.modernclinic.patientmanagement.models.Doctor;
import com.modernclinic.patientmanagement.models.Patient;
import com.modernclinic.patientmanagement.models.Transaction; // Explicitly importing Transaction
import com.modernclinic.patientmanagement.models.User;
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

    private static final String PATIENTS = "patients";
    private static final String USERS = "users";
    private static final String DOCTORS = "doctors";
    private static final String TRANSACTIONS = "transactions";

    private static final Map<String, String> ALIASES = new HashMap<>();
    static {
        ALIASES.put("KAREEM", "KARIM");
        ALIASES.put("DRKAREEM", "KARIM");
        ALIASES.put("ZOOBIA", "ZOBIA");
        ALIASES.put("JAINE", "JAINEE");
    }

    @PostConstruct
    public void initMasterUser() {
        try {
            if (getUsers().stream().noneMatch(u -> "admin".equalsIgnoreCase(u.getUsername()))) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword("clinic2026");
                admin.setRole("ADMIN");
                saveUser(admin);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String cleanConsultantName(String name) {
        if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("null")) return "NOT ASSIGNED";
        String upper = name.toUpperCase().trim();
        boolean hasDr = upper.contains("DR") || upper.contains("DR.");
        String raw = upper.replaceAll("(?i)dr\\.?\\s*", "").trim();
        if (ALIASES.containsKey(raw)) raw = ALIASES.get(raw);
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

    // --- PATIENT CORE & CLINICAL CARD ---
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
            // Ensure clinical data isn't wiped on basic updates
            Patient existing = getPatientById(p.getId());
            if (existing != null) {
                p.setTotalBalance(existing.getTotalBalance());
                if (p.getChiefComplaint() == null) p.setChiefComplaint(existing.getChiefComplaint());
                if (p.getMedicalHistory() == null) p.setMedicalHistory(existing.getMedicalHistory());
                if (p.getClinicalFindings() == null) p.setClinicalFindings(existing.getClinicalFindings());
                if (p.getDiagnosis() == null) p.setDiagnosis(existing.getDiagnosis());
                if (p.getTreatmentPlan() == null) p.setTreatmentPlan(existing.getTreatmentPlan());
                if (p.getAge() == null) p.setAge(existing.getAge());
                if (p.getGender() == null) p.setGender(existing.getGender());
                if (p.getAddress() == null) p.setAddress(existing.getAddress());
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
        if (p != null) {
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
    }

    public void deletePatient(String id) { firestore.collection(PATIENTS).document(id).delete(); }

    // --- FINANCIAL LEDGER (TRANSACTIONS) ---
    public List<Transaction> getPatientTransactions(String patientId) throws Exception {
        List<Transaction> list = new ArrayList<>();
        ApiFuture<QuerySnapshot> query = firestore.collection(TRANSACTIONS).whereEqualTo("patientId", patientId).get();
        for (QueryDocumentSnapshot doc : query.get().getDocuments()) {
            list.add(doc.toObject(Transaction.class));
        }
        list.sort(Comparator.comparing(Transaction::getTimestamp));
        return list;
    }

    public String saveTransaction(Transaction t) throws Exception {
        if (t.getId() == null || t.getId().isEmpty()) t.setId(UUID.randomUUID().toString());
        t.setTimestamp(System.currentTimeMillis());

        Patient p = getPatientById(t.getPatientId());
        if (p != null) {
            // Math: Current Balance + New Bill - Amount Paid
            double newBalance = p.getTotalBalance() + t.getCharges() - t.getReceived();
            t.setBalance(newBalance);
            p.setTotalBalance(newBalance);

            // Save updated patient balance
            firestore.collection(PATIENTS).document(p.getId()).set(p);
        }
        // Save the transaction record
        firestore.collection(TRANSACTIONS).document(t.getId()).set(t);
        return t.getId();
    }

    // --- DOCTORS ---
    public List<Doctor> getDoctors() throws Exception {
        List<Doctor> list = new ArrayList<>();
        for (QueryDocumentSnapshot d : firestore.collection(DOCTORS).get().get().getDocuments()) list.add(d.toObject(Doctor.class));
        list.sort(Comparator.comparing(Doctor::getName));
        return list;
    }

    public String saveDoctor(Doctor d) throws Exception {
        if (d.getId() == null || d.getId().isEmpty()) d.setId(UUID.randomUUID().toString());
        d.setName(d.getName().toUpperCase());
        firestore.collection(DOCTORS).document(d.getId()).set(d);
        return d.getId();
    }
    public void deleteDoctor(String id) { firestore.collection(DOCTORS).document(id).delete(); }

    // --- USERS ---
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

    // --- EXCEL ---
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
                p.setRegNo(fmt.formatCellValue(row.getCell(0)));
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
}