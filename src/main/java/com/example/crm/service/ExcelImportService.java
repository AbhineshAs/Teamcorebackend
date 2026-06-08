package com.example.crm.service;

import com.example.crm.entity.Lead;
import com.example.crm.entity.User;
import com.example.crm.repository.LeadRepository;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelImportService {

    @Autowired
    private LeadRepository leadRepository;

    public void importLeads(MultipartFile file, User currentUser) throws Exception {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        List<Lead> leadList = new ArrayList<>();
        
        int addedCount = 0;
        int skippedCount = 0;

        for (Row row : sheet) {
            // Skip header or empty rows
            if (row.getRowNum() == 0 || isRowEmpty(row)) continue;

            try {
                // 1. Safe Cell Retrieval to prevent NullPointerException
                String name = getCellValueAsString(row.getCell(0));
                String email = getCellValueAsString(row.getCell(1));
                String phone = getCellValueAsString(row.getCell(2));
                String source = getCellValueAsString(row.getCell(3));

                // 2. DUPLICATE PREVENTION: Restore unique check (Phone or Email)
                if (leadRepository.existsByPhoneNumberOrEmail(phone, email)) {
                    skippedCount++;
                    continue; // Skip this lead
                }

                Lead lead = new Lead();
                lead.setCustomerName(name);
                lead.setEmail(email);
                lead.setPhoneNumber(phone);
                lead.setSource(source != null ? source : "Bulk Import");
                lead.setStatus("New");
                lead.setFollowUpStatus("PENDING");
                lead.setUser(currentUser);

                // 3. Robust Follow-up Date Logic (Column index 4)
                Cell dateCell = row.getCell(4);
                if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                    lead.setFollowUpDate(dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                } else if (dateCell != null && dateCell.getCellType() == CellType.STRING && !dateCell.getStringCellValue().isEmpty()) {
                    lead.setFollowUpDate(LocalDate.parse(dateCell.getStringCellValue()));
                } else {
                    lead.setFollowUpDate(LocalDate.now()); // Default to today
                }

                leadList.add(lead);
                addedCount++;

            } catch (Exception e) {
                skippedCount++; // Increment skipped on parsing errors
                System.err.println("Error parsing row " + row.getRowNum() + ": " + e.getMessage());
            }
        }

        // 4. Batch Save
        if (!leadList.isEmpty()) {
            leadRepository.saveAll(leadList);
        }
        workbook.close();

        // 5. Store counts in Session for the UI Popup
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession();
        session.setAttribute("bulkAdded", addedCount);
        session.setAttribute("bulkSkipped", skippedCount);
    }

    // Helper: Prevent crashes on different cell types (Numeric/String)
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: 
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    // Helper: Skip rows that are physically in Excel but have no data
    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}