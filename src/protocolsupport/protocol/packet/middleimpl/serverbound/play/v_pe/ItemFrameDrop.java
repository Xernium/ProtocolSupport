package protocolsupport.protocol.packet.middleimpl.serverbound.play.v_pe;

import io.netty.buffer.ByteBuf;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.ServerBoundMiddlePacket;
import protocolsupport.protocol.packet.middle.serverbound.play.MiddleUseEntity;
import protocolsupport.protocol.packet.middleimpl.ServerBoundPacketData;
import protocolsupport.protocol.serializer.PositionSerializer;
import protocolsupport.protocol.utils.networkentity.NetworkEntity;
import protocolsupport.protocol.utils.types.Position;
import protocolsupport.protocol.utils.types.UsedHand;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableEmptyList;
import protocolsupport.utils.recyclable.RecyclableSingletonList;

public class ItemFrameDrop extends ServerBoundMiddlePacket {

	protected Position position = new Position(0, 0, 0);

	public ItemFrameDrop(ConnectionImpl connection) {
		super(connection);
	}

	@Override
	public void readFromClientData(ByteBuf clientdata) {
		PositionSerializer.readPEPositionTo(clientdata, position);
	}

	@Override
	public RecyclableCollection<ServerBoundPacketData> toNative() {
		NetworkEntity itemFrame = cache.getPETileCache().getItemFrameAt(position);
		if (itemFrame != null) {
			return RecyclableSingletonList.create(MiddleUseEntity.create(itemFrame.getId(), MiddleUseEntity.Action.ATTACK, null, UsedHand.MAIN));
		}
		return RecyclableEmptyList.get();
	}
}
