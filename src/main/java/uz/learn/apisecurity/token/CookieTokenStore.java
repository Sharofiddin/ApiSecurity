package uz.learn.apisecurity.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import spark.Request;
import spark.Session;

public class CookieTokenStore implements TokenStore {

	@Override
	public String create(Request request, Token token) {
		var session = request.session(false);
		if(session != null) {
			session.invalidate();
		}
        session = request.session(true);
		session.attribute("username", token.username);
		session.attribute("expiry", token.expiry);
		session.attribute("attrs", token.attributes);

		return Base64url.encode(sha256(session.id()));
	}

	private byte[] sha256(String tokenId) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			return sha256.digest(tokenId.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Optional<Token> read(Request request, String tokenId) {
		Session session = request.session(false);
		if (session == null) {
			return Optional.empty();
		}
		var provided = Base64url.decode(tokenId);
		var computed = sha256(session.id());
		if(!MessageDigest.isEqual(provided, computed)) {
			return Optional.empty();
		}
		Token token = new Token(session.attribute("username"), session.attribute("expiry"));
		token.attributes.putAll(session.attribute("attrs"));
		return Optional.of(token);
	}
}
