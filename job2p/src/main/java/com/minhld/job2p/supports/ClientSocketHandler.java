package com.minhld.job2p.supports;

import android.os.Handler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocketHandler extends SocketHandler {
    private ChatManager chat;
    private InetAddress mAddress;

    public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress) {
        super(handler);
        this.mAddress = groupOwnerAddress;
        this.socketType = socketType.CLIENT;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            // initiate client socket
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                            Utils.SERVER_PORT), Utils.SERVER_TIMEOUT);
            writeLog("[client] launching the I/O handler");

            // connect it to a chat manager
            chat = new ChatManager(Utils.SocketType.CLIENT, socket, handler);
            new Thread(chat).start();
        } catch (IOException e) {
            e.printStackTrace();
            writeLog("[client] exception: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }

    @Override
    public void write(byte[] data) {
        chat.write(data);
    }

    @Override
    public void write(byte[] data, int channelIndex) {
        chat.write(data);
    }

    @Override
    public void dispose() {
        // close socket here

    }

    @Override
    public boolean isSocketWorking() {
        return true;
    }

    public ChatManager getChat() {
        return chat;
    }
}