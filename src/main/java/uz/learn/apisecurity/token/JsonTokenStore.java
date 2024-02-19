package uz.learn.apisecurity.token;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;

import spark.Request;

public class JsonTokenStore implements TokenStore {

	@Override
	public String create(Request request, Token token) {
		JSONObject json = new JSONObject();
		json.put("sub", token.username);
		json.put("exp", token.expiry.getEpochSecond());
		json.put("attrs", token.attributes);
		byte[] jsonBytes = json.toString().getBytes();
		return Base64url.encode(jsonBytes);
	}

	@Override
	public Optional<Token> read(Request request, String tokenId) {
		try {
			byte[] decoded = Base64url.decode(tokenId);
			JSONObject json = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
			String username = json.getString("sub");
			Instant expiry = Instant.ofEpochSecond(json.getLong("exp"));
			JSONObject attribites = json.getJSONObject("attrs");
			Token token = new Token(username, expiry);
			for (String key : attribites.keySet()) {
				token.attributes.put(key, attribites.getString(key));
			}
			return Optional.of(token);
		} catch (JSONException e) {
			return Optional.empty();
		}
	}

	@Override
	public void revoke(Request request, String tokenId) {
		// TODO Auto-generated method stub

	}

}
