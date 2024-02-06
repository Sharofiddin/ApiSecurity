package uz.learn.apisecurity;

import static spark.Spark.halt;

import java.util.Set;

import spark.Filter;
import spark.Request;
import spark.Response;

public class CorsFilter implements Filter {
    
	
	private final Set<String> allowedOrigins;
	public CorsFilter(Set<String> allowedOrigins) {
	  this.allowedOrigins = allowedOrigins;
	}
	@Override
	public void handle(Request request, Response response) throws Exception {
	   String origin = request.headers("Origin");
	   if(origin != null && allowedOrigins.contains(origin)) {
		   response.header("Access-Control-Allow-Origin", origin);
		   response.header("Origin", "vary");
	   }
	   if(isPreflightRequest(request)) {
		   if(origin == null || !allowedOrigins.contains(origin)) {
			   halt(403);
		   }
		   response.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
		   response.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
		   halt(204);
	   }
	}

	   private boolean isPreflightRequest(Request request) {
		   return "OPTIONS".equals(request.requestMethod()) && request.headers().contains("Access-Control-Request-Method");
	   }
}
