package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe;

import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleBlockTileUpdate;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.serializer.ItemStackSerializer;
import protocolsupport.protocol.serializer.PositionSerializer;
import protocolsupport.protocol.typeremapper.tile.TileEntityRemapper;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupport.protocol.utils.types.Position;
import protocolsupport.protocol.utils.types.TileEntity;
import protocolsupport.protocol.utils.types.nbt.NBTCompound;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableSingletonList;

public class BlockTileUpdate extends MiddleBlockTileUpdate {

	public BlockTileUpdate(ConnectionImpl connection) {
		super(connection);
	}

	protected final TileEntityRemapper tileremapper = TileEntityRemapper.getRemapper(connection.getVersion());

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		return RecyclableSingletonList.create(create(connection.getVersion(), tileremapper.remap(tile, cache.getTileCache().getBlockData(position))));
	}

	public static ClientBoundPacketData create(ProtocolVersion version, TileEntity tile) {
		return create(version, tile.getPosition(), tile.getNBT());
	}

	public static ClientBoundPacketData create(ProtocolVersion version, Position position, NBTCompound nbt) {
		ClientBoundPacketData serializer = ClientBoundPacketData.create(PEPacketIDs.TILE_DATA_UPDATE);
		PositionSerializer.writePEPosition(serializer, position);
		ItemStackSerializer.writeTag(serializer, true, version, nbt);
		return serializer;
	}

}
