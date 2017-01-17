package com.rabtman.wsmanager;

/**
 * @author zjm
 * @Description:
 * @date 2017/1/12
 */

public interface IWsManager {
    void connected();

    void disconnected();

    void buildConnect();

    void sendMessage(String msg);
}
