package com.configserver.hrm.attendanceService.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPunchOutReminder(String toEmail, String employeeName) {
        if (toEmail == null) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Punch-Out Missing Reminder");
        message.setText("Hello " + employeeName + ",\n\n" +
                "You forgot to punch out today. Please update your attendance or contact HR.\n\n" +
                "Regards,\nHR Team");

        mailSender.send(message);
    }
}
