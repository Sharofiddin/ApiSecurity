package uz.learn.apisecurity.token;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import spark.Request;

public class SignedJwtTokenStore implements TokenStore {
    
	private final JWSSigner jwsSigner;
	private final JWSVerifier jwsVerifier;
	private final JWSAlgorithm algorithm;
	private final String audience;
	
	
	
	public SignedJwtTokenStore(JWSSigner jwsSigner, JWSVerifier jwsVerifier, JWSAlgorithm algorithm, String audience) {
		this.jwsSigner = jwsSigner;
		this.jwsVerifier = jwsVerifier;
		this.algorithm = algorithm;
		this.audience = audience;
	}

	@Override
	public String create(Request request, Token token) {
		JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder().subject(token.username)
		.audience(audience)
		.expirationTime(Date.from(token.expiry))
		.claim("attrs", token.attributes)
		.build();
		JWSHeader jwsHeader = new JWSHeader(algorithm);
		SignedJWT signedJWT = new SignedJWT(jwsHeader, jwtClaimsSet);
		try {
			signedJWT.sign(jwsSigner);
			return signedJWT.serialize();
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<Token> read(Request request, String tokenId) {
		try {
			SignedJWT jwt = SignedJWT.parse(tokenId);
			
			if(!jwt.verify(jwsVerifier)) {
				throw new JOSEException("Invalid signature");
			}
			
			JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
			if(!jwtClaimsSet.getAudience().contains(audience)) {
				throw new JOSEException("Incorrect audience");
			}
			
			Instant expiry = jwtClaimsSet.getExpirationTime().toInstant();
			String subject = jwtClaimsSet.getSubject();
			Token token = new Token(subject, expiry);
			Map<String, Object> attrs = jwtClaimsSet.getJSONObjectClaim("attrs");
			attrs.forEach((k,v) -> token.attributes.put(k, (String)v));
			return Optional.of(token);
		} catch (ParseException | JOSEException e) {
			return Optional.empty();
		}
	}

	@Override
	public void revoke(Request request, String tokenId) {

	}

}
