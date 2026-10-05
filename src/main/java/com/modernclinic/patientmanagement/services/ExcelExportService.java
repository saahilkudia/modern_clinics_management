package com.modernclinic.patientmanagement.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

@Service
public class ExcelExportService {

    public ByteArrayInputStream exportClinicDataToExcel(List<?> patients, List<?> financialRecords) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ==========================================
            // HEADER STYLING
            // ==========================================
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // ==========================================
            // SHEET 1: PATIENT DATA
            // ==========================================
            Sheet patientSheet = workbook.createSheet("Patient Data");
            String[] pHeaders = {"Reg No", "Full Name", "Phone", "Department", "Consultant", "Total Balance"};

            Row pHeaderRow = patientSheet.createRow(0);
            for (int i = 0; i < pHeaders.length; i++) {
                Cell cell = pHeaderRow.createCell(i);
                cell.setCellValue(pHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int pRowIdx = 1;
            if (patients != null) {
                for (Object p : patients) {
                    Row row = patientSheet.createRow(pRowIdx++);
                    createCell(row, 0, getAnyFieldValue(p, "regNo", "id"), dataStyle);
                    createCell(row, 1, getAnyFieldValue(p, "fullName", "name"), dataStyle);
                    createCell(row, 2, getAnyFieldValue(p, "phoneNumber", "contact"), dataStyle);
                    createCell(row, 3, getAnyFieldValue(p, "type", "department"), dataStyle);
                    createCell(row, 4, getAnyFieldValue(p, "consultantName", "doctor"), dataStyle);
                    createCell(row, 5, getAnyFieldValue(p, "totalBalance", "balance"), dataStyle);
                }
            }

            // AWS SERVER FIX: Hardcoded column widths instead of auto-sizing
            for (int i = 0; i < pHeaders.length; i++) {
                patientSheet.setColumnWidth(i, 25 * 256); // 25 characters wide
            }

            // ==========================================
            // SHEET 2: FINANCIAL DATA
            // ==========================================
            Sheet finSheet = workbook.createSheet("Financial Data");
            String[] fHeaders = {"Date", "Reference No", "Description", "Debit (Rs)", "Credit (Rs)"};

            Row fHeaderRow = finSheet.createRow(0);
            for (int i = 0; i < fHeaders.length; i++) {
                Cell cell = fHeaderRow.createCell(i);
                cell.setCellValue(fHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int fRowIdx = 1;
            if (financialRecords != null) {
                for (Object v : financialRecords) {
                    Row row = finSheet.createRow(fRowIdx++);

                    createCell(row, 0, getAnyFieldValue(v, "voucherDate", "date", "timestamp"), dataStyle);
                    createCell(row, 1, getAnyFieldValue(v, "voucherNo", "id", "reference"), dataStyle);
                    createCell(row, 2, getAnyFieldValue(v, "memo", "description", "treatment"), dataStyle);

                    double totalDr = 0;
                    double totalCr = 0;
                    Object linesObj = getAnyFieldValue(v, "lines");

                    if (linesObj instanceof List) {
                        for (Object line : (List<?>) linesObj) {
                            Object dr = getAnyFieldValue(line, "debit");
                            if (dr instanceof Number) totalDr += ((Number) dr).doubleValue();
                            Object cr = getAnyFieldValue(line, "credit");
                            if (cr instanceof Number) totalCr += ((Number) cr).doubleValue();
                        }
                    } else {
                        Object amtObj = getAnyFieldValue(v, "amount", "charges");
                        if (amtObj instanceof Number) totalDr = ((Number) amtObj).doubleValue();
                    }

                    createCell(row, 3, totalDr > 0 ? totalDr : "-", dataStyle);
                    createCell(row, 4, totalCr > 0 ? totalCr : "-", dataStyle);
                }
            }

            // AWS SERVER FIX: Hardcoded column widths instead of auto-sizing
            for (int i = 0; i < fHeaders.length; i++) {
                finSheet.setColumnWidth(i, 25 * 256); // 25 characters wide
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void createCell(Row row, int index, Object value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value != null && !value.toString().trim().isEmpty()) {
            cell.setCellValue(value.toString());
        } else {
            cell.setCellValue("-");
        }
        cell.setCellStyle(style);
    }

    private Object getAnyFieldValue(Object obj, String... fieldNames) {
        if (obj == null) return null;
        for (String fieldName : fieldNames) {
            Object val = getFieldValue(obj, fieldName);
            if (val != null) return val;
        }
        return null;
    }

    private Object getFieldValue(Object obj, String fieldName) {
        Class<?> current = obj.getClass();
        while (current != Object.class && current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}