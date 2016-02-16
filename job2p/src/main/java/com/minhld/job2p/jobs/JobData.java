package com.minhld.job2p.jobs;

import com.minhld.job2p.supports.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by minhld on 11/3/2015.
 */
public class JobData implements Serializable {
    public int index;
    public byte jobType;
    // this is an another representation of bitmapData in binary data
    public byte[] byteData;
    public byte[] jobClass;

    public JobData() {
        this.index = 0;
        this.jobType = Utils.JOB_TYPE_ORG;
        this.byteData = new byte[0];
        this.jobClass = new byte[0];
    }

    /**
     * this constructor is used
     *
     * @param index
     * @param byteData
     * @param jobClassBytes
     */
    public JobData(int index, byte[] byteData, byte[] jobClassBytes) {
        this.index = index;
        this.jobType = Utils.JOB_TYPE_ORG;
        this.byteData = byteData;
        this.jobClass = jobClassBytes;
    }

    public JobData(int index, byte jobType, byte[] byteData, byte[] jobClassBytes) {
        this(index, byteData, jobClassBytes);
        this.jobType = jobType;
    }

    public JobData(int index, byte[] byteData, File jobClassFile) {
        this.index = index;
        this.jobType = Utils.JOB_TYPE_ORG;

        // assign the binary data
        this.byteData = byteData;

        // assign the job details data
        try {
            jobClass = Utils.readFile(jobClassFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JobData(int index, byte jobType, byte[] byteData, File jobClassFile) {
        this(index, byteData, jobClassFile);
        this.jobType = jobType;
    }

    /**
     * return a serialized byte array of the current job object
     *
     * @return
     */
    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] thisBytesData = Utils.serialize(this);
            byte[] lengthBytes = Utils.intToBytes(thisBytesData.length);
            bos.write(lengthBytes, 0, lengthBytes.length);
            bos.write(thisBytesData, 0, thisBytesData.length);
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

}
