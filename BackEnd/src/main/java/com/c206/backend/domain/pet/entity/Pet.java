package com.c206.backend.domain.pet.entity;

import com.c206.backend.domain.pet.entity.enums.PetType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long petId;

    private String image;

    private String name;

    @Enumerated(EnumType.STRING)
    private PetType petType;


}
