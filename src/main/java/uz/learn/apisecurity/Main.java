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

import spark.Request;
import spark.Response;
import spark.Spark;
import uz.learn.apisecurity.controller.SpaceController;

import static spark.Spark.*;

public class Main {
	public static void main(String[] args) throws URISyntaxException, IOException {
		exception(IllegalArgumentException.class, Main::badRequest);
		exception(JSONException.class, Main::badRequest);
		exception(EmptyResultException.class, (e, req, res)->res.status(404));
		Spark.before((req, res)->{
			if(req.requestMethod().equals("POST") &&
					!"application/json".equals(req.contentType())) {
				halt(415, new JSONObject().put("error", "Only application/json supported").toString());
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
		createTables(database);
		var spaceController = new SpaceController(database);
		post("/spaces", spaceController::createSpace);
		after((request, response) -> response.type("application/json"));
		internalServerError(new JSONObject().put("error", "internal server error").toString());
		notFound(new JSONObject().put("error", "not found").toString());
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
