package com.example.KW_SPACE.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
		name = "users",
		uniqueConstraints = {
			@UniqueConstraint(name = "uk_users_klas_id", columnNames = "klas_id"),
			@UniqueConstraint(name = "uk_users_phone_number", columnNames = "phone_number")
		}
)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "klas_id", nullable = false, length = 20)
	private String klasId;

	@Column(nullable = false, length = 50)
	private String name;

	@Column(name = "phone_number", length = 20)
	private String phoneNumber;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role = UserRole.USER;

	@Column(name = "token_version", nullable = false)
	private int tokenVersion;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "last_login_at")
	private LocalDateTime lastLoginAt;

	protected User() {
	}

	private User(String klasId, String name, String phoneNumber, String passwordHash) {
		this.klasId = klasId;
		this.name = name;
		this.phoneNumber = phoneNumber;
		this.passwordHash = passwordHash;
	}

	public static User create(String klasId, String name, String phoneNumber, String passwordHash) {
		return new User(klasId, name, phoneNumber, passwordHash);
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public void changePhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public void resetPassword(String passwordHash) {
		this.passwordHash = passwordHash;
		this.tokenVersion++;
	}

	public Long getId() {
		return id;
	}

	public String getKlasId() {
		return klasId;
	}

	public String getName() {
		return name;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public int getTokenVersion() {
		return tokenVersion;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public LocalDateTime getLastLoginAt() {
		return lastLoginAt;
	}
}
