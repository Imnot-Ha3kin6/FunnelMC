package me.THEREALWWEFAN231.funnelmc.javaconnection;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Lifecycle;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;

// There's no real Java server here to send the registry-sync packets (ClientboundRegistryDataPacket)
// a vanilla client would normally get before login, and ClientPacketListener's constructor and
// ClientLevel both hard-require dynamic registries like worldgen/biome and worldgen/dimension_type
// to actually be present (not just the static/built-in item/block/etc layer ClientRegistryLayer.STATIC
// provides). This rebuilds what those packets would have populated, straight from vanilla's bundled
// default data via VanillaRegistries' in-memory datagen bootstrap - the same data a real Java server
// with no datapacks would have sent.
//
// VanillaRegistries.createLookup() only hands back lightweight HolderLookup facades for the registries
// it bootstraps (not real Registry instances - RegistrySetBuilder doesn't build one), but
// RegistryAccess#lookupOrThrow requires an actual Registry, so each dynamic registry is copied into a
// real frozen MappedRegistry below.
public class VanillaRegistryAccess {

	private static RegistryAccess.Frozen instance;

	public static synchronized RegistryAccess.Frozen get() {
		if (instance == null) {
			instance = build();
		}
		return instance;
	}

	private static RegistryAccess.Frozen build() {
		RegistryAccess.Frozen staticAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
		HolderLookup.Provider vanillaLookup = VanillaRegistries.createLookup();

		List<Registry<?>> registries = new ArrayList<>();
		staticAccess.registries().forEach(entry -> registries.add(entry.value()));

		vanillaLookup.listRegistryKeys().forEach(key -> {
			if (staticAccess.lookup(key).isPresent()) {
				return;
			}
			registries.add(toRegistry(key, vanillaLookup));
		});

		return new RegistryAccess.ImmutableRegistryAccess(registries).freeze();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> Registry<T> toRegistry(ResourceKey<? extends Registry<?>> key, HolderLookup.Provider provider) {
		ResourceKey<Registry<T>> registryKey = (ResourceKey<Registry<T>>) (ResourceKey<?>) key;
		HolderLookup.RegistryLookup<T> lookup = (HolderLookup.RegistryLookup<T>) provider.lookupOrThrow((ResourceKey) key);

		MappedRegistry<T> registry = new MappedRegistry<>(registryKey, Lifecycle.stable());
		lookup.listElements().forEach(holder -> registry.register(holder.key(), holder.value(), RegistrationInfo.BUILT_IN));
		return registry.freeze();
	}

}
