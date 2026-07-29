package me.THEREALWWEFAN231.funnelmc.nethernet;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.THEREALWWEFAN231.funnelmc.FunnelMC;

// Ports gophertunnel's minecraft/service.AuthorizationEnvironment.Token(): exchanges a PlayFab
// session ticket for an MCToken (an "authorizationHeader" bearer value), which is what actually
// authenticates the NetherNet signaling websocket. Without "mc-signaling-usewebsockets" in
// treatmentOverrides, the signaling server accepts the connection but silently refuses to deliver
// any signals to/from it - this isn't a hidden per-account flag, just a value the client asks for
// in this same request body.
public class FranchiseAuth {

	public static class Token {
		public String authorizationHeader;
		public List<String> treatments = new ArrayList<>();
	}

	public static Token startSession(String authServiceUri, String playFabTitleId, String playFabSessionTicket, String gameVersion) throws Exception {
		JsonArray treatmentOverrides = new JsonArray();
		treatmentOverrides.add("mc-signaling-usewebsockets");

		JsonObject device = new JsonObject();
		device.addProperty("applicationType", "MinecraftPE");
		device.add("capabilities", new JsonArray());
		device.addProperty("gameVersion", gameVersion);
		device.addProperty("id", UUID.randomUUID().toString());
		device.addProperty("memory", Long.toString(16L * 1024 * 1024 * 1024));
		device.addProperty("platform", "Windows10");
		device.addProperty("playFabTitleId", playFabTitleId);
		device.addProperty("storePlatform", "uwp.store");
		device.add("treatmentOverrides", treatmentOverrides);
		device.addProperty("type", "Windows10");

		JsonObject user = new JsonObject();
		user.addProperty("language", "en");
		user.addProperty("languageCode", "en-US");
		user.addProperty("regionCode", "US");
		user.addProperty("token", playFabSessionTicket);
		user.addProperty("tokenType", "PlayFab");

		JsonObject body = new JsonObject();
		body.add("device", device);
		body.add("user", user);

		String url = authServiceUri.replaceAll("/+$", "") + "/api/v1.0/session/start";

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(FunnelMC.instance.fileManagement.normalGson.toJson(body)))
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new RuntimeException("Franchise session/start failed with HTTP " + response.statusCode() + ": " + response.body());
		}

		JsonObject responseBody = FunnelMC.instance.fileManagement.jsonParser.parse(response.body()).getAsJsonObject();
		JsonObject data = responseBody.has("data") ? responseBody.getAsJsonObject("data") : null;
		if (data == null || !data.has("authorizationHeader")) {
			throw new RuntimeException("Franchise session/start response missing authorizationHeader: " + response.body());
		}

		Token token = new Token();
		token.authorizationHeader = data.get("authorizationHeader").getAsString();
		if (data.has("treatments")) {
			for (JsonElement e : data.getAsJsonArray("treatments")) {
				token.treatments.add(e.getAsString());
			}
		}
		return token;
	}

}
