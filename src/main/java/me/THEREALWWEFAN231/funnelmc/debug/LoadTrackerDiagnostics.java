package me.THEREALWWEFAN231.funnelmc.debug;

import java.lang.reflect.Field;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.THEREALWWEFAN231.funnelmc.FunnelMC;
import me.THEREALWWEFAN231.funnelmc.events.EventPlayerTick;
import net.minecraft.client.multiplayer.ClientPacketListener;

// Temporary instrumentation for the "stuck on Loading Terrain" investigation - reflectively dumps
// ClientPacketListener's private levelLoadTracker state once a second so we can see which of
// LevelLoadTracker's three states (WaitingForServer / WaitingForPlayerChunk / ClientLevelReady) the
// vanilla dismissal logic is actually stuck in, instead of guessing from outside. Remove once the
// stuck-loading-screen issue is resolved.
public class LoadTrackerDiagnostics {

	private static final Logger logger = LogManager.getLogger(LoadTrackerDiagnostics.class);

	private int tickCounter;

	public LoadTrackerDiagnostics() {
		EventManager.register(this);
	}

	@EventTarget
	public void onEvent(EventPlayerTick event) {
		this.tickCounter++;
		if (this.tickCounter % 20 != 0) {
			return;
		}

		try {
			ClientPacketListener connection = FunnelMC.mc.getConnection();
			if (connection == null) {
				logger.warn("[LoadTrackerDiag] Minecraft.getConnection() is null");
				return;
			}

			Field levelLoadTrackerField = ClientPacketListener.class.getDeclaredField("levelLoadTracker");
			levelLoadTrackerField.setAccessible(true);
			Object levelLoadTracker = levelLoadTrackerField.get(connection);

			if (levelLoadTracker == null) {
				logger.warn("[LoadTrackerDiag] levelLoadTracker is null (already cleared - level should be ready)");
				return;
			}

			Field clientStateField = levelLoadTracker.getClass().getDeclaredField("clientState");
			clientStateField.setAccessible(true);
			Object clientState = clientStateField.get(levelLoadTracker);

			if (clientState == null) {
				logger.warn("[LoadTrackerDiag] clientState is null");
				return;
			}

			String stateClassName = clientState.getClass().getSimpleName();

			if (stateClassName.contains("WaitingForPlayerChunk")) {
				Field playerSectionReadyField = clientState.getClass().getDeclaredField("playerSectionReady");
				playerSectionReadyField.setAccessible(true);
				Object playerSectionReady = playerSectionReadyField.get(clientState);
				logger.warn("[LoadTrackerDiag] state=WaitingForPlayerChunk playerSectionReady={} playerPos={} cameraBlockPos={}",
						playerSectionReady,
						FunnelMC.mc.player != null ? FunnelMC.mc.player.blockPosition() : "null",
						FunnelMC.mc.gameRenderer != null ? FunnelMC.mc.gameRenderer.mainCamera().blockPosition() : "null");
			} else {
				logger.warn("[LoadTrackerDiag] state={} playerPos={}", stateClassName,
						FunnelMC.mc.player != null ? FunnelMC.mc.player.blockPosition() : "null");
			}
		} catch (Exception e) {
			logger.error("[LoadTrackerDiag] Failed to inspect levelLoadTracker", e);
		}
	}

}
