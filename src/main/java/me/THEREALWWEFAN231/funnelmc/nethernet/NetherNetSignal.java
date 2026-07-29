package me.THEREALWWEFAN231.funnelmc.nethernet;

// Mirrors go-nethernet's Signal: negotiation messages exchanged over the signaling websocket to
// set up a WebRTC connection (see df-mc/nethernet-spec). Wire format is a single string
// "TYPE CONNECTIONID DATA" (space-separated, DATA may itself contain spaces/newlines since it's
// an SDP blob or ICE candidate string) - this is nested inside the outer signaling.Message
// envelope's "Message" field, not sent as raw JSON itself.
public class NetherNetSignal {

	public static final String TYPE_OFFER = "CONNECTREQUEST";
	public static final String TYPE_ANSWER = "CONNECTRESPONSE";
	public static final String TYPE_CANDIDATE = "CANDIDATEADD";
	public static final String TYPE_ERROR = "CONNECTERROR";

	public final String type;
	public final long connectionId;
	public final String data;

	public NetherNetSignal(String type, long connectionId, String data) {
		this.type = type;
		this.connectionId = connectionId;
		this.data = data;
	}

	public String encode() {
		return this.type + " " + this.connectionId + " " + this.data;
	}

	public static NetherNetSignal decode(String text) {
		String[] parts = text.split(" ", 3);
		if (parts.length != 3) {
			throw new IllegalArgumentException("Malformed NetherNet signal (expected 3 space-separated segments): " + text);
		}
		return new NetherNetSignal(parts[0], Long.parseUnsignedLong(parts[1]), parts[2]);
	}

}
