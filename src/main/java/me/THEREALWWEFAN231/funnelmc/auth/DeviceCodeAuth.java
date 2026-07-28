package me.THEREALWWEFAN231.funnelmc.auth;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonObject;

import me.THEREALWWEFAN231.funnelmc.FunnelMC;

/*
 * Microsoft's OAuth device authorization flow, used instead of making the player manually copy
 * an access token out of a browser redirect URL. Uses the same client_id that the rest of the
 * auth package already uses for the (now-removed) implicit grant flow, since it's registered for
 * both. Endpoints/params verified directly against login.live.com, not guessed:
 *  - POST oauth20_connect.srf with response_type=device_code gets you a user_code + device_code.
 *  - The player goes to verification_uri (microsoft.com/link) and enters user_code.
 *  - POST oauth20_token.srf with grant_type=device_code, polled every `interval` seconds, returns
 *    "authorization_pending" until they finish, then an access_token.
 */
public class DeviceCodeAuth {

	private static final String CLIENT_ID = "00000000441cc96b";
	private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
	private static final String DEVICE_CODE_URL = "https://login.live.com/oauth20_connect.srf";
	private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";

	public static class DeviceCodeInfo {
		public final String userCode;
		public final String deviceCode;
		public final String verificationUri;
		public final int interval;
		public final int expiresIn;

		public DeviceCodeInfo(String userCode, String deviceCode, String verificationUri, int interval, int expiresIn) {
			this.userCode = userCode;
			this.deviceCode = deviceCode;
			this.verificationUri = verificationUri;
			this.interval = interval;
			this.expiresIn = expiresIn;
		}
	}

	public static DeviceCodeInfo requestDeviceCode() throws Exception {
		JsonObject response = post(DEVICE_CODE_URL, "client_id=" + CLIENT_ID + "&scope=" + urlEncode(SCOPE) + "&response_type=device_code");

		return new DeviceCodeInfo(
				response.get("user_code").getAsString(),
				response.get("device_code").getAsString(),
				response.get("verification_uri").getAsString(),
				response.get("interval").getAsInt(),
				response.get("expires_in").getAsInt());
	}

	// Blocks (polling every `interval` seconds) until the player finishes logging in, the code
	// expires, or something goes wrong. Call this off the render thread.
	public static String pollForAccessToken(DeviceCodeInfo deviceCodeInfo) throws Exception {
		long deadline = System.currentTimeMillis() + deviceCodeInfo.expiresIn * 1000L;

		while (System.currentTimeMillis() < deadline) {
			Thread.sleep(deviceCodeInfo.interval * 1000L);

			JsonObject response = post(TOKEN_URL, "client_id=" + CLIENT_ID + "&device_code=" + urlEncode(deviceCodeInfo.deviceCode) + "&grant_type=device_code");

			if (response.has("access_token")) {
				return response.get("access_token").getAsString();
			}

			String error = response.has("error") ? response.get("error").getAsString() : "unknown_error";
			if (!error.equals("authorization_pending")) {
				throw new Exception("Microsoft login failed: " + error);
			}
		}

		throw new Exception("Microsoft login code expired before you finished logging in.");
	}

	private static JsonObject post(String urlString, String body) throws Exception {
		URL url = new URL(urlString);
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setDoOutput(true);

		try (OutputStream outputStream = connection.getOutputStream()) {
			outputStream.write(body.getBytes("UTF-8"));
		}

		InputStream responseStream = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
		String responseText = FunnelMC.instance.fileManagement.getTextFromInputStream(responseStream);
		return FunnelMC.instance.fileManagement.jsonParser.parse(responseText).getAsJsonObject();
	}

	private static String urlEncode(String value) throws Exception {
		return URLEncoder.encode(value, "UTF-8");
	}

}
