package me.THEREALWWEFAN231.tunnelmc.translator.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DimensionTranslator {

	// TODO: modern DimensionType (26.2) is fully registry/datapack driven and carries a lot more
	// data than 1.16.5's hardcoded OVERWORLD/THE_NETHER/THE_END static fields did (skybox,
	// cardinal lighting, environment attributes, timelines, world clock...). Since this fake
	// client has no real Java server/datapack to pull a Holder<DimensionType> from, we need to
	// bundle a snapshot of vanilla's default registry data (extracted via datagen) and load it
	// through RegistryOps at mod init, then look the right dimension up from there. Left
	// unimplemented for now - see PR description.
	public static ResourceKey<Level> bedrockToJavaRegistryKey(int bedrockDimensionId) {
		if (bedrockDimensionId == 0) {
			return Level.OVERWORLD;
		} else if (bedrockDimensionId == 1) {
			return Level.NETHER;
		} else if (bedrockDimensionId == 2) {
			return Level.END;
		}

		return Level.OVERWORLD;
	}

}
