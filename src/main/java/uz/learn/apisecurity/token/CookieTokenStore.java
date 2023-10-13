package uz.learn.apisecurity.token;

import java.util.Optional;

import spark.Request;
import spark.Session;

public class CookieTokenStore implements TokenStore {

	@Override
	public String create(Request request, Token token) {
		var session = request.session(true);

		session.attribute("username", token.username);
		session.attribute("expiry", token.expiry);
		session.attribute("attrs", token.attributes);

		return session.id();
	}

	@Override
	public Optional<Token> read(Request request, String tokenId) {
		Session session = request.session(false);
		if (session == null) {
			return Optional.empty();
		}
		Token token = new Token(session.attribute("username"), session.attribute("expiry"));
		token.attributes.putAll(session.attribute("attrs"));
		return Optional.of(token);
	}

}
