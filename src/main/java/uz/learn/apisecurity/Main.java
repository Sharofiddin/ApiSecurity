package uz.learn.apisecurity;

import static spark.Service.SPARK_DEFAULT_PORT;
import static spark.Spark.after;
import static spark.Spark.afterAfter;
import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.internalServerError;
import static spark.Spark.notFound;
import static spark.Spark.post;
import static spark.Spark.secure;
import static spark.Spark.staticFileLocation;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Set;

import org.dalesbred.Database;
import org.dalesbred.result.EmptyResultException;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.util.concurrent.RateLimiter;
import com.nimbusds.jose.JOSEException;

import spark.Request;
import spark.Response;
import spark.Spark;
import uz.learn.apisecurity.controller.AuditController;
import uz.learn.apisecurity.controller.SpaceController;
import uz.learn.apisecurity.controller.UserContorller;
import uz.learn.apisecurity.token.DatabaseTokenStore;
import uz.learn.apisecurity.token.EncryptedJwtTokenStore;
import uz.learn.apisecurity.token.SecureTokenStore;
import uz.learn.apisecurity.token.TokenController;

public class Main {
	private static final String AUDIENCE = "https://localhost:4567";
	private static final String SESSIONS = "/sessions";
	private static final String ERROR = "error";

	public static void main(String[] args) throws URISyntaxException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, JOSEException {
		secure("localhost.p12", "changeit", null, null);
		Spark.port(args.length == 0 ? SPARK_DEFAULT_PORT : Integer.parseInt(args[0]));
		staticFileLocation("/public");
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
		var keyPassword = System.getProperty("keystore.password", "changeit").toCharArray();
		var keystore = KeyStore.getInstance("PKCS12");
		keystore.load(new FileInputStream("keystore.p12"), keyPassword);
		var encKey = keystore.getKey("aes-key", keyPassword);
		before(new CorsFilter(Set.of("https://localhost:9999")));
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
		DatabaseTokenStore databaseTokenStore = new DatabaseTokenStore(database);
		SecureTokenStore tokenStore = new EncryptedJwtTokenStore(encKey, AUDIENCE, databaseTokenStore);
		var tokenController = new TokenController(tokenStore);
		before(userController::authenticate);
		before(tokenController::validateToken);
		
		var auditController = new AuditController(database);
		before(auditController::auditRequestStart);
		afterAfter(auditController::auditRequestEnd);
		
		before(SESSIONS, userController::requireAuthentication);
		post(SESSIONS, tokenController::login);
		delete(SESSIONS, tokenController::logout);

        before("/spaces", userController::requireAuthentication);
		post("/spaces", spaceController::createSpace);
		
		before("/spaces/:spaceId/messages/*", userController.requirePermission("GET", "r"));
		get("/spaces/:spaceId/messages/:msgId", spaceController::readMessage);
		
		before("/spaces/:spaceId/messages", userController.requirePermission("GET", "r"));
		get("/spaces/:spaceId/messages", spaceController::findMessages);
		
		before("/spaces/:spaceId/messages", userController.requirePermission("POST", "w"));
		post("/spaces/:spaceId/messages", spaceController::postMessage);
		
		before("/spaces/:spaceId/messages/:msgId", userController.requirePermission("DELETE", "d"));
		delete("/spaces/:spaceId/messages/:msgId", spaceController::deleteMessage);
		
		before("/spaces/:spaceId/members", userController.requirePermission("POST", "rwd"));
		post("/spaces/:spaceId/members", spaceController::addMember);
		
		post("/users", userController::registerUser);
		
		before("/logs", userController::requireAuthentication);
		get("/logs", auditController::readAuditLog);
		
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
