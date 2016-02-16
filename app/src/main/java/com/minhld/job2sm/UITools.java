package com.minhld.job2sm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by minhld on 01/28/2016.
 */
public class UITools {
    public static final SimpleDateFormat SDF = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

    /**
     * display confirmation YES/NO
     *
     * @param c
     * @param message
     * @param listener
     */
    public static void showYesNo(Context c, String message, final ConfirmListener listener){
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("confirm");
        builder.setMessage(message);

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (listener != null) {
                    listener.confirmed();
                }
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * write the log out to the main screen
     *
     * @param c
     * @param log
     * @param msg
     */
    public static void writeLog(Activity c, final TextView log, final String msg){
        c.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.append(SDF.format(new Date()) + ": " + msg + "\r\n");
            }
        });
    }

    /**
     * write the logout with prefix and exception
     *
     * @param c
     * @param log
     * @param prefix
     * @param e
     */
    public static void writeLog(Activity c, final TextView log, final String prefix, final Exception e){
        c.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.append(SDF.format(new Date()) + ": [" + prefix + "] " + e.getMessage() + "\r\n");
                e.printStackTrace();
            }
        });
    }

    public interface ConfirmListener {
        public void confirmed();
    }

}
