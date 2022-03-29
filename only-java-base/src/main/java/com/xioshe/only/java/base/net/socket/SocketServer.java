package com.xioshe.only.java.base.net.socket;

import java.io.*;
import java.net.Socket;

/**
 * 服务的抽象类
 *
 * @author xioshe 2022-03-03
 */
public interface SocketServer {

    /**
     * 服务具体实现比较灵活
     *
     * @param port 服务端口号
     */
    void startServer(int port);

    /**
     * 连接序号 + 1
     *
     * @return old sequence number
     */
    int getAndIncreaseSeq();

    /**
     * 查询当前连接序号
     *
     * @return current sequence number
     */
    int getSeq();
}
