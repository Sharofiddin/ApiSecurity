package uz.learn.apisecurity.token;

import java.security.Key;
import java.util.Optional;

import software.pando.crypto.nacl.SecretBox;
import spark.Request;

public class EncryptedTokenStore implements TokenStore {
    
	private final TokenStore deleate;
	private final Key encryptionKey;
	
	
	
	public EncryptedTokenStore(TokenStore deleate, Key encryptionKey) {
		this.deleate = deleate;
		this.encryptionKey = encryptionKey;
	}

	@Override
	public String create(Request request, Token token) {
		String tokenId = deleate.create(request, token);
		return SecretBox.encrypt(encryptionKey, tokenId).toString();
	}

	@Override
	public Optional<Token> read(Request request, String tokenId) {
		SecretBox box = SecretBox.fromString(tokenId);
		String origTokenId = box.decryptToString(encryptionKey); 
		return deleate.read(request, origTokenId);
	}

	@Override
	public void revoke(Request request, String tokenId) {
		SecretBox box = SecretBox.fromString(tokenId);
		deleate.revoke(request, box.decryptToString(encryptionKey));
	}

}
