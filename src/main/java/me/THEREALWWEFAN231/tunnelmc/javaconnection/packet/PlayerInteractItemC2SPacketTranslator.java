package me.THEREALWWEFAN231.tunnelmc.javaconnection.packet;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;

import me.THEREALWWEFAN231.tunnelmc.TunnelMC;
import me.THEREALWWEFAN231.tunnelmc.bedrockconnection.Client;
import me.THEREALWWEFAN231.tunnelmc.translator.PacketTranslator;
import net.minecraft.world.entity.Pose;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class PlayerInteractItemC2SPacketTranslator extends PacketTranslator<ServerboundUseItemPacket> {

	//TODO: im not even fully sure about this, i dont really have any means to test it currently
	//actually i think i could test it on a chest, i should do that sometime

	@Override
	public void translate(ServerboundUseItemPacket packet) {

		ItemData usingItem = Client.instance.containers.getPlayerInventory().getItemFromSlot(TunnelMC.mc.player.getInventory().getSelectedSlot());

		if (TunnelMC.mc.hitResult.getType() == HitResult.Type.BLOCK) {
			BlockPos blockPos = ((BlockHitResult) TunnelMC.mc.hitResult).getBlockPos();
			Vector3i blockPosition = Vector3i.from(blockPos.getX(), blockPos.getY(), blockPos.getZ());

			Vec3 sideHitOffset = ((BlockHitResult) TunnelMC.mc.hitResult).getLocation().subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ());

			InventoryTransactionPacket useInventoryTransactionPacket = new InventoryTransactionPacket();
			useInventoryTransactionPacket.setTransactionType(InventoryTransactionType.ITEM_USE);
			useInventoryTransactionPacket.setActionType(0);
			useInventoryTransactionPacket.setBlockPosition(blockPosition);
			useInventoryTransactionPacket.setBlockFace(((BlockHitResult) TunnelMC.mc.hitResult).getDirection().ordinal());
			useInventoryTransactionPacket.setHotbarSlot(TunnelMC.mc.player.getInventory().getSelectedSlot());
			useInventoryTransactionPacket.setItemInHand(usingItem);
			useInventoryTransactionPacket.setPlayerPosition(Vector3f.from(TunnelMC.mc.player.getX(), TunnelMC.mc.player.getY() + TunnelMC.mc.player.getEyeHeight(Pose.STANDING), TunnelMC.mc.player.getZ()));
			useInventoryTransactionPacket.setClickPosition(Vector3f.from(sideHitOffset.x, sideHitOffset.y, sideHitOffset.z));
			Client.instance.sendPacket(useInventoryTransactionPacket);

		} else {
			//they used the item in air

			InventoryTransactionPacket inventoryTransactionPacket = new InventoryTransactionPacket();
			inventoryTransactionPacket.setTransactionType(InventoryTransactionType.ITEM_USE);
			inventoryTransactionPacket.setActionType(1);
			inventoryTransactionPacket.setBlockPosition(Vector3i.ZERO);
			inventoryTransactionPacket.setBlockFace(255);
			inventoryTransactionPacket.setHotbarSlot(TunnelMC.mc.player.getInventory().getSelectedSlot());
			inventoryTransactionPacket.setItemInHand(usingItem);
			inventoryTransactionPacket.setPlayerPosition(Vector3f.from(TunnelMC.mc.player.getX(), TunnelMC.mc.player.getY() + TunnelMC.mc.player.getEyeHeight(Pose.STANDING), TunnelMC.mc.player.getZ()));
			inventoryTransactionPacket.setClickPosition(Vector3f.ZERO);

			Client.instance.sendPacket(inventoryTransactionPacket);
		}

	}

	@Override
	public Class<?> getPacketClass() {
		return ServerboundUseItemPacket.class;
	}

}
