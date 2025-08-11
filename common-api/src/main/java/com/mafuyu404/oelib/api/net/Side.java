package com.mafuyu404.oelib.api.net;

/**
 * 网络包的目标端。
 */
public enum Side {
    /**
     * 仅客户端接收。
     */
    CLIENT,
    
    /**
     * 仅服务端接收。
     */
    SERVER,
    
    /**
     * 双端都接收。
     */
    BOTH
}