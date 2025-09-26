package com.configserver.hrm.leaveService.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employee_leave")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeLeave {

    @Id
    @GeneratedValue
    @Column(length = 36)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeaveType leaveType;

    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeaveStatus status;

    private String approvedBy;
    private LocalDateTime approvedOn;
    private LocalDateTime appliedOn;



}
