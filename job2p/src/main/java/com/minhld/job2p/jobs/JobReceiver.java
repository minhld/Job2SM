package com.minhld.job2p.jobs;

import android.os.AsyncTask;
import android.os.Handler;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobReceiver extends AsyncTask {
    Handler jobDoneHandler;

    public JobReceiver(Handler jobHandler) {
        this.jobDoneHandler = jobHandler;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        return null;
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);

    }
}
