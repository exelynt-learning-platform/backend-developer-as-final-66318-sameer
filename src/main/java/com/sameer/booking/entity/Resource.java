package com.sameer.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // e.g. ROOM, VEHICLE, EQUIPMENT

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String location;

    @Builder.Default
    @Column(nullable = false)
    private boolean available = true;

    @OneToMany(mappedBy = "resource", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();
}
