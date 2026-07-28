package me.THEREALWWEFAN231.funnelmc.auth;

import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.THEREALWWEFAN231.funnelmc.FunnelMC;

/*
 * Xbox Live's friends list, presence, and Multiplayer Session Directory (MPSD) APIs, used to find
 * which friends are currently playing a joinable Bedrock world and how to connect to it.
 *
 * Endpoints/headers/response shapes verified against:
 *  - OpenXbox/xbox-webapi-python (actively maintained, MIT-licensed Xbox Live client library) for
 *    the peoplehub friends list and userpresence batch calls.
 *  - Microsoft's own official MPSD documentation (learn.microsoft.com, .../uri-handlesqueryincludepost
 *    and the MPSD session document structure under .../live-mpsd-details) for /handles/query and
 *    the session JSON document shape (properties.custom).
 *  - jrcarl624/FriendConnect (a working open source tool that broadcasts a Bedrock world onto Xbox
 *    Live for friends to join - this reads the same kind of session that tool writes) for the
 *    Minecraft-specific SCID, session template name, and the SupportedConnections field names,
 *    none of which are documented anywhere by Microsoft.
 *
 * Not yet verified against a real logged-in account/session - the general shape is solid but this
 * is the one part of this feature that couldn't be tested without a real Xbox Live login.
 */
public class XboxLiveApi {

	private static final String MINECRAFT_TITLE_ID = "896928775"; // same constant Auth#getOfflineChainData already uses
	private static final String MINECRAFT_SCID = "4fc10100-5f7a-4470-899b-280835760c07";
	private static final String MPSD_CONTRACT_VERSION = "107";

	public static class Friend {
		public final String xuid;
		public final String gamertag;

		public Friend(String xuid, String gamertag) {
			this.xuid = xuid;
			this.gamertag = gamertag;
		}
	}

	public static class JoinableSession {
		public final String hostIp;
		public final int hostPort;

		public JoinableSession(String hostIp, int hostPort) {
			this.hostIp = hostIp;
			this.hostPort = hostPort;
		}
	}

	public static List<Friend> getFriends(String authorizationHeader) throws Exception {
		JsonObject response = get("https://peoplehub.xboxlive.com/users/me/people/social/decoration/detail", authorizationHeader, "3");

		List<Friend> friends = new ArrayList<>();
		for (JsonElement element : response.getAsJsonArray("people")) {
			JsonObject person = element.getAsJsonObject();
			friends.add(new Friend(person.get("xuid").getAsString(), person.get("gamertag").getAsString()));
		}
		return friends;
	}

	// Returns the xuids (out of the ones given) that are currently active in Minecraft.
	public static List<String> getXuidsActiveInMinecraft(List<String> xuids, String authorizationHeader) throws Exception {
		JsonObject body = new JsonObject();
		JsonArray users = new JsonArray();
		for (String xuid : xuids) {
			users.add(xuid);
		}
		body.add("users", users);
		body.addProperty("onlineOnly", true);

		JsonArray response = postForArray("https://userpresence.xboxlive.com/users/batch", authorizationHeader, "3", body);

		List<String> activeXuids = new ArrayList<>();
		for (JsonElement element : response) {
			JsonObject presenceItem = element.getAsJsonObject();
			if (!presenceItem.has("devices")) {
				continue;
			}
			for (JsonElement deviceElement : presenceItem.getAsJsonArray("devices")) {
				JsonObject device = deviceElement.getAsJsonObject();
				if (!device.has("titles")) {
					continue;
				}
				for (JsonElement titleElement : device.getAsJsonArray("titles")) {
					JsonObject title = titleElement.getAsJsonObject();
					if (title.has("id") && title.has("state")
							&& MINECRAFT_TITLE_ID.equals(title.get("id").getAsString())
							&& "Active".equals(title.get("state").getAsString())) {
						activeXuids.add(presenceItem.get("xuid").getAsString());
					}
				}
			}
		}
		return activeXuids;
	}

	// Looks up the friend's active Minecraft session via MPSD and reads its connection info, if any.
	public static JoinableSession findJoinableSession(String xuid, String authorizationHeader) throws Exception {
		JsonObject handleBody = new JsonObject();
		handleBody.addProperty("type", "activity");
		handleBody.addProperty("scid", MINECRAFT_SCID);

		JsonObject handleResponse = post("https://sessiondirectory.xboxlive.com/handles/query?include=relatedInfo&xuid=" + xuid, authorizationHeader, MPSD_CONTRACT_VERSION, handleBody);

		if (!handleResponse.has("results") || handleResponse.getAsJsonArray("results").isEmpty()) {
			return null;
		}

		JsonObject sessionRef = handleResponse.getAsJsonArray("results").get(0).getAsJsonObject().getAsJsonObject("sessionRef");
		String scid = sessionRef.get("scid").getAsString();
		String templateName = sessionRef.get("templateName").getAsString();
		String name = sessionRef.get("name").getAsString();

		JsonObject session = get("https://sessiondirectory.xboxlive.com/serviceconfigs/" + scid + "/sessiontemplates/" + templateName + "/sessions/" + name, authorizationHeader, MPSD_CONTRACT_VERSION);

		if (!session.has("properties")) {
			return null;
		}
		JsonObject properties = session.getAsJsonObject("properties");
		if (!properties.has("custom")) {
			return null;
		}
		JsonObject custom = properties.getAsJsonObject("custom");
		if (!custom.has("SupportedConnections") || custom.getAsJsonArray("SupportedConnections").isEmpty()) {
			return null;
		}

		JsonObject connection = custom.getAsJsonArray("SupportedConnections").get(0).getAsJsonObject();
		return new JoinableSession(connection.get("HostIpAddress").getAsString(), connection.get("HostPort").getAsInt());
	}

	private static JsonObject get(String url, String authorizationHeader, String contractVersion) throws Exception {
		HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Authorization", authorizationHeader);
		connection.setRequestProperty("x-xbl-contract-version", contractVersion);
		connection.setRequestProperty("Accept-Language", "en-US");

		String responseText = FunnelMC.instance.fileManagement.getTextFromInputStream(connection.getInputStream());
		return FunnelMC.instance.fileManagement.jsonParser.parse(responseText).getAsJsonObject();
	}

	private static JsonObject post(String url, String authorizationHeader, String contractVersion, JsonObject body) throws Exception {
		HttpsURLConnection connection = openPost(url, authorizationHeader, contractVersion, body);
		String responseText = FunnelMC.instance.fileManagement.getTextFromInputStream(connection.getInputStream());
		return FunnelMC.instance.fileManagement.jsonParser.parse(responseText).getAsJsonObject();
	}

	private static JsonArray postForArray(String url, String authorizationHeader, String contractVersion, JsonObject body) throws Exception {
		HttpsURLConnection connection = openPost(url, authorizationHeader, contractVersion, body);
		String responseText = FunnelMC.instance.fileManagement.getTextFromInputStream(connection.getInputStream());
		return FunnelMC.instance.fileManagement.jsonParser.parse(responseText).getAsJsonArray();
	}

	private static HttpsURLConnection openPost(String url, String authorizationHeader, String contractVersion, JsonObject body) throws Exception {
		HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Authorization", authorizationHeader);
		connection.setRequestProperty("x-xbl-contract-version", contractVersion);
		connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		connection.setRequestProperty("Accept-Language", "en-US");
		connection.setDoOutput(true);

		try (OutputStream outputStream = connection.getOutputStream()) {
			outputStream.write(FunnelMC.instance.fileManagement.normalGson.toJson(body).getBytes("UTF-8"));
		}
		return connection;
	}

}
