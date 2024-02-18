package uz.learn.apisecurity.token;

import static uz.learn.apisecurity.token.CookieTokenStore.sha256;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.dalesbred.Database;
import org.json.JSONException;
import org.json.JSONObject;

import spark.Request;

public class DatabaseTokenStore implements TokenStore {
	private final Database database;
	private final SecureRandom secureRandom;

	public DatabaseTokenStore(Database database) {
		this.database = database;
		this.secureRandom = new SecureRandom();
		Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::deleteExpiredTokens, 10, 10,
				TimeUnit.MINUTES);
	}

	private String randomId() {
		byte[] bytes = new byte[20]; // 160 bit secure random
		secureRandom.nextBytes(bytes);
		return Base64url.encode(bytes);
	}

	@Override
	public String create(Request request, Token token) {
		String tokenId = randomId();
		String attrs = new JSONObject(token.attributes).toString();

		database.updateUnique("""
				  INSERT INTO tokens(token_id, user_id, expiry, attributes)
				  VALUES(?,?,?,?)
				""", hash(tokenId), token.username, token.expiry, attrs);
		return tokenId;
	}

	@Override
	public Optional<Token> read(Request request, String tokenId) {
		return database.findOptional(this::readToken, """
				SELECT user_id, expiry, attributes
				  FROM tokens WHERE token_id = ?
				""", hash(tokenId));

	}

	@Override
	public void revoke(Request request, String tokenId) {
		database.update("DELETE FROM tokens WHERE token_id = ?", hash(tokenId));
	}

	private Token readToken(ResultSet resultset) throws JSONException, SQLException {
		String username = resultset.getString(1);
		Instant expiry = resultset.getTimestamp(2).toInstant();
		JSONObject json = new JSONObject(resultset.getString(3));
		Token token = new Token(username, expiry);
		for (String key : json.keySet()) {
			token.attributes.put(key, json.getString(key));
		}
		return token;
	}

	public void deleteExpiredTokens() {
		database.update("DELETE FROM tokens WHERE expiry < current_timestamp");
	}
	
	private static String hash(String tokenId) {
		return Base64url.encode(sha256(tokenId));
	}

}
