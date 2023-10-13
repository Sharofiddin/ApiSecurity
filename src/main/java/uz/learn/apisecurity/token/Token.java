package uz.learn.apisecurity.token;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Token {
	public final String username;
	public final Instant expiry;
	public final Map<String, String> attributes;

	public Token(String username, Instant expiry) {
		this.username = username;
		this.expiry = expiry;
		this.attributes = new ConcurrentHashMap<>();
	}

}
