package com.example.KW_SPACE.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String studentNumber;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber;

    protected User() {
    }

    public User(String name, String studentNumber, String password, String phoneNumber) {
        this.name = name;
        this.studentNumber = studentNumber;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getStudentNumber() { return studentNumber; }
    public String getPassword() { return password; }
    public String getPhoneNumber() { return phoneNumber; }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
