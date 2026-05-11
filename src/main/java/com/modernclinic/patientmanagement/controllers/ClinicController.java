package com.modernclinic.patientmanagement.controllers;

import com.modernclinic.patientmanagement.models.*;
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

    @GetMapping
    public String index(Model model) throws Exception {
        List<Patient> all = services.getPatients();
        long dCount = all.stream().filter(p -> "DENTAL".equals(p.getType())).count();
        long aCount = all.stream().filter(p -> "AESTHETIC".equals(p.getType())).count();

        model.addAttribute("dental", all.stream().filter(p -> "DENTAL".equals(p.getType())).collect(Collectors.toList()));
        model.addAttribute("aesthetic", all.stream().filter(p -> "AESTHETIC".equals(p.getType())).collect(Collectors.toList()));
        model.addAttribute("doctors", services.getDoctors());

        model.addAttribute("totalCount", all.size());
        model.addAttribute("dCount", dCount);
        model.addAttribute("aCount", aCount);

        Map<String, Long> doctorMap = all.stream()
                .filter(p -> p.getConsultantName() != null && !p.getConsultantName().equals("NOT ASSIGNED"))
                .collect(Collectors.groupingBy(Patient::getConsultantName, Collectors.counting()));

        List<String> docLabels = doctorMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5).map(Map.Entry::getKey).collect(Collectors.toList());

        List<Long> docValuesList = docLabels.stream().map(doctorMap::get).collect(Collectors.toList());

        model.addAttribute("docLabels", String.join(",", docLabels));
        model.addAttribute("docValues", docValuesList.stream().map(String::valueOf).collect(Collectors.joining(",")));

        return "index";
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody User req) throws Exception {
        User u = services.authenticate(req.getUsername(), req.getPassword());
        return u != null ? ResponseEntity.ok(u) : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // --- PATIENT CORE API ---
    @PostMapping("/api/patients")
    @ResponseBody
    public String saveP(@RequestBody Patient p) throws Exception { return services.savePatient(p); }

    // CRITICAL FIX: Endpoint required to load the patient data into the Card UI
    @GetMapping("/api/patients/{id}")
    @ResponseBody
    public Patient getPatient(@PathVariable String id) throws Exception { return services.getPatientById(id); }

    @DeleteMapping("/api/patients/{id}")
    @ResponseBody
    public void deleteP(@PathVariable String id) { services.deletePatient(id); }


    // --- DOSSIER (CARD) & LEDGER API ---

    // CRITICAL FIX: Endpoint required to save Clinical Notes
    @PostMapping("/api/patients/{id}/notes")
    @ResponseBody
    public ResponseEntity<String> saveNotes(@PathVariable String id, @RequestBody Map<String, String> notes) {
        try {
            services.saveClinicalNotes(id, notes);
            return ResponseEntity.ok("Saved");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // CRITICAL FIX: Endpoint required to fetch all Transactions for the Ledger
    @GetMapping("/api/patients/{id}/transactions")
    @ResponseBody
    public List<Transaction> getTransactions(@PathVariable String id) throws Exception {
        return services.getPatientTransactions(id);
    }

    // CRITICAL FIX: Endpoint required to save a new Transaction
    @PostMapping("/api/transactions")
    @ResponseBody
    public String saveTransaction(@RequestBody Transaction t) throws Exception {
        return services.saveTransaction(t);
    }


    // --- DOCTORS API ---
    @PostMapping("/api/doctors")
    @ResponseBody
    public String saveD(@RequestBody Doctor d) throws Exception { return services.saveDoctor(d); }

    @DeleteMapping("/api/doctors/{id}")
    @ResponseBody
    public void deleteD(@PathVariable String id) { services.deleteDoctor(id); }

    // --- EXCEL ---
    @PostMapping("/api/patients/import")
    @ResponseBody
    public ResponseEntity<String> importExcel(@RequestParam("file") MultipartFile file) {
        try { services.importFromExcel(file); return ResponseEntity.ok("Success"); }
        catch (Exception e) { return ResponseEntity.status(500).body("Error"); }
    }

    @GetMapping("/api/patients/export")
    public ResponseEntity<byte[]> exportExcel() throws Exception {
        byte[] data = services.exportToExcel();
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        h.setContentDisposition(ContentDisposition.attachment().filename("Clinic_Data.xlsx").build());
        return new ResponseEntity<>(data, h, HttpStatus.OK);
    }
}