package com.example.bookingservice.repository;

import com.example.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByRideId(Long rideId);

    List<Booking> findByPassengerUsername(String passengerUsername);

    List<Booking> findByPassengerUsernameAndStatus(String passengerUsername, Booking.BookingStatus status);

    Optional<Booking> findByIdAndPassengerUsername(Long id, String passengerUsername);

    boolean existsByRideIdAndStatus(Long rideId, Booking.BookingStatus status);
}
