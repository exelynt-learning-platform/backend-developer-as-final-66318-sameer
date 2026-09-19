package com.sameer.booking.controller;

import com.sameer.booking.dto.BookingRequest;
import com.sameer.booking.entity.Booking;
import com.sameer.booking.entity.Movie;
import com.sameer.booking.service.BookMovieService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RequestMapping("/api/Booking")
@RestController
public class BookMovieController {

    private final BookMovieService bookingService;

    @PostMapping
    @Operation(summary = "Book the tickets for the movie.")
    public ResponseEntity<Booking> bookTicket(@RequestBody BookingRequest request){

        Booking booking = bookingService.bookTicket(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(booking);
    }
// I updated the code sir I Got where was the problem sir

}
