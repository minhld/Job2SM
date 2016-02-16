package com.minhld.job2p.supports;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;


/**
 * Created by minhld on 9/17/2015.
 */
public class WifiBroadcaster extends BroadcastReceiver {
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    IntentFilter mIntentFilter;

    BroadCastListener broadCastListener;

    SocketHandler mSocketHandler;
    Handler mSocketUIListener;

    public void setSocketHandler(Handler skHandler) {
        this.mSocketUIListener = skHandler;
    }

    public WifiBroadcaster(Activity c){
        this.mManager = (WifiP2pManager)c.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(c, c.getMainLooper(), null);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled
                //writeLog("wifi p2p is enabled");
            } else {
                // Wi-Fi P2P is not enabled
                writeLog("wifi p2p is disabled");
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    Collection<WifiP2pDevice> deviceList = peers.getDeviceList();
                    writeLog(deviceList.size() + " devices was found");
                    if (broadCastListener != null){
                        broadCastListener.peerDeviceListUpdated(deviceList);
                    }
                }
            });
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            //writeLog("device's connection changed");

            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {
                    if (info.groupFormed && info.isGroupOwner) {
                        // if current device is a server
                        if (mSocketHandler != null && mSocketHandler.isSocketWorking() &&
                                mSocketHandler.socketType == Utils.SocketType.SERVER) {
                            writeLog("server is still be reused @ " + info.groupOwnerAddress.getHostAddress());
                        } else {
                            try {
                                mSocketHandler = new ServerSocketHandler(mSocketUIListener);
                                mSocketHandler.start();
                                writeLog("become server @ " + info.groupOwnerAddress.getHostAddress());
                            } catch (IOException e) {
                                e.printStackTrace();
                                writeLog("[wifi] error: " + e.getMessage());
                                return;     // we don't enable transmission
                            }
                        }
                        broadCastListener.socketUpdated(Utils.SocketType.SERVER, true);
                    } else if (info.groupFormed) {
                        // if current device is a client
                        mSocketHandler = new ClientSocketHandler(mSocketUIListener, info.groupOwnerAddress);
                        mSocketHandler.start();
                        broadCastListener.socketUpdated(Utils.SocketType.CLIENT, true);
                    } else {
                        // if something different happens, then close the ability of data transmission
                        broadCastListener.socketUpdated(Utils.SocketType.CLIENT, false);
                    }
                }
            });

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            //writeLog("device's wifi state changed");
        }

    }

    /**
     * send the request
     */
    public void discoverPeers(){
        this.mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                writeLog("discovery called successfully");
            }

            @Override
            public void onFailure(int reason) {
                switch (reason){
                    case 0: {
                        writeLog("discovery failed: operation failed due to an internal error");
                        break;
                    }
                    case 1: {
                        writeLog("discovery failed: p2p is unsupported on the device");
                        break;
                    }
                    case 2: {
                        writeLog("discovery failed: framework is busy and unable to service the request");
                        break;
                    }
                }
            }
        });
    }

    /**
     * this function creates an intent filter to only select the intents the broadcast
     * receiver checks for
     *
     * @return
     */
    public IntentFilter getSingleIntentFilter(){
        if (mIntentFilter == null) {
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        }

        return mIntentFilter;
    }

    /**
     * disconnect with a peer
     *
     * @param deviceName
     * @param listener
     */
    public void disconnect(final String deviceName, final WifiP2pConnectionListener listener){
        // close the current socket
        mSocketHandler.dispose();

        // dispose the group it connected to
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                writeLog("disconnected with " + deviceName + " successfully");
                if (listener != null) {
                    listener.connectInfoReturned(0);
                }
            }

            @Override
            public void onFailure(int reason) {
                writeLog("disconnected from " + deviceName + " failed");
            }
        });
    }

    public void connectToADevice(final WifiP2pDevice device, final WifiP2pConnectionListener listener) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                writeLog("connection established with " + device.deviceName + " successfully");

                if (listener != null) {
                    listener.connectInfoReturned(0);
                }
            }

            @Override
            public void onFailure(int reason) {
                writeLog("connection with " + device.deviceName + " failed");
                if (listener != null) {
                    listener.connectInfoReturned(reason);
                }
            }
        });
    }

    public WifiP2pManager getWifiP2pManager() {
        return this.mManager;
    }

    public void setBroadCastListener(BroadCastListener pdlcListener){
        this.broadCastListener = pdlcListener;
    }

    public interface BroadCastListener {
        public void peerDeviceListUpdated(Collection<WifiP2pDevice> deviceList);
        public void socketUpdated(Utils.SocketType socketType, boolean connected);
    }

    /**
     * this function will send an object through socket to the server
     *
     * @param st should be a serializable object
     */
    public void sendObject(Object st) {
        if (st instanceof byte[]) {
            mSocketHandler.write((byte[])st);
        }else {
            try {
                // we need to serialize it to binary array before dispatching it
                mSocketHandler.write(Utils.serialize(st));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * this function will send an object through socket from server to a
     * specific client using channel index
     *
     * @param st should be a serializable object
     * @param channelIndex index of each channel server connects to
     */
    public void sendObject(Object st, int channelIndex) {
        if (st instanceof byte[]) {
            mSocketHandler.write((byte[])st, channelIndex);
        }else {
            try {
                // we need to serialize it to binary array before dispatching it
                mSocketHandler.write(Utils.serialize(st), channelIndex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * write log to an output
     *
     * @param msg
     */
    public void writeLog(final String msg){
        String outMsg = Utils.SDF.format(new Date()) + ": " + msg;
        mSocketUIListener.obtainMessage(Utils.MESSAGE_INFO, outMsg).sendToTarget();
    }

}
