package com.example.demo.filemanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
@Entity
@Table(name = "users",schema="TRAINING_NISHA")

public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long UserId;
    private String firstName;
    private String lastName;
    private String contact;
    private String email;
    private String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(UserId, user.UserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(UserId);
    }


}

