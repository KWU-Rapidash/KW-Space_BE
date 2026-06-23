package com.example.KW_SPACE.auth.security;

import com.example.KW_SPACE.user.domain.User;
import com.example.KW_SPACE.user.domain.UserRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

	private final Long id;
	private final String passwordHash;
	private final UserRole role;
	private final int tokenVersion;

	private CustomUserDetails(Long id, String passwordHash, UserRole role, int tokenVersion) {
		this.id = id;
		this.passwordHash = passwordHash;
		this.role = role;
		this.tokenVersion = tokenVersion;
	}

	public static CustomUserDetails from(User user) {
		return new CustomUserDetails(user.getId(), user.getPasswordHash(), user.getRole(), user.getTokenVersion());
	}

	public Long getId() {
		return id;
	}

	public UserRole getRole() {
		return role;
	}

	public int getTokenVersion() {
		return tokenVersion;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return String.valueOf(id);
	}
}
