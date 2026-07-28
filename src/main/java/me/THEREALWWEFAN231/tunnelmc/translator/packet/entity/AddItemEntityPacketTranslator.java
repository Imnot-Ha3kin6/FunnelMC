package me.THEREALWWEFAN231.tunnelmc.translator.packet.entity;

import java.util.UUID;

import org.cloudburstmc.protocol.bedrock.packet.AddItemEntityPacket;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import me.THEREALWWEFAN231.tunnelmc.translator.item.ItemTranslator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.phys.Vec3;

public class AddItemEntityPacketTranslator extends PacketTranslator<AddItemEntityPacket> {

	@Override
	public void translate(AddItemEntityPacket packet) {

		int id = (int) packet.getUniqueEntityId();
		double x = packet.getPosition().getX();
		double y = packet.getPosition().getY();
		double z = packet.getPosition().getZ();
		double motionX = packet.getMotion().getX();
		double motionY = packet.getMotion().getY();
		double motionZ = packet.getMotion().getZ();

		EntityType<?> itemEntityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("item"));
		ItemEntity itemEntity = (ItemEntity) itemEntityType.create(TunnelMC.mc.level, EntitySpawnReason.LOAD);
		itemEntity.setId(id);
		itemEntity.setPos(x, y, z);
		itemEntity.setDeltaMovement(motionX, motionY, motionZ);
		itemEntity.setItem(ItemTranslator.itemDataToItemStack(packet.getItemInHand()));
		itemEntity.setUUID(UUID.randomUUID());

		ClientboundAddEntityPacket addEntityPacket = new ClientboundAddEntityPacket(id, itemEntity.getUUID(), x, y, z, 0, 0, itemEntityType, 0, new Vec3(motionX, motionY, motionZ), 0);
		Client.instance.javaConnection.processServerToClientPacket(addEntityPacket);

		ClientboundSetEntityDataPacket entityTrackerUpdateS2CPacket = new ClientboundSetEntityDataPacket(id, itemEntity.getEntityData().packDirty());
		Client.instance.javaConnection.processServerToClientPacket(entityTrackerUpdateS2CPacket);
	}

	@Override
	public Class<?> getPacketClass() {
		return AddItemEntityPacket.class;
	}

}
