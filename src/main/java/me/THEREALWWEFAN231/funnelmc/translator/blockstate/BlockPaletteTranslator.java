package me.THEREALWWEFAN231.funnelmc.translator.blockstate;

import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleBlockDefinition;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

/*
 * as of 1.16.100, the block palette is static between all servers, so we can load this once and be over it
 * it uses BlockStateTranslator which loaded blocks.json, from BlockStateTranslator we can match blocks and get their runtime id for a Bedrock server
 */
public class BlockPaletteTranslator {

	public static int AIR_BEDROCK_BLOCK_ID;
	public static int WATER_BEDROCK_BLOCK_ID;

	// Used for persistent v8 decoding.
	private static final Object2IntMap<String> BEDROCK_BLOCK_STATE_TO_RUNTIME_ID = new Object2IntOpenHashMap<>();

	public static final Int2ObjectMap<BlockState> RUNTIME_ID_TO_BLOCK_STATE = new Int2ObjectOpenHashMap<>();
	public static final Object2IntMap<BlockState> BLOCK_STATE_TO_RUNTIME_ID = new Object2IntOpenHashMap<>();

	// The runtime block palette itself is client-local (see the class comment) and doesn't depend on
	// any particular connection, so this can be built once here rather than per-connection - the
	// Bedrock codec needs it to decode any packet carrying item stacks with block-item components
	// (ItemComponentPacket, CreativeContentPacket, CraftingDataPacket, ...).
	public static DefinitionRegistry<BlockDefinition> BLOCK_DEFINITIONS;

	public static void loadMap(NbtList<NbtMap> blockPaletteData) {
		// Callers are expected to skip this call entirely when a server doesn't provide a live palette
		// (see ClientBatchHandler), but guard here too since this is also called directly from
		// BlockStateTranslator.load() - iterating a null NbtList NPEs on NbtList.iterator().
		if (blockPaletteData == null) {
			return;
		}

		// Runtime IDs are just this list's index order - they're only meaningful relative to whichever
		// palette produced them, so stale entries from a previous loadMap() call (mod startup's bundled
		// vanilla-only fallback, or a previous connection's server) have to be cleared before
		// repopulating, otherwise a reconnect to a different server leaves old (now wrong) mappings
		// mixed in with the new ones.
		BEDROCK_BLOCK_STATE_TO_RUNTIME_ID.clear();
		RUNTIME_ID_TO_BLOCK_STATE.clear();
		BLOCK_STATE_TO_RUNTIME_ID.clear();

		int runtimeId = 0;
		SimpleDefinitionRegistry.Builder<BlockDefinition> blockDefinitionsBuilder = SimpleDefinitionRegistry.builder();
		for (NbtMap nbtMap : blockPaletteData) {
			BedrockBlockState bedrockBlockState = bedrockStateFromNBTMap(nbtMap);
			BEDROCK_BLOCK_STATE_TO_RUNTIME_ID.put(bedrockBlockState.toString(), runtimeId);
			blockDefinitionsBuilder.add(new SimpleBlockDefinition(bedrockBlockState.identifier, runtimeId, nbtMap.getCompound("states")));

			BlockState blockState = BlockStateTranslator.BEDROCK_BLOCK_STATE_STRING_TO_JAVA_BLOCK_STATE.get(bedrockBlockState.toString());
			if (blockState != null) {
				RUNTIME_ID_TO_BLOCK_STATE.put(runtimeId, blockState);
				BLOCK_STATE_TO_RUNTIME_ID.put(blockState, runtimeId);
				if (bedrockBlockState.identifier.equals("minecraft:air")) {
					AIR_BEDROCK_BLOCK_ID = runtimeId;
				} else if (bedrockBlockState.identifier.equals("minecraft:water")) {
					WATER_BEDROCK_BLOCK_ID = runtimeId;
				}
			} else {
				RUNTIME_ID_TO_BLOCK_STATE.put(runtimeId, resolveByDefaultNameMatch(bedrockBlockState));
			}

			runtimeId++;
		}

		BLOCK_DEFINITIONS = blockDefinitionsBuilder.build();
	}

	// blocks.json is a hand-curated table from 2020 (see BlockStateTranslator) and was never going to
	// keep up with every block added since. Rather than defaulting every miss to plain stone, mirror
	// what modern Geyser mapping data actually does for the vast majority of blocks: match by identical
	// namespaced identifier (true for most vanilla blocks on both platforms) and apply whichever
	// Bedrock state properties happen to share a name with a real Java property on that block
	// (axis, waterlogged, powered, ...). This won't get bedrock-specific encodings (like
	// facing_direction ints or *_bit flags) right, but it beats every unmapped block silently
	// rendering as stone.
	private static BlockState resolveByDefaultNameMatch(BedrockBlockState bedrockBlockState) {
		Identifier id = Identifier.tryParse(bedrockBlockState.identifier);
		Block block = id != null ? BuiltInRegistries.BLOCK.getOptional(id).orElse(null) : null;

		if (block == null) {
			System.out.println("Unable to find suitable block state for " + bedrockBlockState.toString());
			return Blocks.STONE.defaultBlockState();
		}

		BlockState blockState = block.defaultBlockState();
		for (Map.Entry<String, String> entry : bedrockBlockState.properties.entrySet()) {
			Property<?> property = block.getStateDefinition().getProperty(entry.getKey());
			if (property != null) {
				blockState = applyProperty(blockState, property, entry.getValue());
			}
		}
		return blockState;
	}

	private static <T extends Comparable<T>> BlockState applyProperty(BlockState blockState, Property<T> property, String rawValue) {
		return property.getValue(rawValue).map(value -> blockState.trySetValue(property, value)).orElse(blockState);
	}

	public static int getBedrockBlockId(BedrockBlockState state) {
		return BlockPaletteTranslator.BEDROCK_BLOCK_STATE_TO_RUNTIME_ID.getOrDefault(state.toString(), AIR_BEDROCK_BLOCK_ID);
	}

	public static BedrockBlockState bedrockStateFromNBTMap(NbtMap nbtMap) {
		String mcbeStringBlockName = nbtMap.getString("name");
		NbtMap blockStates = nbtMap.getCompound("states");

		BedrockBlockState bedrockBlockState = new BedrockBlockState();
		bedrockBlockState.identifier = mcbeStringBlockName;

		for (Map.Entry<String, Object> blockState : blockStates.entrySet()) {

			String value = "";
			if (blockState.getValue() instanceof String || blockState.getValue() instanceof Integer) {
				value = blockState.getValue().toString();
			} else if (blockState.getValue() instanceof Byte) {
				byte theByte = (byte) blockState.getValue();
				value = theByte == 0 ? "false" : "true";
			} else {
				System.out.println("Unknown type " + blockState.getValue().getClass());
			}

			bedrockBlockState.properties.put(blockState.getKey(), value);
		}
		return bedrockBlockState;
	}

}
