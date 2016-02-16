package com.minhld.job2p.jobs;

import android.app.Activity;
import android.os.Handler;

import com.minhld.job2p.supports.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobExecutor extends ClassLoader implements Runnable {
    Activity context;
    JobData jobData;
    private Handler handler;
    private JobDataParser dataParser;

    /**
     * this constructor is used in client mode, when client receives a package
     * from server. it then saves the job jar file into Download folder, and
     * execute the inside class to get the result and send back to server
     *
     * @param c
     * @param handler
     * @param jobDataBytes
     */
    public JobExecutor(Activity c, Handler handler, JobDataParser dataParser,
                       ByteArrayOutputStream jobDataBytes) {
        this.context = c;
        this.handler = handler;
        this.dataParser = dataParser;

        try {
            this.jobData = (JobData) Utils.deserialize(jobDataBytes.toByteArray());

            // save the job class to Download folder
            saveJobFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * this constructor is used by server mode, when server try to use it for running
     * the first piece of an object. the app will check if the job jar file exists in
     * the Download folder, and use it to run the very first result
     *
     * @param c
     * @param handler
     * @param jobData
     */
    public JobExecutor(Activity c, Handler handler, JobDataParser dataParser,
                       JobData jobData) {
        this.context = c;
        this.handler = handler;
        this.dataParser = dataParser;
        this.jobData = jobData;

        try {
            saveJobFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Object orgObj = null;
        Object resObj = null;
        try {
            // get the original data
            orgObj = dataParser.parseBytesToObject(jobData.byteData);

            // initiate the Job algorithm class & execute it
            // suppose that job was download to Download folder in local device
            String jobPath = Utils.getDownloadPath() + "/" + Utils.JOB_FILE_NAME;
            resObj = Utils.runRemote(this.context, jobPath, orgObj, dataParser.getDataClass());

            // release the original data
            dataParser.destroy(orgObj);

            // send this result to server
            byte[] resObjBytes = dataParser.parseObjectToBytes(resObj);
            JobData jobResult = new JobData(this.jobData.index, resObjBytes, new byte[0]);

            // release the result data
            dataParser.destroy(orgObj);

            handler.obtainMessage(Utils.JOB_OK, jobResult).sendToTarget();

        } catch (Exception e) {
            handler.obtainMessage(Utils.JOB_FAILED, e);
        } finally {
            // release the result bitmap
            if (resObj != null && !dataParser.isObjectDestroyed(resObj)) {
                dataParser.destroy(resObj);
            }

            // release the original bitmap
            if (orgObj != null && !dataParser.isObjectDestroyed(orgObj)) {
                dataParser.destroy(orgObj);
            }
        }

    }

    /**
     * store job data to a local file
     *
     * @throws IOException
     */
    private void saveJobFile() throws IOException {
        String outputJobFilePath = Utils.getDownloadPath() + "/" + Utils.JOB_FILE_NAME;

        if (!new File(outputJobFilePath).exists()) {
            Utils.writeFile(outputJobFilePath, this.jobData.jobClass);
        }
    }
}
