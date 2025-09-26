package com.configserver.hrm.attendanceService.service.impl;

import com.configserver.hrm.attendanceService.config.EmployeeEmailConfig;
import com.configserver.hrm.attendanceService.dto.AttendanceRequestDTO;
import com.configserver.hrm.attendanceService.entity.AttendanceStatus;
import com.configserver.hrm.attendanceService.entity.EmployeeAttendance;
import com.configserver.hrm.attendanceService.exception.ExcelDownloadException;
import com.configserver.hrm.attendanceService.exception.InvalidAttendanceDataException;
import com.configserver.hrm.attendanceService.external.EtimeOfficeService;
import com.configserver.hrm.attendanceService.repository.EmployeeAttendanceRepository;
import com.configserver.hrm.attendanceService.service.AttendanceService;
import com.configserver.hrm.attendanceService.service.EmailService;
import com.configserver.hrm.attendanceService.util.ExcelReportGenerator;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private EmployeeAttendanceRepository repository;


    @Autowired
    private EmailService emailService;

    @Autowired
    private EmployeeEmailConfig employeeEmailConfig;

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    @Autowired
    private EtimeOfficeService etimeOfficeService;

    @Override
    public void importAttendance(List<AttendanceRequestDTO> attendanceList) {
        attendanceList.stream()
                .filter(dto -> !repository.existsByEmployeeIdAndDate(dto.getEmployeeId(), dto.getDate()))
                .map(this::mapDtoToEntity)
                .forEach(repository::save);
    }

    @Override
    @Transactional
    public List<EmployeeAttendance> importDailyAttendanceFromEtimeOffice() {
        try {
            String reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            byte[] excelData = etimeOfficeService.downloadDailyReport(reportDate);
            if (excelData == null || excelData.length == 0) {
                throw new RuntimeException("Downloaded report empty");
            }

            repository.deleteByDate(LocalDate.now());

            List<AttendanceRequestDTO> attendanceList = parseExcelToDTO(excelData, LocalDate.now());
            if (!attendanceList.isEmpty()) {
                importAttendance(attendanceList);
            }

            return repository.findByDate(LocalDate.now());

        } catch (Exception e) {
            throw new RuntimeException("Failed to import daily attendance: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EmployeeAttendance> importAttendanceFromEtimeOffice(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidAttendanceDataException("Future date not allowed: " + date);
        }

        try {
            String reportDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            byte[] excelData = etimeOfficeService.downloadDailyReport(reportDate);
            if (excelData == null || excelData.length == 0) {
                throw new ExcelDownloadException("Downloaded report empty for date: " + reportDate);
            }

            repository.deleteByDate(date);

            List<AttendanceRequestDTO> attendanceList = parseExcelToDTO(excelData, date);
            if (!attendanceList.isEmpty()) {
                importAttendance(attendanceList);
            }

            return repository.findByDate(date);

        } catch (Exception e) {
            throw new ExcelDownloadException("Failed to import attendance for date " + date, e);
        }
    }

    @Override
    public List<EmployeeAttendance> getDailyAttendance(LocalDate date) {
        return repository.findByDate(date);
    }

    @Override
    public List<EmployeeAttendance> getEmployeeAttendance(String employeeId, LocalDate from, LocalDate to) {
        return repository.findByEmployeeIdAndDateBetween(employeeId, from, to);
    }
    @Override
    public byte[] downloadAttendanceReport(LocalDate date) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String formattedDate = date.format(formatter);

            byte[] excelData = etimeOfficeService.downloadDailyReport(formattedDate);
            if (excelData == null || excelData.length == 0) {
                throw new ExcelDownloadException("No report found for date: " + formattedDate);
            }
            return excelData;
        } catch (Exception e) {
            throw new ExcelDownloadException("Failed to download report for date: " + date, e);
        }
    }
