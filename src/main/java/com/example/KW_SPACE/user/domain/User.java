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
    private String klasId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber;

    protected User() {
    }

    public User(String name, String klasId, String password, String phoneNumber) {
        this.name = name;
        this.klasId = klasId;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getKlasId() { return klasId; }
    public String getPassword() { return password; }
    public String getPhoneNumber() { return phoneNumber; }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
