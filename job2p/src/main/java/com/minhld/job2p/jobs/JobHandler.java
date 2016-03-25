package com.minhld.job2p.jobs;

import android.app.Activity;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;

import com.minhld.job2p.supports.ACKExchanger;
import com.minhld.job2p.supports.Utils;
import com.minhld.job2p.supports.WifiBroadcaster;

import java.util.Collection;

/**
 * this class handles everything regarding job executions and handlers
 *
 * Created by minhld on 11/23/2015.
 * Have been updated on 02/02/2016
 */
public class JobHandler {
    Activity context;
    Handler uiHandler;

    JobDataParser dataParser;
    JobClientHandler clientHandler;
    JobServerHandler serverHandler;

    WifiBroadcaster mReceiver;
    public WifiBroadcaster getConnector() {
        return this.mReceiver;
    }

    IntentFilter mIntentFilter;



    JobSocketListener jobSocketListener;
    public void setSocketListener(JobSocketListener jobSocketListener) {
        this.jobSocketListener = jobSocketListener;
    }

    public JobHandler(Activity c, Handler uiHandler, JobDataParser dataParser) {
        this.context = c;
        this.uiHandler = uiHandler;
        this.dataParser = dataParser;

        // load the configuration, the configuration will be saved in Utils
        // object which is a singleton to use between the layers
        Utils.readConfigs(c);

        clientHandler = new JobClientHandler(uiHandler, dataParser);
        serverHandler = new JobServerHandler(this.context, uiHandler, clientHandler, dataParser);

        // configure wifi receiver
        mReceiver = new WifiBroadcaster(this.context);
        mReceiver.setBroadCastListener(new BroadcastUpdatesHandler());
        mReceiver.setSocketHandler(serverHandler);

        clientHandler.setBroadcaster(mReceiver);

        discoverPeers();

    }

    /**
     * discover the peers in the WiFi peer-to-peer mobile network
     */
    public void discoverPeers() {
        // start discovering
        mReceiver.discoverPeers();
        mIntentFilter = mReceiver.getSingleIntentFilter();
    }

    /**
     * split the job into tasks, and dispatch to other peers
     *
     * @param useCluster
     */
    public void dispatchJob(boolean useCluster, String dataPath, String jobPath) {
//        // exchange ACKs between the devices to get device status to support
//        // decision maker to select available peers
//        ACKExchanger exchanger = new ACKExchanger(this.mReceiver, this.serverHandler);
//        exchanger.execute();
//
//        try {
//            Thread.sleep(500);
//        } catch(Exception e) { }

        // start dispatching jobs after decision maker select the available peers
        new JobDispatcher(context, mReceiver, serverHandler, dataParser,
                                useCluster, dataPath, jobPath).execute();
    }

    /**
     * this should be added at the end of onPause on main activity
     */
    public void actOnPause() {
        if (mReceiver != null && mIntentFilter != null) {
            this.context.unregisterReceiver(mReceiver);
        }
    }

    /**
     * this should be added at the end of onResume on main activity
     */
    public void actOnResume() {
        if (mReceiver != null && mIntentFilter != null) {
            this.context.registerReceiver(mReceiver, mIntentFilter);
        }
    }

    /**
     * this class handles the device list when it is updated
     */
    private class BroadcastUpdatesHandler implements WifiBroadcaster.BroadCastListener {
        @Override
        public void peerDeviceListUpdated(Collection<WifiP2pDevice> deviceList) {
            jobSocketListener.peerListUpdated(deviceList);

            // check if it is server, it won't connect but only listening
            if (Utils.getConfig("role").equals("server")) {
                return;
            }

            // if current device is client, it will auto connect through
            // the list of devices
//            for (WifiP2pDevice device : deviceList) {
//                if (device.status == Utils.WiFiDirectStatus.AVAILABLE) {
//                    mReceiver.connectToADevice(device, null);
//                }
//            }
        }

        @Override
        public void socketUpdated(final Utils.SocketType socketType, final boolean connected) {
            if (jobSocketListener != null) {
                jobSocketListener.socketUpdated(socketType == Utils.SocketType.SERVER, connected);
            }
        }
    }

    /**
     * listener - for updates from socket and peer list
     */
    public interface JobSocketListener {
        /**
         * when socket status is updated
         * @param isServer
         * @param isConnected
         */
        public void socketUpdated(boolean isServer, boolean isConnected);

        /**
         * when peer list is updated
         * @param deviceList
         */
        public void peerListUpdated(Collection<WifiP2pDevice> deviceList);
    }
}
