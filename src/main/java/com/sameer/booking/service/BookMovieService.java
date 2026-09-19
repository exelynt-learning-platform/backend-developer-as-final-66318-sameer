package com.sameer.booking.service;

import com.sameer.booking.dto.BookingRequest;
import com.sameer.booking.entity.Booking;
import com.sameer.booking.entity.Movie;
import com.sameer.booking.repository.BookingRepository;
import com.sameer.booking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookMovieService {

    private final MovieRepository movieRepository;
    private final BookingRepository bookingRepository;

    public Booking bookTicket(BookingRequest request) {

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new RuntimeException("Movie Not Found"));
        if (request.getNumberOfTickets() <= 0) {
            throw new RuntimeException("Number Of Tickets must be greater than the 0");
        }

        if (movie.getSeatsAvailable() < request.getNumberOfTickets()) {
            throw new RuntimeException("Not enough seats are available");
        }

        movie.setSeatsAvailable(movie.getSeatsAvailable() - request.getNumberOfTickets());

        Booking booking = new Booking();
        booking.setMovie(movie);
        booking.setNumberOfTickets(request.getNumberOfTickets());
        booking.setBookingTime(LocalDateTime.now());

        movieRepository.save(movie);
        return bookingRepository.save(booking);
    }

}
