package me.THEREALWWEFAN231.tunnelmc.translator.blockstate;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.utils.FileManagement;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;

public class BlockStateTranslator {

	//TODO: create an override file, which allows us to override the blocks.json information and or create new information, once we do that fix all the printlns' from BlockPaletteTranslator
	//TODO: i also dont think water logged blocks work, i cant test right now as i cant connect to a dedicated bedrock server
	
	
	public static final HashMap<String, BlockState> BEDROCK_BLOCK_STATE_STRING_TO_JAVA_BLOCK_STATE = new HashMap<String, BlockState>();

	public static void load() {

		JsonObject jsonObject = TunnelMC.instance.fileManagement.getJsonObjectFromResource("geyser/blocks.json");
		if(jsonObject == null) {
			return;
		}

		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String javaBlockState = entry.getKey(); // could be for example, wheat[age=0]
			JsonObject blockEntry = entry.getValue().getAsJsonObject();

			BedrockBlockState bedrockBlockState = new BedrockBlockState();
			bedrockBlockState.identifier = blockEntry.get("bedrock_identifier").getAsString();

			if (blockEntry.has("bedrock_states")) {
				for (Map.Entry<String, JsonElement> stateEntry : blockEntry.get("bedrock_states").getAsJsonObject().entrySet()) {
					if (!(stateEntry.getValue() instanceof JsonPrimitive)) {
						continue;
					}

					JsonPrimitive jsonPrimitive = (JsonPrimitive) stateEntry.getValue();
					String value;

					if (jsonPrimitive.isBoolean()) {
						value = Boolean.valueOf(jsonPrimitive.getAsBoolean()).toString();
					} else if (jsonPrimitive.isNumber()/* && jsonPrimitive.getAsNumber() instanceof Integer*/) {//gson uses "LazilyParsedNumber" ree, this causes me some pain, hopefully we can find a better solution some time, that doesn't use jsonPrimitive.toString, although, its not needed right now, as there are no double properties
						value = jsonPrimitive.getAsInt() + "";
					} else if (jsonPrimitive.isString()) {
						value = jsonPrimitive.getAsString();
					} else {
						System.out.println("Unknown block state value, key=" + stateEntry.getKey() + " value=" + jsonPrimitive.toString() + ":" + (jsonPrimitive.getAsNumber().getClass()));
						continue;
					}

					bedrockBlockState.properties.put(stateEntry.getKey(), value);
				}
			}

			if (entry.getKey().equals("minecraft:water[level=1]")) {
				System.out.println(bedrockBlockState.toString());
			}

			BlockState blockState = BlockStateTranslator.parseBlockState(javaBlockState);
			if (blockState == null) { // we print in the parseBlockState method
				continue;
			}

			BEDROCK_BLOCK_STATE_STRING_TO_JAVA_BLOCK_STATE.put(bedrockBlockState.toString(), blockState);

		}

		InputStream stream = FileManagement.class.getClassLoader().getResourceAsStream("tunnelmc/blockpalette.nbt");
		if (stream == null) {
			throw new RuntimeException("Could not find the block palette file!");
		}

		NbtList<NbtMap> blocksTag;
		try (NBTInputStream nbtInputStream = new NBTInputStream(new DataInputStream(new GZIPInputStream(stream)))) {
			NbtMap blockPalette = (NbtMap) nbtInputStream.readTag();
			blocksTag = (NbtList<NbtMap>) blockPalette.getList("blocks", NbtType.COMPOUND);
		} catch (Exception e) {
			throw new AssertionError("Unable to get blocks from runtime block states", e);
		}
		BlockPaletteTranslator.loadMap(blocksTag);

	}

	private static BlockState parseBlockState(String blockStateInformation) {//parses for example wheat[age=0]

		String javaBlockIdentifier = "";

		int firstLeftBracketIndex = blockStateInformation.indexOf("[");
		if (firstLeftBracketIndex != -1) {//if its found
			javaBlockIdentifier = blockStateInformation.substring(0, firstLeftBracketIndex);
		} else {
			javaBlockIdentifier = blockStateInformation;
		}

		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(javaBlockIdentifier));
		//do not use block instanceof AirBlock, as there is void_air and cave_air, i guess, never knew they existed
		if (block == Blocks.AIR && !javaBlockIdentifier.equals("minecraft:air")) {//BuiltInRegistries.BLOCK.getValue returns air if its not found, so if this is true, the block is not found, and this generally isn't good
			System.out.println(javaBlockIdentifier + " block was not found, this generally isn't good.");
			return null;
		}

		BlockState theBlockState = block.defaultBlockState();

		if (firstLeftBracketIndex != -1) {
			String blockProperties = blockStateInformation.substring(firstLeftBracketIndex + 1, blockStateInformation.length() - 1);

			String[] blockProperyKeysAndValues = blockProperties.split(",");

			for (String keyAndValue : blockProperyKeysAndValues) {
				String[] keyAndValueArray = keyAndValue.split("=");
				String key = keyAndValueArray[0];
				String value = keyAndValueArray[1];

				Property<?> property = block.getStateDefinition().getProperty(key);

				theBlockState = parsePropertyValue(theBlockState, property, value);
				if (theBlockState == null) {
					System.out.println("Could not find the state " + key + " or set the value " + value + " " + blockStateInformation);
					return null;
				}
			}

		}

		return theBlockState;

	}

	private static <T extends Comparable<T>> BlockState parsePropertyValue(BlockState before, Property<T> property, String value) {//from the value command, jesus christ, i could barely get this to work, all this generic stuff :flushed:
		Optional<T> optional = property.getValue(value);
		if (optional.isPresent()) {
			return before.setValue(property, optional.get());
		}
		return null;
	}

}
