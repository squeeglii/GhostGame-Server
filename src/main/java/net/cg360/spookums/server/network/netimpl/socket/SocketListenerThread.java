package net.cg360.spookums.server.network.netimpl.socket;

import net.cg360.spookums.server.network.netimpl.NetworkInterface;

import java.util.UUID;

public class SocketListenerThread extends Thread {

    protected UUID clientUUID;
    protected NetworkInterface networkInterface;

    public SocketListenerThread(UUID clientUUID, NetworkInterface netInf) {
        this.clientUUID = clientUUID;
        this.networkInterface = netInf;
    }

    @Override
    public void run() {

        while (networkInterface.isRunning()) {
            networkInterface.checkForInboundPackets(clientUUID);
        }

    }

    @Override
    public void interrupt() {

    }

}
