package com.minhld.job2p.jobs;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import com.minhld.job2p.supports.PeerSpecs;
import com.minhld.job2p.supports.Utils;

import java.io.ByteArrayOutputStream;

/**
 * Created by minhld on 11/6/2015.
 */
public class JobServerHandler extends Handler {

    Activity parent;
    JobClientHandler clientHandler;
    Handler mainUiHandler;
    JobDataParser dataParser;
    Object finalObject;

    public JobServerHandler(Activity parent, Handler uiHandler, JobClientHandler clientHandler, JobDataParser dataParser) {
        this.parent = parent;
        this.mainUiHandler = uiHandler;
        this.clientHandler = clientHandler;
        this.dataParser = dataParser;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case Utils.MESSAGE_READ_CLIENT: {
                // client received job or ACK, will send the result here
                ByteArrayOutputStream readBuf = (ByteArrayOutputStream) msg.obj;

                JobData jobData = Utils.convert2JobData(readBuf.toByteArray());

                // check if the message is ACK
                if (jobData.jobType == Utils.JOB_TYPE_ACK) {
                    // get specification info
                    String specsJSON = PeerSpecs.getMyJSONSpecs(this.parent, jobData.index);

                    // makeup ACK with index and return
                    JobData ackRes = new JobData(jobData.index, Utils.JOB_TYPE_ACK,
                                            specsJSON.getBytes(), new byte[0]);
                    this.clientHandler.getBroadcaster().sendObject(ackRes.toByteArray(), jobData.index);

                    // send message out
                    this.mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[client] ACK received. answer now").sendToTarget();

                } else if (jobData.jobType == Utils.JOB_TYPE_ORG){
                    // if message is job request
                    // print out that it received a job from server
                    this.mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[client] received a job from server. executing...").sendToTarget();

                    // run the job, result will be thrown to client executor handler
                    new Thread(new JobExecutor(parent, clientHandler, dataParser, jobData)).start();
                }
                break;
            }
            case Utils.MESSAGE_READ_SERVER: {
                // server received client result, will merge the results here
                JobData clientJobResult = null;

                try {
                    if (msg.obj instanceof JobData) {
                        // this case happens when server finishes its own job
                        clientJobResult = (JobData) msg.obj;
                    } else {
                        // this case happens when server receives a result from client - in
                        // binary array
                        ByteArrayOutputStream readBuf = (ByteArrayOutputStream) msg.obj;
                        clientJobResult = (JobData) Utils.deserialize(readBuf.toByteArray());
                    }


                    if (clientJobResult.jobType == Utils.JOB_TYPE_ACK) {
                        // if the job data is ACK type, parse the ACK
                        mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[server] received ACK from client [" + clientJobResult.index + "]").sendToTarget();
                        String ackSignal = new String(clientJobResult.byteData);

                    } else if (clientJobResult.jobType == Utils.JOB_TYPE_ORG) {
                        // if the job data received is original job object

                        dataParser.copyPartToPlaceholder(finalObject, clientJobResult.byteData, clientJobResult.index);

                        // also display it partially
                        mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[server] received data from client [" + clientJobResult.index + "]").sendToTarget();
                        mainUiHandler.obtainMessage(Utils.MAIN_JOB_DONE, finalObject).sendToTarget();
                    }
                } catch (Exception e) {
                    mainUiHandler.obtainMessage(Utils.MAIN_INFO, "[server-error] " + e.getMessage()).sendToTarget();
                }
                break;
            }
            case Utils.MESSAGE_READ_JOB_SENT: {
                // when job is dispatched, a placeholder bitmap will be created
                // to accumulate the results from clients
                String jsonData = (String) msg.obj;
                finalObject = dataParser.createPlaceholder(jsonData);
                break;
            }
            case Utils.JOB_FAILED: {
                String exStr = (String) msg.obj;
                mainUiHandler.obtainMessage(Utils.MAIN_INFO, exStr).sendToTarget();
                break;
            }
            case Utils.MESSAGE_INFO: {
                // self instruction, don't care
                Object obj = msg.obj;
                mainUiHandler.obtainMessage(Utils.MAIN_INFO, msg.obj + "").sendToTarget();
                break;
            }

        }
    }

}
