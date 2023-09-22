package uz.learn.apisecurity;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.dalesbred.Database;
import org.dalesbred.result.EmptyResultException;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.util.concurrent.RateLimiter;

import spark.Request;
import spark.Response;
import uz.learn.apisecurity.controller.SpaceController;
import uz.learn.apisecurity.controller.UserContorller;

import static spark.Spark.*;

public class Main {
	private static final String ERROR = "error";

	public static void main(String[] args) throws URISyntaxException, IOException {
		exception(IllegalArgumentException.class, Main::badRequest);
		exception(JSONException.class, Main::badRequest);
		exception(EmptyResultException.class, (e, req, res)->res.status(404));
		RateLimiter rateLimiter = RateLimiter.create(2.d);
		before((req, res)->{
			if(!rateLimiter.tryAcquire()) {
				res.header("Retry-After", "2");
				halt(429);
			}
		});
		before((req, res)->{
			if(req.requestMethod().equals("POST") &&
					!"application/json".equals(req.contentType())) {
				halt(415, new JSONObject().put(ERROR, "Only application/json supported").toString());
			}
		});
		afterAfter((req, res)->{
		res.type("application/json;charset=utf-8");	
		res.header("X-Content-Type-Options", "nosniff");
		res.header("X-Frame-Options", "DENY");
		res.header("X-XSS-Protection", "0");
		res.header("Cache-Control", "np-store");
		res.header("Content-Security-Policy", "default-src 'none', frame-ancestors 'none'; sandbox");
		res.header("Server", "");
		});
		var datasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter", "password");
		var database = Database.forDataSource(datasource);
		datasource = JdbcConnectionPool.create("jdbc:h2:mem:natter", "natter_api_user", "password");
		createTables(database);
		database = Database.forDataSource(datasource);
		var spaceController = new SpaceController(database);
		var userController = new UserContorller(database);
		before(userController::authenticate);
		post("/spaces", spaceController::createSpace);
		post("/spaces/:spaceId/messages", spaceController::postMessage);
		post("/users", userController::registerUser);
		after((request, response) -> response.type("application/json"));
		internalServerError(new JSONObject().put(ERROR, "internal server error").toString());
		notFound(new JSONObject().put(ERROR, "not found").toString());
	}

	private static void createTables(@NotNull Database database) throws URISyntaxException, IOException {
		var path = Paths.get(Main.class.getResource("/schema.sql").toURI());
		database.update(Files.readString(path));
	}
	
	public static void badRequest(Exception ex, Request req, Response res) {
		res.status(400);
		res.body("{\"error\": \"" + ex.getMessage() + "\"}" );
	}
}
