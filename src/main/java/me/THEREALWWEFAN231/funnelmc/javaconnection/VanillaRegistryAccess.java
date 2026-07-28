package me.THEREALWWEFAN231.funnelmc.javaconnection;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;

// There's no real Java server here to send the registry-sync packets (ClientboundRegistryDataPacket)
// a vanilla client would normally get before login, and ClientPacketListener's constructor and
// ClientLevel both hard-require dynamic registries like worldgen/biome and worldgen/dimension_type
// to actually be present (not just the static/built-in item/block/etc layer ClientRegistryLayer.STATIC
// provides) - with their tags genuinely bound, not just their elements.
//
// An earlier version of this built worldgen registries from VanillaRegistries.createLookup(), the
// same in-memory bootstrap the game's own datagen tooling uses to dump default registry JSON at build
// time. That bootstrap is deliberately incapable of real tag resolution (see
// RegistrySetBuilder.EmptyTagLookup#get(TagKey) - always returns HolderSet.emptyNamed(...), a
// placeholder whose contents() unconditionally throws "can't be dereferenced during construction").
// Any bootstrapped value that captures a tag reference internally (e.g. DimensionType.timelines(),
// used by ClientLevel's EnvironmentAttributeSystem on every login) carries that poisoned reference
// forever, so it crashes the instant real gameplay code dereferences it - regardless of anything done
// to the registry afterward, since the reference is baked into the value at bootstrap time.
//
// So this instead runs the same real JSON-registry-loading pipeline WorldLoader uses to build a
// server's (or a singleplayer world's) registries at startup - RegistryDataLoader for elements plus
// TagLoader for real tag bindings - just pointed at the client's own ResourceManager instead of a
// server datapack. The client jar already bundles the same data/minecraft/... registry and tag JSON a
// vanilla server would ship, since it needs it for singleplayer/LAN worlds anyway.
public class VanillaRegistryAccess {

	private static RegistryAccess.Frozen instance;

	public static synchronized RegistryAccess.Frozen get() {
		if (instance == null) {
			instance = build();
		}
		return instance;
	}

	private static RegistryAccess.Frozen build() {
		ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

		LayeredRegistryAccess<RegistryLayer> layers = RegistryLayer.createRegistryAccess();
		List<Registry.PendingTags<?>> staticTags = TagLoader.loadTagsForExistingRegistries(resourceManager, layers.getLayer(RegistryLayer.STATIC));

		RegistryAccess.Frozen worldgenLoadContext = layers.getAccessForLoading(RegistryLayer.WORLDGEN);
		List<HolderLookup.RegistryLookup<?>> worldgenContextRegistries = TagLoader.buildUpdatedLookups(worldgenLoadContext, staticTags);

		RegistryAccess.Frozen worldgenRegistries = RegistryDataLoader
				.load(resourceManager, worldgenContextRegistries, RegistryDataLoader.WORLDGEN_REGISTRIES, Runnable::run)
				.join();

		// Same ordering WorldLoader.load uses: only commit the real static-layer tags (item/block/
		// entity_type/... tags, shared with the rest of the running client) once the worldgen load that
		// referenced their patched-but-not-yet-applied view has actually succeeded.
		staticTags.forEach(Registry.PendingTags::apply);

		return layers.replaceFrom(RegistryLayer.WORLDGEN, worldgenRegistries).compositeAccess();
	}

}
