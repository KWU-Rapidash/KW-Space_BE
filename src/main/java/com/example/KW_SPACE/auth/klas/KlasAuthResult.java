package com.example.KW_SPACE.auth.klas;

public record KlasAuthResult(
		boolean authenticated,
		String name,
		String klasId
) {

	public static KlasAuthResult success(String klasId, String name) {
		return new KlasAuthResult(true, name, klasId);
	}

	public static KlasAuthResult failure() {
		return new KlasAuthResult(false, null, null);
	}
}
