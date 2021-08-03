package net.cg360.spookums.server.core.event.type.network;

import net.cg360.spookums.server.core.event.type.Event;
import net.cg360.spookums.server.util.Check;

import java.util.UUID;

// Not cancellable as the protocol compatibility between the client + server
// hasn't been verified yet. Disconnect later on using the NetworkInterface.
public class ClientConnectionEvent extends Event {

    protected UUID clientNetID;

    public ClientConnectionEvent(UUID clientNetID) {
        Check.nullParam(clientNetID, "clientNetID");

        this.clientNetID = clientNetID;
    }


    public UUID getClientNetID() { return clientNetID; }

}
