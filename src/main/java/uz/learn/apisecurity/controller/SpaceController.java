package uz.learn.apisecurity.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.dalesbred.Database;
import org.json.JSONArray;
import org.json.JSONObject;

import spark.Request;
import spark.Response;

public class SpaceController {
	private static final String SPACE_ID = ":spaceId";
	private final Database database;

	public SpaceController(Database database) {
		this.database = database;
	}

	public JSONObject createSpace(Request request, Response response) {
		var json = new JSONObject(request.body());
		var owner = json.getString("owner");
		var subject = request.attribute("subject");
		if(!owner.equals(subject)) {
			throw new IllegalArgumentException("owner must match authenticated user");
		}
		if( !owner.matches("[a-zA-Z][a-zA-Z0-9]{1,29}")) {
			throw new IllegalArgumentException("invalid username " + owner);
		}
		var spaceName = json.getString("name");
		if( spaceName.length() > 255) {
			throw new IllegalArgumentException("Space name is too long");
		}
		return database.withTransaction(tx-> {
			var spaceId = database.findUniqueLong("SELECT NEXT VALUE FOR space_id_seq");
			database.updateUnique(
					"""
					  INSERT INTO spaces(space_id, name, owner) 
					  VALUES(?, ?, ?);
					""",
					spaceId, spaceName, owner);
			database.updateUnique(
					"""
					INSERT INTO permissions( space_id, user_id, perms)
					VALUES (?,?,?)
					""",spaceId, owner, "rwd" );
			response.status(201);
			response.header("Location", "/spaces/" + spaceId);
			return new JSONObject().put("name", spaceName).put("uri", "/spaces/" + spaceId);
		});
	}
	
	public JSONObject postMessage(Request request, Response response) {
	    var spaceId = Long.parseLong(request.params(SPACE_ID));
	    var json = new JSONObject(request.body());
	    var user = json.getString("author");
	    if(!user.equals(request.attribute("subject"))) {
	    	throw new IllegalArgumentException("Author must match authenticated user");
	    }
	    if (!user.matches("[a-zA-Z][a-zA-Z0-9]{0,29}")) {
	      throw new IllegalArgumentException("invalid username");
	    }
	    var message = json.getString("message");
	    if (message.length() > 1024) {
	      throw new IllegalArgumentException("message is too long");
	    }

	    return database.withTransaction(tx -> {
	      var msgId = database.findUniqueLong(
	          "SELECT NEXT VALUE FOR msg_id_seq;");
	      database.updateUnique(
	          "INSERT INTO messages(space_id, msg_id, msg_time," +
	              "author, msg_text) " +
	              "VALUES(?, ?, current_timestamp, ?, ?)",
	          spaceId, msgId, user, message);

	      response.status(201);
	      var uri = "/spaces/" + spaceId + "/messages/" + msgId;
	      response.header("Location", uri);
	      return new JSONObject().put("uri", uri);
	    });
	  }

	  public Message readMessage(Request request, Response response) {
	    var spaceId = Long.parseLong(request.params(SPACE_ID));
	    var msgId = Long.parseLong(request.params(":msgId"));

	    var message = database.findUnique(Message.class,
	        "SELECT space_id, msg_id, author, msg_time, msg_text " +
	            "FROM messages WHERE msg_id = ? AND space_id = ?",
	        msgId, spaceId);

	    response.status(200);
	    return message;
	  }
      
	  public JSONObject deleteMessage(Request request, Response response) {
		    var spaceId = Long.parseLong(request.params(SPACE_ID));
		    var msgId = Long.parseLong(request.params(":msgId"));

		    database.updateUnique(
		        "DELETE FROM messages WHERE msg_id = ? AND space_id = ?",
		        msgId, spaceId);
		    response.status(204);
		    return new JSONObject();
		  }
	  
	  public JSONObject addMember(Request request, Response response) {
		  var json = new JSONObject(request.body());
		  var username = json.getString("username");
		  var spaceId = request.params(SPACE_ID);
		  var perms = json.getString("permissions");
		  if(!perms.matches("r?w?d?")) {
			  throw new IllegalArgumentException("invalid permissions");
		  }
		  database.update(
		     """
                INSERT INTO Permissions (space_id, user_id, perms) 
                VALUES(?,?,?)
             """, 
            spaceId, username, perms);
		  return new JSONObject().put("username", username).put("permissions", perms);
		  
	  }
	  
	  public JSONArray findMessages(Request request, Response response) {
	    var since = Instant.now().minus(1, ChronoUnit.DAYS);
	    if (request.queryParams("since") != null) {
	      since = Instant.parse(request.queryParams("since"));
	    }
	    var spaceId = Long.parseLong(request.params(SPACE_ID));

	    var messages = database.findAll(Long.class,
	        "SELECT msg_id FROM messages " +
	            "WHERE space_id = ? AND msg_time >= ?;",
	        spaceId, since);

	    response.status(200);
	    return new JSONArray(messages.stream()
	        .map(msgId -> "/spaces/" + spaceId + "/messages/" + msgId)
	        .toList());
	  }

	  public static class Message {
	    private final long spaceId;
	    private final long msgId;
	    private final String author;
	    private final Instant time;
	    private final String message;

	    public Message(long spaceId, long msgId, String author,
	        Instant time, String message) {
	      this.spaceId = spaceId;
	      this.msgId = msgId;
	      this.author = author;
	      this.time = time;
	      this.message = message;
	    }
	    @Override
	    public String toString() {
	      JSONObject msg = new JSONObject();
	      msg.put("uri",
	          "/spaces/" + spaceId + "/messages/" + msgId);
	      msg.put("author", author);
	      msg.put("time", time.toString());
	      msg.put("message", message);
	      return msg.toString();
	    }
	  }
}
