package com.example.KW_SPACE.auth.klas;

public class KlasAuthServerUnavailableException extends RuntimeException {

	public KlasAuthServerUnavailableException(String message) {
		super(message);
	}

	public KlasAuthServerUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
