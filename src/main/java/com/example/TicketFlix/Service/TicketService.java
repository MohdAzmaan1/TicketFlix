package com.example.TicketFlix.Service;

import com.example.TicketFlix.EntryDTOs.DeleteTicketEntryDTO;
import com.example.TicketFlix.EntryDTOs.TicketEntryDTO;
import com.example.TicketFlix.Kafka.KafkaProducerService;
import com.example.TicketFlix.Models.Show;
import com.example.TicketFlix.Models.ShowSeat;
import com.example.TicketFlix.Models.Ticket;
import com.example.TicketFlix.Models.User;
import com.example.TicketFlix.Repository.ShowRepository;
import com.example.TicketFlix.Repository.TicketRepository;
import com.example.TicketFlix.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.mail.MessagingException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TicketService {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    ShowRepository showRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    MailService mailService;

    @Autowired
    RedisService redisService;

    @Autowired
    KafkaProducerService kafkaProducerService;

    public String addTicket(TicketEntryDTO ticketEntryDTO) throws InterruptedException, MessagingException {
        List<String> requestedSeatsToBook = ticketEntryDTO.getRequestedSeats();
        List<RLock> locks = new ArrayList<>();
        boolean allLocksAcquired = false;
        try {
            for(String seatNumber: requestedSeatsToBook){
                String lockKey = "seat-lock" + "-" + ticketEntryDTO.getShowId() + "-"+ seatNumber;
                RLock lock = redissonClient.getLock(lockKey);
                boolean acquired = lock.tryLock(5, 3, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new InterruptedException("Seat " + seatNumber + " is currently being booked by another user. Please try again.");
                }
                locks.add(lock);
            }
            allLocksAcquired = true;
            //1. Create TicketEntity from entryDto : Convert DTO ---> Entity
            Ticket ticket = new Ticket();

            //Validation : Check if the requested seats are available or not ?
            boolean isValidRequest = checkValidityOfRequestedSeats(ticketEntryDTO);

            if (!isValidRequest) {
                log.error("Requested seats are not available");
                throw new MessagingException("Requested seats are not available");
            }


            Show show = showRepository.findById(ticketEntryDTO.getShowId()).get();
            List<ShowSeat> seatList = show.getListOfShowSeats();
            List<String> requestedSeats = ticketEntryDTO.getRequestedSeats();

            //Calculate the total amount :
            int totalAmount = 0;
            for (ShowSeat showSeat : seatList) {

                if (requestedSeats.contains(showSeat.getSeatNumber())) {
                    totalAmount = totalAmount + showSeat.getPrice();
                    showSeat.setBooked(true);
                    showSeat.setBookedAt(new Date());
                }
            }

            //Setting the other attributes for the ticketEntity
            ticket.setTotalAmount(totalAmount);
            ticket.setMovieName(show.getMovie().getMovieName());
            ticket.setShowDate(show.getShowDate());
            ticket.setShowTime(show.getShowTime());
            ticket.setTheaterName(show.getTheater().getName());


            //We need to set that string that talked about requested Seats
            String allottedSeats = getAllowedSeatsFromShowSeats(requestedSeats);
            ticket.setBookedSeat(allottedSeats);


            //Setting the foreign key attributes
            User user = userRepository.findById(ticketEntryDTO.getUserId()).get();
            ticket.setUser(user);
            ticket.setShow(show);

            ticket = ticketRepository.save(ticket);

            //Save the parent
            List<Ticket> ticketList = show.getListOfBookedTickets();
            ticketList.add(ticket);
            show.setListOfBookedTickets(ticketList);
            showRepository.save(show);


            List<Ticket> ticketList1 = user.getBookedTickets();
            ticketList1.add(ticket);
            user.setBookedTickets(ticketList1);
            userRepository.save(user);

            redisService.increaseMovieCounter(ticket.getMovieName());
            
            // Publish ticket booking event to Kafka
            kafkaProducerService.publishTicketBookingEvent(ticket, user);
            
            // Send email asynchronously via Kafka
            String subject = "Confirmation for your ticket booking";
            String body = "Hi, " + user.getName() + "\n\nThis is to confirm your ticket booking for the movie:- " + ticket.getMovieName()
                    + "\nTicket id - " + ticket.getTicketId() + "\nBooked Seats - " + allottedSeats + "\nAmount of rupees - "
                    + ticket.getTotalAmount() + "\n\n\n" + "Thank you for using our services, have a wonderful day!";
            kafkaProducerService.publishEmailNotification(user.getEmail(), subject, body);
            
            log.info("Ticket booked successfully. Email notification sent via Kafka for ticket ID: {}", ticket.getTicketId());
            return "Ticket is successfully booked. Confirmation email will be sent shortly.";
        }finally {
            if(allLocksAcquired){
                for(int i=0;i< locks.size();i++){
                    try {
                        locks.get(i).unlock();
                    } catch (Exception e) {
                        log.error("Failed to release lock for seat: " + requestedSeatsToBook.get(i), e);
                    }
                }
            }
        }
    }

    private String getAllowedSeatsFromShowSeats(List<String> requestedSeats){

        String result = "";
        for(int i = 0; i < requestedSeats.size();i++){
            if(i == requestedSeats.size() - 1){
                result = result + requestedSeats.get(i);
            }
            else
                result = result + requestedSeats.get(i) + ",";
        }
        return result;
    }


    private boolean checkValidityOfRequestedSeats(TicketEntryDTO ticketEntryDTO){

        int showId = ticketEntryDTO.getShowId();

        List<String> requestedSeats = ticketEntryDTO.getRequestedSeats();

        Show show = showRepository.findById(showId).get();

        List<ShowSeat> listOfSeats = show.getListOfShowSeats();

        //Iterating over the list Of Seats for that particular show
        for(ShowSeat showSeat : listOfSeats){

            String seatNo = showSeat.getSeatNumber();

            if(requestedSeats.contains(seatNo)){

                if(showSeat.isBooked()){
                    return false; //Since this seat cant be occupied : returning false
                }
            }
        }
        //All the seats requested were available
        return true;
    }

    public String cancelTicket(DeleteTicketEntryDTO deleteTicketEntryDTO)throws Exception {
        Ticket ticket = ticketRepository.findById(deleteTicketEntryDTO.getTicketId()).get();
        String ticketsToBeDeleted = ticket.getBookedSeat();
        String cancelledSeats = "";

        String [] currSeats = ticketsToBeDeleted.split(",");

        Show show = ticket.getShow();
        List<ShowSeat> showSeatList = show.getListOfShowSeats();

        cancelBookingOfSeats(currSeats,showSeatList);

        for(int i = 0; i < currSeats.length; i++){
            if(i == currSeats.length - 1)
                cancelledSeats += currSeats[i];
            else
                cancelledSeats += currSeats[i] +",";
        }

        showRepository.save(show);

        User user = ticket.getUser();
        redisService.decreaseCounter(ticket.getMovieName());
        
        // Publish ticket cancellation event to Kafka
        kafkaProducerService.publishTicketCancellationEvent(ticket, user);
        
        // Send cancellation email asynchronously via Kafka
        String subject = "Confirmation for your ticket cancellation";
        String body = "Hi, " + user.getName() + "\n\nThis is to confirm your booking cancellation.\n"
                + "Ticket id - " + ticket.getTicketId() + "\nCancelled Seats - " + cancelledSeats + "\n"
                + "Amount of rupees - " + ticket.getTotalAmount() + " will be refunded in to your account in 6-7 working days"
                + "\n\n\n" + "Have a wonderful day!";
        kafkaProducerService.publishEmailNotification(user.getEmail(), subject, body);
        
        log.info("Ticket cancelled successfully. Email notification sent via Kafka for ticket ID: {}", ticket.getTicketId());
        return "Tickets have been successfully cancelled. Confirmation email will be sent shortly.";
    }

    private void cancelBookingOfSeats(String [] currSeats, List<ShowSeat> showSeatList) {
        for(ShowSeat showSeat : showSeatList){
            String seatNo = showSeat.getSeatNumber();
            if(Arrays.asList(currSeats).contains(seatNo)){
                showSeat.setBookedAt(null);
                showSeat.setBooked(false);
            }
        }
    }
}
