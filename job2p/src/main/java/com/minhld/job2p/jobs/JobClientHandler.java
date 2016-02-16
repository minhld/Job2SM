package com.minhld.job2p.jobs;

import android.os.Handler;
import android.os.Message;

import com.minhld.job2p.supports.Utils;
import com.minhld.job2p.supports.WifiBroadcaster;

/**
 * Created by minhld on 11/6/2015.
 */
public class JobClientHandler extends Handler {
    Handler mainUiHandler;
    JobDataParser dataParser;
    WifiBroadcaster mReceiver;

    public JobClientHandler(Handler uiHandler, JobDataParser dataParser) {
        this.mainUiHandler = uiHandler;
        this.dataParser = dataParser;
    }

    public void setBroadcaster(WifiBroadcaster mReceiver) {
        this.mReceiver = mReceiver;
    }

    public WifiBroadcaster getBroadcaster() {
        return this.mReceiver;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Utils.JOB_OK: {
                // finish
                // when client completed the job and send back to server
                JobData jobData = (JobData) msg.obj;
                mReceiver.sendObject(jobData.toByteArray(), jobData.index);

                // displaying small image on client device
                try {
                    Object pieceObj = dataParser.parseBytesToObject(jobData.byteData);
                    mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[client] job done. send back result.").sendToTarget();
                    mainUiHandler.obtainMessage(Utils.MAIN_JOB_DONE, pieceObj).sendToTarget();
                } catch (Exception e) {
                    e.printStackTrace();
                    mainUiHandler.obtainMessage(Utils.MAIN_INFO,
                                "exception: " + e.getMessage()).sendToTarget();
                }
                break;
            }
            case Utils.JOB_FAILED: {
                // send some error data
                break;
            }
        }
    }
}
