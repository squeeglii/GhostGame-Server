package net.cg360.spookums.server.network.packet.type;

import net.cg360.spookums.server.network.packet.NetworkPacket;

public abstract class PacketInOutEmpty extends NetworkPacket {

    @Override protected final short encodeBody() { return 0; }
    @Override protected final void decodeBody(short inboundSize) { }

}