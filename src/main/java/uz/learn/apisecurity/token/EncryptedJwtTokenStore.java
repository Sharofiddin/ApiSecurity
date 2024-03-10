package uz.learn.apisecurity.token;

import java.security.Key;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.crypto.SecretKey;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;

import spark.Request;

public class EncryptedJwtTokenStore implements SecureTokenStore {

	private final SecretKey key;
	private final String audience;
	private final DatabaseTokenStore allowListTokenStore;

	public EncryptedJwtTokenStore(Key key, String audience, DatabaseTokenStore allowListTokenStore) {
		this.key = (SecretKey)key;
		this.audience = audience;
		this.allowListTokenStore = allowListTokenStore;
	}

	@Override
	public String create(Request request, Token token) {
		Token allowListToken = new Token(token.username, token.expiry);
		String jwtId = allowListTokenStore.create(request, allowListToken);
		JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
				.jwtID(jwtId)
				.subject(token.username).audience(audience)
				.expirationTime(Date.from(token.expiry)).claim("attrs", token.attributes).build();
		JWEHeader jweHeader = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A128CBC_HS256);
		EncryptedJWT encryptedJWT = new EncryptedJWT(jweHeader, jwtClaimsSet);
		try {
			var encrypter = new DirectEncrypter(key);
			encryptedJWT.encrypt(encrypter);
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}
		return encryptedJWT.serialize();
	}

	@Override
	public Optional<Token> read(Request request, String tokenId) {
		try {
			var jwt = EncryptedJWT.parse(tokenId);
			var decrypter = new DirectDecrypter(key);
			jwt.decrypt(decrypter);

			JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
			if (!jwtClaimsSet.getAudience().contains(audience)) {
				throw new JOSEException("Incorrect audience");
			}
			String jwtid = jwtClaimsSet.getJWTID();
			if(allowListTokenStore.read(request, jwtid).isEmpty()) {
				return Optional.empty();
			}
			Instant expiry = jwtClaimsSet.getExpirationTime().toInstant();
			String subject = jwtClaimsSet.getSubject();
			Token token = new Token(subject, expiry);
			var ignore = Set.of("sub", "iss", "aud");
			Map<String, Object> attrs = jwtClaimsSet.getJSONObjectClaim("attrs");
			attrs.forEach((k, v) -> {
				if (!ignore.contains(k)) {
					token.attributes.put(k, (String) v);
				}
			});
			return Optional.of(token);
		} catch (ParseException | JOSEException e) {
			return Optional.empty();
		}
	}

	@Override
	public void revoke(Request request, String tokenId) {
		try {
			var jwt = EncryptedJWT.parse(tokenId);
			var decrypter = new DirectDecrypter(key);
			jwt.decrypt(decrypter);
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			allowListTokenStore.revoke(request, claims.getJWTID());
		} catch (ParseException | JOSEException e) {
		   throw new IllegalArgumentException("Invalid token",e);
		}
	}

}
