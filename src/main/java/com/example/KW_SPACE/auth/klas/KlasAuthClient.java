package com.example.KW_SPACE.auth.klas;

public interface KlasAuthClient {

	KlasAuthResult verify(String klasId, String klasPassword);
}
