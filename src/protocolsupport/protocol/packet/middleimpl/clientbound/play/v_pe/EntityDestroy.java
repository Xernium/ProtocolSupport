package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe;

import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleEntityDestroy;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupport.protocol.utils.networkentity.NetworkEntity;
import protocolsupport.protocol.utils.networkentity.NetworkEntityDataCache;
import protocolsupport.protocol.utils.networkentity.NetworkEntityType;
import protocolsupport.protocol.utils.types.Position;
import protocolsupport.utils.recyclable.RecyclableArrayList;
import protocolsupport.utils.recyclable.RecyclableCollection;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityDestroy extends MiddleEntityDestroy {

	protected Set<Position> destroyFrames = new HashSet();

	public EntityDestroy(ConnectionImpl connection) {
		super(connection);
	}

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		NetworkEntityDataCache dataCache = cache.getWatchedEntityCache().getSelfPlayer().getDataCache();
		RecyclableArrayList<ClientBoundPacketData> packets = RecyclableArrayList.create();
		for (int entityId : entityIds) {
			if (dataCache.getVehicleId() == entityId) // If the user is riding something and the vehicle is destroyed, the unlink packet is never sent, causing desync (and errors) with the cache
				dataCache.setVehicleId(0);
			packets.add(create(entityId));
		}
		for (Position pos : destroyFrames) {
			if (cache.getPEChunkMapCache().isMarkedAsSent(pos.getChunkCoord())) {
				packets.add(BlockChangeSingle.createRaw(pos, 0));
				cache.getPETileCache().updateForPosition(connection.getVersion(), pos, packets);
			}
		}
		return packets;
	}

	public static ClientBoundPacketData create(long entityId) {
		ClientBoundPacketData serializer = ClientBoundPacketData.create(PEPacketIDs.ENTITY_DESTROY);
		VarNumberSerializer.writeSVarLong(serializer, entityId);
		return serializer;
	}

	@Override
	public boolean postFromServerRead() {
		for (int entityId : entityIds) {
			NetworkEntity entity = cache.getWatchedEntityCache().getWatchedEntity(entityId);
			if (entity == null || !entity.getType().isOfType(NetworkEntityType.ITEM_FRAME)) {
				continue;
			}
			cache.getPETileCache().removeItemFrame(entity);
			destroyFrames.add(entity.getDataCache().getPosition());
		}
		return super.postFromServerRead();
	}

}
