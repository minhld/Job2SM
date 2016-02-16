package com.minhld.job2p.jobs;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;

import com.minhld.job2p.supports.Utils;
import com.minhld.job2p.supports.WifiBroadcaster;

import java.io.File;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobDispatcher extends AsyncTask {
    private Activity context;
    private WifiBroadcaster broadcaster;
    private Handler socketHandler;
    private JobDataParser dataParser;

    String jobPath = "";
    String dataPath = "";
    String objJsonMetadata = "";
    boolean useCluster = true;

    public JobDispatcher(Activity c, WifiBroadcaster broadcaster, Handler socketHandler,
                         JobDataParser dataParser, boolean useCluster, String dataPath, String jobPath) {
        this.context = c;
        this.broadcaster = broadcaster;
        this.socketHandler = socketHandler;
        this.dataParser = dataParser;
        this.useCluster = useCluster;
        this.dataPath = dataPath;
        this.jobPath = jobPath;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        // start sending data
        Object orgObj = null;

        if (new File(dataPath).exists()) {
            try {
                // read data from the binary data
                orgObj = dataParser.readFile(dataPath);

                objJsonMetadata = dataParser.getJsonMetadata(orgObj);

                // depend on number of clients, we will split the number
                JobData jobData;
                Object splitObject;
                int deviceNum = Utils.connectedDevices.size() + 1;
                // get width of each slice
                //int pieceWidth = bmpWidth / deviceNum;

                for (int i = 0; i < deviceNum; i++) {
                    // create job data
                    splitObject = dataParser.getSinglePart(orgObj, deviceNum, i);//Bitmap.createBitmap(orgBmp, (pieceWidth * i), 0, pieceWidth, orgBmp.getHeight());
                    byte[] objectBytes = dataParser.parseObjectToBytes(splitObject);
                    jobData = new JobData(i, objectBytes, jobPath.getBytes());

                    // no longer need this data
                    dataParser.destroy(splitObject);

                    // and send to all the clients
                    // however it will skip the client 0, server will handle this
                    if (this.useCluster) {
                        if (i == 0) {
                            // do it at server
                            this.socketHandler.obtainMessage(Utils.MESSAGE_INFO, "[server] do own job #" + i);
                            new Thread(new JobExecutor(this.context, this.socketHandler, dataParser, jobData)).start();
                        } else {
                            // dispatch this one to client to resolve it
                            // it should be 32288 bytes to be sent
                            byte[] jobBytes = jobData.toByteArray();
                            this.broadcaster.sendObject(jobBytes, i);
                        }
                    } else {
                        // do all of the tasks at server
                        this.socketHandler.obtainMessage(Utils.MESSAGE_INFO, "[server] do own job #" + i).sendToTarget();
                        new Thread(new JobExecutor(this.context, this.socketHandler, dataParser, jobData)).start();
                    }
                }

                // release the original image
                dataParser.destroy(orgObj);

                publishProgress( objJsonMetadata );
            } catch (Exception e) {
                socketHandler.obtainMessage(Utils.JOB_FAILED, "[server] " + e.getMessage());
            } finally {
                if (dataParser.isObjectDestroyed(orgObj)) {
                    dataParser.destroy(orgObj);
                }
            }
        } else {
            // no file available
            publishProgress(null);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        //super.onProgressUpdate(values);

        // sending completed
        if (values.length > 0) {
            socketHandler.obtainMessage(Utils.MESSAGE_READ_JOB_SENT, values[0].toString()).sendToTarget();
        } else {
            socketHandler.obtainMessage(Utils.MESSAGE_READ_NO_FILE, "[server] data file unavailable");
        }
    }
}
