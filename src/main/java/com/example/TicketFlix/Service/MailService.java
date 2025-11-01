package com.example.TicketFlix.Service;

import com.example.TicketFlix.Models.Ticket;
import com.example.TicketFlix.Models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
@Slf4j
public class MailService {

    @Autowired
    JavaMailSender javaMailSender;
    
    // Simple mail sender method for Kafka consumer
    public void sendSimpleMail(String to, String subject, String body) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
        
        mimeMessageHelper.setFrom("azmaan000@gmail.com");
        mimeMessageHelper.setTo(to);
        mimeMessageHelper.setText(body);
        mimeMessageHelper.setSubject(subject);
        
        javaMailSender.send(mimeMessage);
        log.info("Email sent successfully to: {}", to);
    }

    public String sendMail(User user, Ticket ticket, String allottedSeats) throws MessagingException {
        String body = "Hi, " + user.getName() + "\n\nThis is to confirm your ticket booking for the movie:- " + ticket.getMovieName()
                + "\nTicket id - " + ticket.getTicketId() + "\nBooked Seats - " + allottedSeats + "\nAmount of rupees - "
                + ticket.getTotalAmount() + "\n\n\n" + "Thank you for using our services, have a wonderful day!";

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);

        mimeMessageHelper.setFrom("azmaan000@gmail.com");
        mimeMessageHelper.setTo(user.getEmail());
        mimeMessageHelper.setText(body);
        mimeMessageHelper.setSubject("Confirmation for your ticket booking");
        javaMailSender.send(mimeMessage);
        log.info("Ticket booked Successfully");
        return "Ticket is successfully booked";
    }

    public String cancelMail(User user, Ticket ticket, String cancelledSeats) throws MessagingException {
        String body = "Hi,  "+user.getName()+"\n\nThis is to confirm your booking cancellation."+"" +
                "\nTicket id - "+ticket.getTicketId()+"\nCancelled Seats - "+cancelledSeats+"\n" +
                "Amount of rupees - "+ticket.getTotalAmount()+" will be refunded in to your account in 6-7 working days" +
                "\n\n\n"+"Have a wonderful day !";


        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper=new MimeMessageHelper(mimeMessage,true);
        mimeMessageHelper.setFrom("azmaan000@gmail.com");
        mimeMessageHelper.setTo(user.getEmail());
        mimeMessageHelper.setText(body);
        mimeMessageHelper.setSubject("Confirmation for your ticket cancellation");
        javaMailSender.send(mimeMessage);

        return ("Tickets has been successfully cancelled !");
    }
}
