package com.minhld.job2p.supports;

import android.os.AsyncTask;
import android.os.Handler;

import com.minhld.job2p.jobs.JobData;

/**
 * the ACK exchanger supports sending ACK back and forth between the peers
 * to discover each others
 *
 * Created by minhld on 2/10/2016.
 */
public class ACKExchanger extends AsyncTask {

    private WifiBroadcaster broadcaster;
    private Handler socketHandler;

    public ACKExchanger(WifiBroadcaster broadcaster, Handler socketHandler) {
        this.broadcaster = broadcaster;
        this.socketHandler = socketHandler;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        try {
            int deviceNum = Utils.connectedDevices.size() + 1;

            JobData jobData;
            for (int i = 1; i < deviceNum; i++) {
                jobData = new JobData(i, Utils.JOB_TYPE_ACK, Utils.MSG_ACK.getBytes(), new byte[0]);

                byte[] jobBytes = jobData.toByteArray();
                this.broadcaster.sendObject(jobBytes, i);
            }

            publishProgress();
        } catch (Exception e) {
            socketHandler.obtainMessage(Utils.JOB_FAILED, "[server] " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        // when all the ACK has been dispatched
    }
}