//
//  private EmployeeAttendance mapDtoToEntity(AttendanceRequestDTO dto) {
//        EmployeeAttendance entity = new EmployeeAttendance();
//        // existing mapping
//        entity.setEmployeeId(dto.getEmployeeId());
//        entity.setEmployeeName(dto.getEmployeeName());
//        entity.setShift(dto.getShift());
//        entity.setDate(dto.getDate());
//        entity.setInTime(parseTime(dto.getInTime()));
//        entity.setOutTime(parseTime(dto.getOutTime()));
//        entity.setLateIn(dto.getLateIn());
//        entity.setErlOut(dto.getErlOut());
//        entity.setOverTime(dto.getOverTime());
//        entity.setRemark(dto.getRemark());
//
//        boolean isPresent = (entity.getInTime() != null && entity.getOutTime() != null);
//        entity.setStatus(isPresent ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);
//        entity.setWorkHours(isPresent
//                ? java.time.Duration.between(entity.getInTime(), entity.getOutTime()).toMinutes() / 60.0
//                : 0.0);
//
//        // 🔹 Schedule email reminder if OutTime missing
//        if (entity.getInTime() != null && entity.getOutTime() == null) {
//            long delay = 8; // hours
//            scheduler.schedule(() -> {
//                String email = employeeEmailConfig.getEmailByEmployeeId(entity.getEmployeeId());
//                emailService.sendPunchOutReminder(email, entity.getEmployeeName());
//            }, delay, TimeUnit.HOURS);
//        }
//
//        return entity;
//    }
    private EmployeeAttendance mapDtoToEntity(AttendanceRequestDTO dto) {
        EmployeeAttendance entity = new EmployeeAttendance();

        // existing mapping
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setEmployeeName(dto.getEmployeeName());
        entity.setShift(dto.getShift());
        entity.setDate(dto.getDate());
        entity.setInTime(parseTime(dto.getInTime()));
        entity.setOutTime(parseTime(dto.getOutTime()));
        entity.setLateIn(dto.getLateIn());
        entity.setErlOut(dto.getErlOut());
        entity.setOverTime(dto.getOverTime());
        entity.setRemark(dto.getRemark());

        // ✅ Mark present if at least InTime exists
        boolean isPresent = (entity.getInTime() != null);
        entity.setStatus(isPresent ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT);

        // Calculate work hours only if OutTime exists
        entity.setWorkHours((entity.getInTime() != null && entity.getOutTime() != null)
                ? java.time.Duration.between(entity.getInTime(), entity.getOutTime()).toMinutes() / 60.0
                : 0.0);

        // 🔹 Schedule email reminder if OutTime missing
        if (entity.getInTime() != null && entity.getOutTime() == null) {
            long delay = 8; // hours
            scheduler.schedule(() -> {
                String email = employeeEmailConfig.getEmailByEmployeeId(entity.getEmployeeId());
                emailService.sendPunchOutReminder(email, entity.getEmployeeName());
            }, delay, TimeUnit.HOURS);
        }

        return entity;
    }


    // 🔹 Excel parsing to DTO
    private List<AttendanceRequestDTO> parseExcelToDTO(byte[] excelData, LocalDate date) throws Exception {
        List<AttendanceRequestDTO> attendanceList = new ArrayList<>();
        try (InputStream inputStream = new ByteArrayInputStream(excelData);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String empId = getCellValue(row.getCell(1), evaluator);
                if (empId == null || empId.isBlank()
                        || empId.contains("Total")
                        || empId.contains("Daily")
                        || empId.contains("Config")) continue;

                AttendanceRequestDTO dto = new AttendanceRequestDTO();
                dto.setEmployeeId(empId);
                dto.setEmployeeName(getCellValue(row.getCell(2), evaluator));
                dto.setShift(getCellValue(row.getCell(3), evaluator));
                dto.setInTime(normalizeCellValue(getCellValue(row.getCell(4), evaluator)));
                dto.setLateIn(normalizeCellValue(getCellValue(row.getCell(5), evaluator)));
                dto.setErlOut(normalizeCellValue(getCellValue(row.getCell(6), evaluator)));
                dto.setOutTime(normalizeCellValue(getCellValue(row.getCell(7), evaluator)));
                dto.setWorkOt(normalizeCellValue(getCellValue(row.getCell(8), evaluator)));
                dto.setOverTime(normalizeCellValue(getCellValue(row.getCell(9), evaluator)));
                dto.setRemark(getCellValue(row.getCell(11), evaluator));
                dto.setDate(date);

                attendanceList.add(dto);
            }
        }
        return attendanceList;
    }

    // 🔹 Helpers
    private String normalizeCellValue(String val) {
        if (val == null) return null;
        val = val.trim();
        if (val.isEmpty() || val.equalsIgnoreCase("INTime")
                || val.equalsIgnoreCase("OUTTime") || val.equals("--:--")
                || val.equals("00:00") || val.equals("0")) {
            return null;
        }
        return val;
    }

    private String getCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return null;

        String val = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    : null;
            case FORMULA -> {
                CellType formulaResultType = evaluator.evaluateFormulaCell(cell);
                if (formulaResultType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm"));
                } else if (formulaResultType == CellType.STRING) {
                    yield cell.getStringCellValue().trim();
                } else yield null;
            }
            default -> null;
        };

        return normalizeCellValue(val);
    }

    private LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return null;
        }
    }
}
