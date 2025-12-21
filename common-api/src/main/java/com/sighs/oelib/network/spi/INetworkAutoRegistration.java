package com.sighs.oelib.network.spi;

import com.sighs.oelib.network.api.INetworkPacket;

import java.util.Set;

public interface INetworkAutoRegistration {

    Set<Class<? extends INetworkPacket<?>>> findAnnotatedPackets(Set<String> basePackages);
}

