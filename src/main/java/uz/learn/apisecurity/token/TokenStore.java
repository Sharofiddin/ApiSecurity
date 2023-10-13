package uz.learn.apisecurity.token;

import java.util.Optional;

import spark.Request;

public interface TokenStore {
  String create(Request request, Token token);
  Optional<Token> read(Request request, String tokenId);
}
