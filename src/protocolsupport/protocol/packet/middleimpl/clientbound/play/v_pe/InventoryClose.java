package protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe;

import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.middle.clientbound.play.MiddleInventoryClose;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupport.protocol.typeremapper.pe.inventory.fakes.PEFakeContainer;
import protocolsupport.utils.recyclable.RecyclableCollection;
import protocolsupport.utils.recyclable.RecyclableSingletonList;

public class InventoryClose extends MiddleInventoryClose {

	public InventoryClose(ConnectionImpl connection) {
		super(connection);
	}

	@Override
	public RecyclableCollection<ClientBoundPacketData> toData() {
		cache.getPEInventoryCache().getTransactionRemapper().clear();
		cache.getPEInventoryCache().setPreviousWindowId(0);
		PEFakeContainer.destroyContainers(connection, cache);
		return RecyclableSingletonList.create(create(windowId));
	}

	public static ClientBoundPacketData create(int windowId) {
		ClientBoundPacketData serializer = ClientBoundPacketData.create(PEPacketIDs.CONTAINER_CLOSE);
		serializer.writeByte(windowId);
		return serializer;
	}

}
