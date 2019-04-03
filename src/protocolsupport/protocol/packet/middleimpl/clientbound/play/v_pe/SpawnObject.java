package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe;

import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleSpawnObject;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe.EntityMetadata.PeMetaBase;
import protocolsupport.protocol.serializer.DataWatcherSerializer;
import protocolsupport.protocol.typeremapper.itemstack.ItemStackRemapper;
import protocolsupport.protocol.typeremapper.pe.PEBlocks;
import protocolsupport.protocol.typeremapper.pe.PEDataValues;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObject;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObjectIndex;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectItemStack;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectSVarInt;
import protocolsupport.protocol.utils.networkentity.NetworkEntityDataCache;
import protocolsupport.protocol.utils.networkentity.NetworkEntityItemDataCache;
import protocolsupport.protocol.utils.types.NetworkItemStack;
import protocolsupport.utils.CollectionsUtils.ArrayMap;
import protocolsupport.utils.recyclable.RecyclableArrayList;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableEmptyList;
import protocolsupport.utils.recyclable.RecyclableSingletonList;
import protocolsupport.zplatform.ServerPlatform;

public class SpawnObject extends MiddleSpawnObject {

	public SpawnObject(ConnectionImpl connection) {
		super(connection);
	}

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		ProtocolVersion version = connection.getVersion();
		ArrayMap<DataWatcherObject<?>> spawnmeta = null;
		NetworkEntityDataCache dataCache = entity.getDataCache();
		switch (entity.getType()) {
			case ITEM: {
				((NetworkEntityItemDataCache) dataCache).setData(x, y, z, motX / 8000F, motY / 8000F, motZ / 8000F);
				return RecyclableEmptyList.get();
			}
			case ITEM_FRAME: {
				RecyclableArrayList<ClientBoundPacketData> packets = RecyclableArrayList.create();
				dataCache.setPos((float) x, (float) y, (float) z);
				cache.getPETileCache().addItemFrame(entity, objectdata);
				cache.getPETileCache().updateItemFrame(connection, entity, entityRemapper);
				if (cache.getPEChunkMapCache().isMarkedAsSent(entity)) {
					//System.out.println("item frame chunk already sent ");
					cache.getPETileCache().updateForChunk(version, dataCache.getChunkCoord(), packets);
				}
				return packets;
			}
			case FALLING_OBJECT: {
				spawnmeta = new ArrayMap<>(DataWatcherSerializer.MAX_USED_META_INDEX + 1);
				int pocketBlock = PEBlocks.toPocketBlock(version, ServerPlatform.get().getMiscUtils().getBlockDataByNetworkId(objectdata));
				y -= 0.1; //Freaking PE pushes block because block breaks after sand is spawned
				spawnmeta.put(PeMetaBase.VARIANT, new DataWatcherObjectSVarInt(pocketBlock));
			}
			default: {
				PEDataValues.PEEntityData typeData = PEDataValues.getEntityData(entity.getType());
				if (typeData != null && typeData.getOffset() != null) {
					PEDataValues.PEEntityData.Offset offset = typeData.getOffset();
					x += offset.getX();
					y += offset.getY();
					z += offset.getZ();
					pitch += offset.getPitch();
					yaw += offset.getYaw();
				}
				dataCache.setPos((float) x, (float) y, (float) z);
				dataCache.setYaw(yaw);
				dataCache.setPitch(pitch);
				return RecyclableSingletonList.create(SpawnLiving.create(
					version, cache.getAttributesCache().getLocale(),
					entity,
					(float) x, (float) y, (float) z,
					motX / 8000.F, motY / 8000.F, motZ / 8000.F,
					pitch * 360.F / 256.F, yaw * 360.F / 256.F, 0,
					spawnmeta
				));
			}
		}
	}

}
