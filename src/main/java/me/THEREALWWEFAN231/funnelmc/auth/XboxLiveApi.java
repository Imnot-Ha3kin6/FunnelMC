package me.THEREALWWEFAN231.funnelmc.auth;

import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * Verified end-to-end against a real logged-in account/session (see FriendsDiag log lines emitted
 * throughout this file) - friends list, presence matching, and session lookup all confirmed working.
 */
public class XboxLiveApi {

	private static final Logger logger = LogManager.getLogger(XboxLiveApi.class);

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
		// Non-null when this connection has no direct IP/port at all and instead uses NetherNet
		// (Bedrock's WebRTC-based transport for Xbox Live/Friends-discovered sessions) - the field
		// is called "NetherNetId" on some servers and "WebRTCNetworkId" on others depending on
		// version, both meaning the same thing: the remote peer's signaling network ID.
		public final Long netherNetId;

		public JoinableSession(String hostIp, int hostPort, Long netherNetId) {
			this.hostIp = hostIp;
			this.hostPort = hostPort;
			this.netherNetId = netherNetId;
		}

		public boolean isDirectIp() {
			return this.hostIp != null && !this.hostIp.isEmpty() && this.hostPort != 0;
		}
	}

	public static List<Friend> getFriends(String authorizationHeader) throws Exception {
		JsonObject response = get("https://peoplehub.xboxlive.com/users/me/people/social/decoration/detail", authorizationHeader, "3");

		List<Friend> friends = new ArrayList<>();
		for (JsonElement element : response.getAsJsonArray("people")) {
			JsonObject person = element.getAsJsonObject();
			friends.add(new Friend(person.get("xuid").getAsString(), person.get("gamertag").getAsString()));
		}
		logger.warn("[FriendsDiag] getFriends found {} friend(s): {}", friends.size(), friends.stream().map(f -> f.gamertag).toList());
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
		// Without this, the API defaults to "user" level and omits devices/titles entirely, so
		// every presence lookup would come back with nothing to match against Minecraft's title ID.
		body.addProperty("level", "all");

		JsonArray response = postForArray("https://userpresence.xboxlive.com/users/batch", authorizationHeader, "3", body);
		logger.warn("[FriendsDiag] userpresence batch raw response: {}", response);

		List<String> activeXuids = new ArrayList<>();
		for (JsonElement element : response) {
			JsonObject presenceItem = element.getAsJsonObject();
			String presenceXuid = presenceItem.has("xuid") ? presenceItem.get("xuid").getAsString() : "?";
			String presenceState = presenceItem.has("state") ? presenceItem.get("state").getAsString() : "?";
			if (!presenceItem.has("devices")) {
				logger.warn("[FriendsDiag] xuid={} state={} has no 'devices' entry at all", presenceXuid, presenceState);
				continue;
			}
			for (JsonElement deviceElement : presenceItem.getAsJsonArray("devices")) {
				JsonObject device = deviceElement.getAsJsonObject();
				if (!device.has("titles")) {
					continue;
				}
				for (JsonElement titleElement : device.getAsJsonArray("titles")) {
					JsonObject title = titleElement.getAsJsonObject();
					String titleId = title.has("id") ? title.get("id").getAsString() : "?";
					String titleState = title.has("state") ? title.get("state").getAsString() : "?";
					String titleName = title.has("name") ? title.get("name").getAsString() : "?";
					logger.warn("[FriendsDiag] xuid={} presenceState={} title id={} name={} state={}",
							presenceXuid, presenceState, titleId, titleName, titleState);
					// Minecraft's title ID differs per platform (Xbox/Windows use 896928775, but a
					// friend on iOS reported 1810924247 in a live FriendsDiag log) - matching on a
					// single hardcoded ID was never going to work across platforms. "name" is the
					// same human-readable "Minecraft" string regardless of platform, so match on
					// that instead.
					if (title.has("name") && title.has("state")
							&& "Minecraft".equalsIgnoreCase(title.get("name").getAsString())
							&& "Active".equals(title.get("state").getAsString())) {
						activeXuids.add(presenceXuid);
					}
				}
			}
		}
		return activeXuids;
	}

	// Looks up the friend's active Minecraft session via MPSD and reads its connection info, if any.
	public static JoinableSession findJoinableSession(String xuid, String authorizationHeader) throws Exception {
		// A flat "xuid" query param plus a top-level "scid" in the body isn't what this endpoint
		// expects - confirmed by a live 400 ("'scid', 'templateName', and 'sessionName' must be
		// specified") from a real account. Verified against jrcarl624/FriendConnect's queryHandles:
		// the owner has to be identified via a nested owners.people.monikerXuid object in the body,
		// not a query string parameter.
		JsonObject owners = new JsonObject();
		JsonObject people = new JsonObject();
		people.addProperty("moniker", "people");
		people.addProperty("monikerXuid", xuid);
		owners.add("people", people);

		JsonObject handleBody = new JsonObject();
		handleBody.add("owners", owners);
		handleBody.addProperty("scid", MINECRAFT_SCID);
		handleBody.addProperty("type", "activity");

		// Asking for customProperties (not just relatedInfo) makes MPSD embed the session's own
		// "properties" object directly inside each handle result - the same friends-visibility
		// grant that lets handles/query find the handle at all is enough to read it that way.
		// A separate GET straight to .../sessions/{name} enforces a *different*, stricter
		// permission check (session membership / read-restriction), which is what was producing a
		// live 403 ("must ... be a member of the session ... if the session ... has a read
		// restriction") for a session whose owner isn't a mutual Xbox Live "friend" in the
		// full sense, even though presence/activity-handle visibility still worked. Preferring the
		// embedded document avoids that second, more restrictive call entirely.
		JsonObject handleResponse = post("https://sessiondirectory.xboxlive.com/handles/query?include=relatedInfo,customProperties", authorizationHeader, MPSD_CONTRACT_VERSION, handleBody);

		if (!handleResponse.has("results") || handleResponse.getAsJsonArray("results").isEmpty()) {
			logger.warn("[FriendsDiag] handles/query for xuid={} returned no results - not in an activity handle for scid={}", xuid, MINECRAFT_SCID);
			return null;
		}

		JsonObject handleResult = handleResponse.getAsJsonArray("results").get(0).getAsJsonObject();
		JsonObject sessionRef = handleResult.getAsJsonObject("sessionRef");
		String scid = sessionRef.get("scid").getAsString();
		String templateName = sessionRef.get("templateName").getAsString();
		String name = sessionRef.get("name").getAsString();

		JsonObject session;
		if (handleResult.has("properties")) {
			logger.warn("[FriendsDiag] Using session properties embedded in handles/query result for {}/{}/{} (avoiding a separate, more restrictive GET)", scid, templateName, name);
			session = handleResult;
		} else {
			session = get("https://sessiondirectory.xboxlive.com/serviceconfigs/" + scid + "/sessiontemplates/" + templateName + "/sessions/" + name, authorizationHeader, MPSD_CONTRACT_VERSION);
		}

		if (!session.has("properties")) {
			logger.warn("[FriendsDiag] MPSD session {}/{}/{} has no 'properties'", scid, templateName, name);
			return null;
		}
		JsonObject properties = session.getAsJsonObject("properties");
		if (!properties.has("custom")) {
			logger.warn("[FriendsDiag] MPSD session {}/{}/{} properties has no 'custom'", scid, templateName, name);
			return null;
		}
		JsonObject custom = properties.getAsJsonObject("custom");
		if (!custom.has("SupportedConnections") || custom.getAsJsonArray("SupportedConnections").isEmpty()) {
			logger.warn("[FriendsDiag] MPSD session {}/{}/{} custom has no/empty 'SupportedConnections': {}", scid, templateName, name, custom);
			return null;
		}

		JsonObject connection = custom.getAsJsonArray("SupportedConnections").get(0).getAsJsonObject();
		Long netherNetId = null;
		if (connection.has("NetherNetId")) {
			netherNetId = connection.get("NetherNetId").getAsLong();
		} else if (connection.has("WebRTCNetworkId")) {
			netherNetId = connection.get("WebRTCNetworkId").getAsLong();
		}
		return new JoinableSession(connection.get("HostIpAddress").getAsString(), connection.get("HostPort").getAsInt(), netherNetId);
	}

	private static JsonObject get(String url, String authorizationHeader, String contractVersion) throws Exception {
		HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Authorization", authorizationHeader);
		connection.setRequestProperty("x-xbl-contract-version", contractVersion);
		connection.setRequestProperty("Accept-Language", "en-US");

		String responseText = readResponseOrThrow(connection, url);
		return FunnelMC.instance.fileManagement.jsonParser.parse(responseText).getAsJsonObject();
	}

	private static JsonObject post(String url, String authorizationHeader, String contractVersion, JsonObject body) throws Exception {
		HttpsURLConnection connection = openPost(url, authorizationHeader, contractVersion, body);
		String responseText = readResponseOrThrow(connection, url);
		return FunnelMC.instance.fileManagement.jsonParser.parse(responseText).getAsJsonObject();
	}

	private static JsonArray postForArray(String url, String authorizationHeader, String contractVersion, JsonObject body) throws Exception {
		HttpsURLConnection connection = openPost(url, authorizationHeader, contractVersion, body);
		String responseText = readResponseOrThrow(connection, url);
		return FunnelMC.instance.fileManagement.jsonParser.parse(responseText).getAsJsonArray();
	}

	// HttpsURLConnection#getInputStream() throws for any non-2xx status *without* exposing the
	// response body at all (that only lives on getErrorStream()) - so a 401/403 from a subtly wrong
	// token scope, or a 400 from a malformed request, was previously surfacing as a bare
	// "Server returned HTTP response code: ..." IOException with no way to tell what Xbox Live
	// actually objected to. Read the error body on failure so it shows up in the log instead.
	private static String readResponseOrThrow(HttpsURLConnection connection, String url) throws Exception {
		int status = connection.getResponseCode();
		if (status >= 200 && status < 300) {
			String body = FunnelMC.instance.fileManagement.getTextFromInputStream(connection.getInputStream());
			logger.warn("[FriendsDiag] {} {} -> {} body={}", connection.getRequestMethod(), url, status, body);
			return body;
		}

		String errorBody = connection.getErrorStream() != null
				? FunnelMC.instance.fileManagement.getTextFromInputStream(connection.getErrorStream())
				: "(no error body)";
		logger.warn("[FriendsDiag] {} {} -> {} errorBody={}", connection.getRequestMethod(), url, status, errorBody);
		throw new Exception(connection.getRequestMethod() + " " + url + " failed with HTTP " + status + ": " + errorBody);
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
