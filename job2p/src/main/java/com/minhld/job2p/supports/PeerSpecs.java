package com.minhld.job2p.supports;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.json.JSONObject;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by minhld on 12/7/2015.
 */
public class PeerSpecs {
    public float cpuSpeed;
    public int cpuCoreNum;
    public float cpuUsage;

    public int memTotal;
    public float memUsage;

    public float batTotal;
    public float batUsage;

    public String availability;
    public float RL;

    public static String getMyJSONSpecs(Context c, int index) {
        PeerSpecs ps = getMySpecs(c);
        JSONObject jsonSpecs = new JSONObject();
        try {
            // general information
            jsonSpecs.put("device", index);
            jsonSpecs.put("RL", ps.RL);
            jsonSpecs.put("availability", ps.availability);
            jsonSpecs.put("network", "on");
            jsonSpecs.put("gps", "off");

            // specific data
            JSONObject jsonCPU = new JSONObject();
            jsonCPU.put("usage", ps.cpuUsage);
            jsonCPU.put("speed", ps.cpuSpeed);
            jsonCPU.put("cores", ps.cpuCoreNum);
            jsonSpecs.put("cpu", jsonCPU);

            JSONObject jsonMem = new JSONObject();
            jsonMem.put("usage", ps.memUsage);
            jsonMem.put("total", ps.memTotal);
            jsonSpecs.put("memory", jsonMem);

            JSONObject jsonBattery = new JSONObject();
            jsonBattery.put("usage", ps.batUsage);
            jsonBattery.put("total", ps.batTotal);
            jsonSpecs.put("battery", jsonBattery);

            return jsonSpecs.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * get current specs of the device
     * @return
     */
    public static PeerSpecs getMySpecs(Context c) {
        PeerSpecs ps = new PeerSpecs();

        // cpu
        ps.cpuSpeed = getCpuTotal();
        ps.cpuCoreNum = 1;
        ps.cpuUsage = readUsage();

        // memory
        float[] mems = readMem2(c);
        ps.memTotal = (int) mems[0];
        ps.memUsage = mems[1];

        // battery
        ps.batTotal = getBatteryCapacity(c);
        ps.batUsage = getBatteryUsage(c);

        // get available threshold
        String availThresStr = Utils.getConfig("availability-threshold");
        float availThres = Float.parseFloat(availThresStr);

        ps.availability = ps.batUsage > availThres ? "on" : "off";
        ps.RL = (ps.cpuSpeed * ps.cpuCoreNum / ps.cpuUsage) +
                (ps.memTotal / ps.memUsage) +
                (ps.batTotal / (ps.batUsage * 1000));

        return ps;
    }

    public static float getBatteryCapacity(Context c) {
        Object mPowerProfile_ = null;

        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";

        try {
            mPowerProfile_ = Class.forName(POWER_PROFILE_CLASS).
                            getConstructor(Context.class).newInstance(c);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            double batteryCapacity = (Double) Class
                    .forName(POWER_PROFILE_CLASS)
                    .getMethod("getAveragePower", String.class)
                    .invoke(mPowerProfile_, "battery.capacity");
            return (float) batteryCapacity;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static float getBatteryUsage(Context c) {
        Intent batteryIntent = c.registerReceiver(null,
                            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }

    public static float[] readMem2(Context c) {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memInfo);

        long availableMegs = memInfo.availMem / 1048576L;
        float memTotal = (float) memInfo.totalMem / 1048576f;
        float memUsage = (memTotal - availableMegs) / memTotal;
        return new float[] { memTotal, memUsage };
    }

    public static float[] readMem() {
        RandomAccessFile reader;
        String line = "";
        float memTotal = 0, memFree = 0, memUsage = 0;
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            while ((line = reader.readLine()) != null) {
                line = line.toLowerCase();
                if (line.contains("memtotal:")) {
                    line = line.replaceAll("memtotal:", "").
                            replaceAll("kb","").trim();
                    memTotal = Float.parseFloat(line) / 1048576L;
                }
                if (line.contains("memfree:")) {
                    line = line.replaceAll("memfree:", "").
                            replaceAll("kb", "").trim();
                    memFree = Float.parseFloat(line) / 1048576L;
                    memUsage = (memTotal - memFree) / memTotal;
                }
            }
            reader.close();
            return new float[] { memTotal, memUsage };
        } catch (IOException e) {
            return new float[2];
        }

    }

    public static float getCpuTotal() {
        RandomAccessFile reader;
        String line = "";
        StringBuffer buffer = new StringBuffer();
        float cpuTotal = 0;
        try {
            reader = new RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r");
            while ((line = reader.readLine()) != null) {
                buffer.append(line.toLowerCase());
            }
            reader.close();
            return Float.parseFloat(buffer.toString()) / 1048576L;
        } catch (IOException e) {
            return 0;
        }
    }

    private static float readUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();

            String[] toks = load.split(" ");

            long idle1 = Long.parseLong(toks[5]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            try {
                Thread.sleep(360);
            } catch (Exception e) {}

            reader.seek(0);
            load = reader.readLine();
            reader.close();

            toks = load.split(" ");

            long idle2 = Long.parseLong(toks[5]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return 0;
    }

}
