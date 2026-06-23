package com.example.KW_SPACE.auth.klas;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FakeKlasAuthClient implements KlasAuthClient {

	private static final String DEFAULT_KLAS_ID = "2025404000";
	private static final String DEFAULT_PASSWORD = "valid-klas-password";
	private static final String DEFAULT_NAME = "테스트사용자";

	private final Map<String, FakeKlasAccount> accounts;
	private final boolean serverUnavailable;

	public FakeKlasAuthClient() {
		this(Map.of(DEFAULT_KLAS_ID, new FakeKlasAccount(DEFAULT_PASSWORD, DEFAULT_NAME)), false);
	}

	public FakeKlasAuthClient(Map<String, FakeKlasAccount> accounts) {
		this(accounts, false);
	}

	private FakeKlasAuthClient(Map<String, FakeKlasAccount> accounts, boolean serverUnavailable) {
		this.accounts = Map.copyOf(accounts);
		this.serverUnavailable = serverUnavailable;
	}

	public static FakeKlasAuthClient unavailable() {
		return new FakeKlasAuthClient(Map.of(), true);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public KlasAuthResult verify(String klasId, String klasPassword) {
		if (serverUnavailable) {
			throw new KlasAuthServerUnavailableException("KLAS authentication server is unavailable");
		}

		FakeKlasAccount account = accounts.get(klasId);
		if (account == null || !Objects.equals(account.password(), klasPassword)) {
			return KlasAuthResult.failure();
		}

		return KlasAuthResult.success(klasId, account.name());
	}

	public record FakeKlasAccount(String password, String name) {
	}

	public static class Builder {

		private final Map<String, FakeKlasAccount> accounts = new HashMap<>();

		public Builder account(String klasId, String password, String name) {
			accounts.put(klasId, new FakeKlasAccount(password, name));
			return this;
		}

		public FakeKlasAuthClient build() {
			return new FakeKlasAuthClient(accounts);
		}
	}
}
