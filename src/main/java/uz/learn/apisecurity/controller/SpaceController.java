package uz.learn.apisecurity.controller;

import org.dalesbred.Database;
import org.json.JSONObject;

import spark.Request;
import spark.Response;

public class SpaceController {
	private final Database database;

	public SpaceController(Database database) {
		this.database = database;
	}

	public JSONObject createSpace(Request request, Response response) {
		var json = new org.json.JSONObject(request.body());
		var spaceName = json.get("name");
		var owner = json.get("owner");
		return database.withTransaction(tx-> {
			var spaceId = database.findUniqueLong("SELECT NEXT VALUE FOR space_id_seq");
			database.updateUnique(
					"INSERT INTO spaces(space_id, name, owner) " +
					"VALUES(" + spaceId  + ", '" + spaceName + 
					"', '" + owner + "');"
					);
			response.status(201);
			response.header("Location", "/spaces/" + spaceId);
			return new org.json.JSONObject().put("name", spaceName).put("uri", "/spaces/" + spaceId);
		});
	}
}
