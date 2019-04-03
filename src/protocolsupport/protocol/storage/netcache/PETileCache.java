package protocolsupport.protocol.storage.netcache;

import com.google.common.collect.HashMultimap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.serverbound.play.MiddleUpdateSign;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.packet.middleimpl.ServerBoundPacketData;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe.BlockChangeSingle;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe.BlockTileUpdate;
import protocolsupport.protocol.typeremapper.entity.EntityRemapper;
import protocolsupport.protocol.typeremapper.itemstack.ItemStackRemapper;
import protocolsupport.protocol.typeremapper.pe.PEBlocks;
import protocolsupport.protocol.utils.CommonNBT;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObjectIndex;
import protocolsupport.protocol.utils.networkentity.NetworkEntity;
import protocolsupport.protocol.utils.types.Position;
import protocolsupport.protocol.utils.types.ChunkCoord;
import protocolsupport.protocol.utils.types.NetworkItemStack;
import protocolsupport.protocol.utils.types.nbt.NBTByte;
import protocolsupport.protocol.utils.types.nbt.NBTCompound;
import protocolsupport.protocol.utils.types.nbt.NBTType;
import protocolsupport.utils.recyclable.RecyclableCollection;

import java.util.HashMap;
import java.util.Set;

public class PETileCache {

	private NBTCompound signTag = null;

	public void updateSignTag(NBTCompound signTag) {
		this.signTag = signTag;
	}

	public boolean shouldSignSign() {
		return signTag != null;
	}

	public void signSign(RecyclableCollection<ServerBoundPacketData> packets) {
		int x = signTag.getNumberTag("x").getAsInt();
		int y = signTag.getNumberTag("y").getAsInt();
		int z = signTag.getNumberTag("z").getAsInt();
		String[] nbtLines = new String[4];
		String[] lines = signTag.getTagOfType("Text", NBTType.STRING).getValue().split("\n");
		for (int i = 0; i < nbtLines.length; i++) {
			if (lines.length > i) {
				nbtLines[i] = lines[i];
			} else {
				nbtLines[i] = "";
			}
		}
		signTag = null;
		packets.add(MiddleUpdateSign.create(new Position(x, y, z), nbtLines));
	}

	private HashMultimap<ChunkCoord, NetworkEntity> itemFrameChunkMap = HashMultimap.create();
	private HashMultimap<Position, NetworkEntity> itemFramePositionMap = HashMultimap.create();
	private HashMap<NetworkEntity, NetworkItemStack> itemFrameItem = new HashMap();
	private Object2IntOpenHashMap<NetworkEntity> itemFrameRotation = new Object2IntOpenHashMap();
	private Object2ByteOpenHashMap<NetworkEntity> itemFrameFacing = new Object2ByteOpenHashMap();

	protected static short getPEData(int facing) {
		switch (facing) {
			case 3:
				return 2;
			case 4:
				return 1;
			case 2:
				return 3;
			case 5:
			default:
				return 0;
		}
	}

	public void clear() {
		itemFrameChunkMap.clear();
		itemFramePositionMap.clear();
		itemFrameItem.clear();
		itemFrameRotation.clear();
		itemFrameFacing.clear();
	}

	public void addItemFrame(NetworkEntity itemFrame, int facing) {
		itemFrameChunkMap.put(itemFrame.getDataCache().getChunkCoord(), itemFrame);
		itemFramePositionMap.put(itemFrame.getDataCache().getPosition(), itemFrame);
		itemFrameFacing.put(itemFrame, (byte) facing);
	}

	public NetworkEntity getItemFrameAt(Position position) {
		Set<NetworkEntity> entities = itemFramePositionMap.get(position);
		if (entities.isEmpty()) {
			return null;
		}
		return entities.iterator().next();
	}

	public void updateItemFrame(ConnectionImpl connection, NetworkEntity entity, EntityRemapper entityRemapper) {
		DataWatcherObjectIndex.ItemFrame.ITEM.getValue(entityRemapper.getOriginalMetadata()).ifPresent(itemData -> {
			NetworkItemStack pcStack = itemData.getValue();
			if (pcStack == null || pcStack.isNull()) {
				itemFrameItem.put(entity, null);
			} else {
				NetworkItemStack peStack = ItemStackRemapper.remapToClient(connection.getVersion(),
					connection.getCache().getAttributesCache().getLocale(), pcStack.cloneItemStack());
				itemFrameItem.put(entity, peStack);
			}
		});
		DataWatcherObjectIndex.ItemFrame.ROTATION.getValue(entityRemapper.getOriginalMetadata()).ifPresent(rotation -> {
			itemFrameRotation.put(entity, rotation.getValue().intValue());
		});
	}

	public void removeItemFrame(NetworkEntity itemFrame) {
		itemFrameChunkMap.remove(itemFrame.getDataCache().getChunkCoord(), itemFrame);
		itemFramePositionMap.remove(itemFrame.getDataCache().getPosition(), itemFrame);
		itemFrameItem.remove(itemFrame);
		itemFrameRotation.removeInt(itemFrame);
		itemFrameFacing.removeByte(itemFrame);
	}

	public void updateForEntity(ProtocolVersion version, NetworkEntity entity, RecyclableCollection<ClientBoundPacketData> packets) {
		NetworkItemStack itemStack = itemFrameItem.getOrDefault(entity, NetworkItemStack.NULL);
		int facing = itemFrameFacing.getByte(entity);
		int blockId = PEBlocks.getPocketRuntimeId(new PEBlocks.PEBlock("minecraft:frame", getPEData(facing)));
		int rotation = itemFrameRotation.getOrDefault(entity, 0);
		Position position = entity.getDataCache().getPosition();
		NBTCompound nbt = new NBTCompound();
		if (itemStack != null && !itemStack.isNull()) {
			nbt.setTag("Item", CommonNBT.createItemNBT(itemStack));
			nbt.setTag("ItemRotation", new NBTByte((byte) rotation));
		}
		packets.add(BlockChangeSingle.createRaw(position, blockId));
		packets.add(BlockTileUpdate.create(version, position, nbt));
	}

	public void updateForPosition(ProtocolVersion version, Position position, RecyclableCollection<ClientBoundPacketData> packets) {
		for (NetworkEntity entity : itemFramePositionMap.get(position)) {
			updateForEntity(version, entity, packets);
		}
	}

	public void updateForChunk(ProtocolVersion version, ChunkCoord chunk, RecyclableCollection<ClientBoundPacketData> packets) {
		for (NetworkEntity entity : itemFrameChunkMap.get(chunk)) {
			updateForEntity(version, entity, packets);
		}
	}
}
