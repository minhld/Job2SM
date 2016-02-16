package com.minhld.job2p.supports;

import android.os.Handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The implementation of a ServerSocket handler. This is used by the wifi p2p
 * group owner.
 */
public class ServerSocketHandler extends SocketHandler {
    ServerSocket socket = null;
    private final int THREAD_COUNT = 10;

    // A ThreadPool for client sockets.
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
                        THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>());

    // list of devices to monitor
    ArrayList<ChatManager> chatList = new ArrayList<>();

    public ServerSocketHandler(Handler handler) throws IOException {
        super(handler);
        chatList = new ArrayList<>();
        Utils.connectedDevices = new ArrayList<>();

        try {
            socket = new ServerSocket(Utils.SERVER_PORT);
            this.handler = handler;
            writeLog("[server] socket started");
        } catch (IOException e) {
            e.printStackTrace();
            writeLog("[server-cons] exception: " + e.getMessage());
            pool.shutdownNow();
            throw e;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                // A blocking operation. Initiate a ChatManager instance when
                // there is a new connection
                ChatManager chat = new ChatManager(Utils.SocketType.SERVER, socket.accept(), handler);
                pool.execute(chat);
                chatList.add(chat);
                Utils.connectedDevices.add(new Utils.XDevice());

                writeLog("[server] launching I/O handler");
            } catch (IOException e) {
                // problem with server socket
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (IOException ioe) { }
                e.printStackTrace();
                pool.shutdownNow();
                writeLog("[server-run] exception: " + e.getMessage());
                break;
            }
        }
    }

    @Override
    public void write(byte[] data) {
        // send the same data to all clients
        for (ChatManager chat : chatList) {
            chat.write(data);
        }
    }

    @Override
    public void write(byte[] data, int channelIndex) {

        // send data to each client
        if (channelIndex > 0 && channelIndex - 1 < chatList.size()) {
            chatList.get(channelIndex - 1).write(data);
        }
    }

    @Override
    public void dispose() {
        try {
            // shutdown the thread pool and socket
            pool.shutdownNow();
            chatList.clear();
            Utils.connectedDevices.clear();
            socket.close();
            writeLog("[server-dispose] close server socket");
        }catch(IOException e) {
            e.printStackTrace();
            writeLog("[server-dispose] exception: " + e.getMessage());
        }
    }

    @Override
    public boolean isSocketWorking() {
        return !socket.isClosed();
    }

}
