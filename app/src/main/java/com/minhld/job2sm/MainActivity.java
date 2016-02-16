package com.minhld.job2sm;

import android.graphics.Bitmap;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.minhld.job2p.jobs.JobDataParser;
import com.minhld.job2p.jobs.JobHandler;
import com.minhld.job2p.supports.Utils;

import java.util.Collection;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.broadcastBtn)
    Button broadcastBtn;

    @Bind(R.id.sayHiBtn)
    Button sayHiBtn;

    @Bind(R.id.infoTxt)
    TextView infoText;

    @Bind(R.id.deviceList)
    ListView deviceList;

    @Bind(R.id.viewFlipper)
    ViewFlipper mViewFlipper;

    @Bind(R.id.previewImage)
    ImageView mPreviewImage;

    @Bind(R.id.closeViewBtn)
    Button closeViewBtn;

    @Bind(R.id.useClusterCheck)
    CheckBox useClusterCheck;

    long startTime = 0;

    // this bitmap will be used as the placeholder to merge the parts sent from clients
    Bitmap finalBitmap = null;

    JobHandler jobHandler;

    WifiPeerListAdapter deviceListAdapter;

    Handler mainUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Utils.MAIN_JOB_DONE: {
                    Bitmap bmp = (Bitmap) msg.obj;
                    Bitmap scaleBmp = BitmapTools.createScaleImage(bmp, 500);

                    //// release the bitmap
                    //bmp.recycle();
                    mPreviewImage.setImageBitmap(scaleBmp);

                    long totalTime = System.currentTimeMillis() - startTime;
                    UITools.writeLog(MainActivity.this, infoText, "end time: " + totalTime);

                    //mViewFlipper.showNext();
                    mViewFlipper.setDisplayedChild(1);
                    break;
                }
                case Utils.MAIN_INFO: {
                    String strMsg = (String) msg.obj;
                    UITools.writeLog(MainActivity.this, infoText, strMsg);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        infoText.setMovementMethod(new ScrollingMovementMethod());

        // bitmap parser
        JobDataParser dataParser = new BitmapJobDataParser();
        // handlers registration
        jobHandler = new JobHandler(this, mainUiHandler, dataParser);
        jobHandler.setSocketListener(new JobHandler.JobSocketListener() {
            @Override
            public void socketUpdated(final boolean isServer, final boolean isConnected) {
                // enable/disable the "Say Hi" button when its status changed
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // only server can send jobs to client, client cannot send job to server
                        // but client will send job result to server
                        if (isServer) {
                            sayHiBtn.setEnabled(isConnected);
                        }
                    }
                });

            }

            @Override
            public void peerListUpdated(Collection<WifiP2pDevice> deviceList) {
                deviceListAdapter.clear();
                deviceListAdapter.addAll(deviceList);
                deviceListAdapter.notifyDataSetChanged();
            }
        });

        deviceListAdapter = new WifiPeerListAdapter(this, R.layout.row_devices,
                                    jobHandler.getConnector());
        deviceList.setAdapter(deviceListAdapter);

        sayHiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // go back to the device list screen
                mViewFlipper.setDisplayedChild(0);

                // 1. check if cluster is used
                boolean useCluster = useClusterCheck.isChecked();

                startTime = System.currentTimeMillis();

                // 2. dispatch jobs to clients
                String downloadPath = Utils.getDownloadPath();
                String dataPath = downloadPath + "/mars.jpg";
                String jobPath = downloadPath + "/Job.jar";
                jobHandler.dispatchJob(useCluster, dataPath, jobPath);
            }
        });

        // the status button will only be available when the socket is enabled
        sayHiBtn.setEnabled(false);

        broadcastBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeLog("attempting discover...");
                jobHandler.discoverPeers();
            }
        });

        closeViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mViewFlipper.showNext();
            }
        });

        // enable button if we don't use cluster
        useClusterCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sayHiBtn.setEnabled(!isChecked);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // reset app back to activate mode
        jobHandler.actOnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // reset app to sleep mode
        jobHandler.actOnPause();
    }

    /**
     * write to log
     *
     * @param log
     */
    public void writeLog(String log) {
        UITools.writeLog(this, this.infoText, log);
    }

    /**
     * write to log with exception
     *
     * @param prefix
     * @param e
     */
    public void writeLog(String prefix, Exception e) {
        UITools.writeLog(this, this.infoText, prefix, e);
    }

}
