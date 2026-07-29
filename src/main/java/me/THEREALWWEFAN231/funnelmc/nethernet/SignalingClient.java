package me.THEREALWWEFAN231.funnelmc.nethernet;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The NetherNet signaling protocol (see df-mc/nethernet-spec) runs over a plain JSON WebSocket:
// message Type 0 = ping (client keepalive, no response expected), Type 1 = Signal (SDP offer/answer/
// ICE candidates relayed between peers), Type 2 = Credentials (STUN/TURN servers, sent once by the
// server right after connecting). This class only proves the auth+connection chain works end to end
// by waiting for that initial Credentials message - actually driving a WebRTC connection off of it
// is a separate, much larger piece of work (needs a real ICE/DTLS/SCTP stack).
public class SignalingClient {

	private static final Logger logger = LogManager.getLogger(SignalingClient.class);

	public static String connectAndWaitForCredentials(String signalingServiceUri, String authorizationHeader, long timeoutSeconds) throws Exception {
		CompletableFuture<String> credentialsFuture = new CompletableFuture<>();
		StringBuilder textBuffer = new StringBuilder();

		String networkId = Long.toUnsignedString(new SecureRandom().nextLong());
		String wsUrl = signalingServiceUri.replaceAll("/+$", "")
				.replaceFirst("^https://", "wss://")
				.replaceFirst("^http://", "ws://")
				+ "/ws/v1.0/signaling/" + networkId;

		logger.warn("[NetherNetDiag] Connecting to signaling websocket {} (our networkId={})", wsUrl, networkId);

		HttpClient httpClient = HttpClient.newHttpClient();
		WebSocket.Builder builder = httpClient.newWebSocketBuilder()
				.header("Authorization", authorizationHeader);

		WebSocket.Listener listener = new WebSocket.Listener() {
			@Override
			public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
				textBuffer.append(data);
				webSocket.request(1);
				if (!last) {
					return null;
				}

				String message = textBuffer.toString();
				textBuffer.setLength(0);
				logger.warn("[NetherNetDiag] Signaling message received: {}", message);

				if (message.contains("\"Type\":2") && !credentialsFuture.isDone()) {
					credentialsFuture.complete(message);
				}
				return null;
			}

			@Override
			public void onError(WebSocket webSocket, Throwable error) {
				logger.error("[NetherNetDiag] Signaling websocket error", error);
				credentialsFuture.completeExceptionally(error);
			}

			@Override
			public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
				logger.warn("[NetherNetDiag] Signaling websocket closed: statusCode={} reason={}", statusCode, reason);
				if (!credentialsFuture.isDone()) {
					credentialsFuture.completeExceptionally(new RuntimeException(
							"Signaling websocket closed before Credentials arrived: " + statusCode + " " + reason));
				}
				return null;
			}
		};

		WebSocket webSocket = builder.buildAsync(URI.create(wsUrl), listener).get(timeoutSeconds, TimeUnit.SECONDS);
		try {
			return credentialsFuture.get(timeoutSeconds, TimeUnit.SECONDS);
		} finally {
			webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "diagnostic probe complete").join();
		}
	}

}
