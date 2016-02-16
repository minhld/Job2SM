package com.minhld.job2p.supports;

import android.os.Handler;

/**
 * Created by minhld on 11/1/2015.
 */
public abstract class SocketHandler extends Thread {
    protected Handler handler;
    protected Utils.SocketType socketType;

    public SocketHandler(Handler handler) {
        this.handler = handler;
        this.socketType = Utils.SocketType.SERVER;
    }

    /**
     * write some piece of data into the socket to dispatch to peers
     *
     * @param data
     */
    public abstract void write(byte[] data);

    /**
     * write data into the socket to a specific client
     *
     * @param data
     * @param channelIndex
     */
    public abstract void write(byte[] data, int channelIndex);

    /**
     * to dispose current socket. this only works on server socket.
     */
    public abstract void dispose();

    /**
     * check if socket is still working.
     * this only works on server socket. on client socket, it will always return true
     *
     * @return
     */
    public abstract boolean isSocketWorking();

    /**
     * dispatch messages
     *
     * @param msg
     */
    public void writeLog(String msg) {
        this.handler.obtainMessage(Utils.MESSAGE_INFO, msg).sendToTarget();
    }
}
