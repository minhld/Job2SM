package com.minhld.job2p.jobs;

/**
 * this interface holds an abstraction of data parser. developer needs to override
 * this method to have the suitable parser for their data
 *
 * Created by minhld on 11/23/2015.
 */
public interface JobDataParser {
    /**
     * get class definition of the data object which you use to exchange
     * to other devices
     *
     * @return
     */
    public Class getDataClass();

    /**
     * read data object from local file
     *
     * @param path
     * @return
     * @throws Exception
     */
    public Object readFile(String path) throws Exception;

    /**
     * convert data object to byte array - serialize object to bytes
     *
     * @param objData
     * @return
     * @throws Exception
     */
    public byte[] parseObjectToBytes(Object objData) throws Exception;

    /**
     * convert from byte array to data object - deserialize byte array to object<br>
     * some object type cannot be deserialized, for example Bitmap
     *
     * @param byteData
     * @return
     * @throws Exception
     */
    public Object parseBytesToObject(byte[] byteData) throws Exception;

    /**
     * this function will get one small piece of a data object. the position
     * of the split depends on its index.<br>
     *
     * example: one big bitmap can be splitted into 3 parts, the part with
     * index 2 will be the third one (the last)
     *
     *
     * @param objData
     * @param numOfParts
     * @param index
     * @return
     */
    public Object getSinglePart(Object objData, int numOfParts, int index);
    public String getJsonMetadata(Object objData);

    /**
     * create the placeholder to cumulate the results from other devices
     * sending back to requester
     *
     * @param jsonMetadata
     * @return
     */
    public Object createPlaceholder(String jsonMetadata);

    /**
     * merge the part data object into the final object.
     *
     * @param placeholderObj
     * @param partObj
     * @param index
     * @return
     */
    public Object copyPartToPlaceholder(Object placeholderObj, byte[] partObj, int index);

    /**
     * destroy the data object from the memory. If the object is input/output stream
     * object, then it will be closed. If it is a bitmap, it will be recycled etc...
     *
     * @param data
     */
    public void destroy(Object data);

    /**
     * check if the data object is really destroyed or not
     *
     * @param data
     * @return
     */
    public boolean isObjectDestroyed(Object data);
}
