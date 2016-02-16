package com.minhld.job2p.supports;

import android.os.Handler;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by minhld on 10/23/2015.
 */
public class ChatManager implements Runnable {
    private static final String TAG = "ChatHandler";
    private static final int BUFF_LENGTH = 1024;
    private static final int LENGTH_SIZE = 4;

    private Utils.SocketType socketType;
    private Socket socket = null;
    private Handler handler;

    private InputStream iStream;
    private OutputStream oStream;

    public ChatManager(Utils.SocketType socketType, Socket socket, Handler handler) {
        this.socketType = socketType;
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();

            byte[] buffer = new byte[BUFF_LENGTH];

            //handler.obtainMessage(Utils.MESSAGE_INFO, "OK").sendToTarget();
            ByteArrayOutputStream byteStream = null;

            int readCount = 0, totalCount = 0;
            int length = 0;

            try {
                while (true) {

                    byteStream = new ByteArrayOutputStream();
                    length = 0;
                    totalCount = 0;
                    // read from the input stream
                    while ((readCount = iStream.read(buffer)) >= 0) {

                        if (length > 0) {
                            byteStream.write(buffer, 0, readCount);
                            totalCount += readCount;
                        } else {
                            // detect length of the package
                            byteStream.write(buffer, LENGTH_SIZE, readCount - LENGTH_SIZE);
                            totalCount = readCount - LENGTH_SIZE;
                            byte[] lengthBytes = Arrays.copyOfRange(buffer, 0, LENGTH_SIZE);
                            try {
                                length = (Integer) Utils.bytesToInt(lengthBytes);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // stop if all bytes are read
                        if (totalCount == length) {
                            length = 0;
                            totalCount = 0;
                            break;
                        }
                    }

                    if (readCount == -1) {
                        throw new IOException("socket should be disconnected");
                    }

                    // Send the obtained bytes to the UI Activity
                    if (socketType == Utils.SocketType.SERVER) {
                        handler.obtainMessage(Utils.MESSAGE_READ_SERVER, byteStream).sendToTarget();
                    } else {
                        handler.obtainMessage(Utils.MESSAGE_READ_CLIENT, byteStream).sendToTarget();
                    }

                }
            } catch (IOException e) {
                length = 0;
                totalCount = 0;
                Log.e(TAG, "disconnected", e);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            oStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }
}
