package com.minhld.job2sm;

/**
 * Created by minhld on 01/28/2016
 */

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.minhld.job2p.supports.WifiBroadcaster;

import java.util.List;

// import com.minhld.job2p.supports.WifiBroadcaster;

/**
 * Array adapter for ListFragment that maintains WifiP2pDevice list.
 */
public class WifiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {
    private Context context;
    private List<WifiP2pDevice> items;
    private WifiBroadcaster mWifiBroadcaster;

    /**
     * @param context
     * @param textViewResourceId
     */
    public WifiPeerListAdapter(Context context, int textViewResourceId
                               /* ,List<WifiP2pDevice> objects*/
                               ,WifiBroadcaster mWifiBroadcaster) {
        super(context, textViewResourceId/*, objects*/);

        this.context = context;
        //this.items = objects;
        this.mWifiBroadcaster = mWifiBroadcaster;
    }

//    @Override
//    public WifiP2pDevice getItem(int position) {
//        return super.getItem(position);
//    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.row_devices, null);
        }
        WifiP2pDevice device = this.getItem(position);
        if (device != null) {
            TextView top = (TextView) v.findViewById(R.id.device_name);
            TextView bottom = (TextView) v.findViewById(R.id.device_details);
            if (top != null) {
                top.setText(device.deviceName);
            }
            if (bottom != null) {
                bottom.setText(getDeviceStatus(device.status));
            }
        }
        v.setOnClickListener(new DeviceClickListener(device));
        return v;
    }

    /**
     * this class hold one Device object to establish connection
     * to the device described by this object
     */
    private class DeviceClickListener implements View.OnClickListener {
        WifiP2pDevice device;

        public DeviceClickListener(WifiP2pDevice device) {
            this.device = device;
        }

        @Override
        public void onClick(View v) {
            switch (device.status){
                case WifiP2pDevice.INVITED: {
                    //mWifiBroadcaster.cancelConnection(device.deviceName, null);
                    break;
                }
                case WifiP2pDevice.CONNECTED: {
                    // just disconnect, no confirmation
                    mWifiBroadcaster.disconnect(device.deviceName, null);
                    break;
                }
                case WifiP2pDevice.AVAILABLE: {
                    mWifiBroadcaster.connectToADevice(device, null);
                    break;
                }
                case WifiP2pDevice.UNAVAILABLE: {
                    // nothing todo here
                    break;
                }
                case WifiP2pDevice.FAILED: {
                    // attempting to connect
                    mWifiBroadcaster.connectToADevice(device, null);
                    break;
                }
            }
        }
    }

    /**
     * return device status in String rather than number
     * @param deviceStatus
     * @return
     */
    private static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }
}