package com.webank.wecross.stub;

public interface StubFactory {
    /**
     * create a driver
     *
     * @return
     */
    public Driver newDriver();

    /**
     * create a connection
     *
     * @return Connection
     */
    public Connection newConnection(String path);

    /** load account */
    public Account newAccount(String name, String path);
}