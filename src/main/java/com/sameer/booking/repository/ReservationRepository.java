package com.sameer.booking.repository;

import com.sameer.booking.entity.Reservation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    @EntityGraph(attributePaths = {"user", "resource"})
    Optional<Reservation> findWithUserAndResourceById(Long id);
}
