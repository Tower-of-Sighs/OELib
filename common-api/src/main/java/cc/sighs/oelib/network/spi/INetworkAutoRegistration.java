package cc.sighs.oelib.network.spi;

import cc.sighs.oelib.network.api.INetworkPacket;

import java.util.Set;

public interface INetworkAutoRegistration {

    Set<Class<? extends INetworkPacket<?>>> findAnnotatedPackets(Set<String> basePackages);
}

