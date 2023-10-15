package uz.learn.apisecurity.token;

import java.util.Base64;

public class Base64url {
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	public static String encode(byte[] data) {
		return ENCODER.encodeToString(data);
	}

	public static byte[] decode(String data) {
		return DECODER.decode(data);
	}
}
