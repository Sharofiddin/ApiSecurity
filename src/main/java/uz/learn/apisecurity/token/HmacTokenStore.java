package uz.learn.apisecurity.token;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.crypto.Mac;

import spark.Request;

public class HmacTokenStore implements TokenStore {

	private final Key key;
	private final TokenStore delegate;

	public HmacTokenStore(TokenStore tokenStore, Key key) {
		this.delegate = tokenStore;
		this.key = key;
	}

	@Override
	public String create(Request request, Token token) {
		var tokenId = delegate.create(request, token);
		var tag = hmac(tokenId);
		return tokenId + '.' + Base64url.encode(tag);
	}

	private byte[] hmac(String tokenId) {
		try {
			var mac = Mac.getInstance(key.getAlgorithm());
			mac.init(key);
			return mac.doFinal(tokenId.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public Optional<Token> read(Request request, String tokenId) {
		String realToken = getRealToken(tokenId);
		if (realToken == null)
			return Optional.empty();
		return delegate.read(request, realToken);
	}

	private String getRealToken(String tokenId) {
		int index = tokenId.indexOf(".");
		if (index == -1)
			return null;
		String realToken = tokenId.substring(0, index);
		byte[] provided = Base64url.decode(tokenId.substring(index + 1));
		if (!MessageDigest.isEqual(provided, hmac(realToken))) {
			return null;
		}
		return realToken;
	}

	@Override
	public void revoke(Request request, String tokenId) {
		String realToken = getRealToken(tokenId);
		if (realToken != null)
			delegate.revoke(request, realToken);
	}

}
