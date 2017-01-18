package com.rabtman.wsmanager;

/**
 * @author rabtman
 *
 */

public interface IWsManager {
    void connected();

    void disconnected();

    void buildConnect();

    void sendMessage(String msg);
}
