package com.eon.eet.fenix.interfaces.neon;

import com.eon.eet.fenix.common.DestroyableObjectStore;
import com.eon.eet.fenix.common.dbase.DbaseUtil;
import com.eon.eet.fenix.common.logging.Log;
import com.eon.eet.fenix.interfaces.neon.message.NeonMessageType;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

/**
 * Prepares for and triggers the process of sending messages to Neon.
 * 
 * @author G. Moore
 *         Created 31 Aug 2011
 * 
 */

/* 
 * L1 Code Review Passed: 25-Jun-2014
 * 
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | RSS No.   | Date        | Who         | Description                                                                             |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 |           | 31-Aug-2011 | G. Moore    | Initial version.                                                                        |
 * | 002 |           | 29-Sep-2011 | G. Moore    | Renamed from NeonDealMessageTrigger, now handles multi message types and batch messages.|
 * | 003 |           | 11-Oct-2011 | G. Moore    | Introduced ITransporter.                                                                |
 * | 004 |           | 15-Oct-2011 | G. Moore    | Neon request id introduced.                                                             |
 * | 005 |           | 01-Dec-2011 | G. Moore    | Neon recipient id introduced.                                                           |
 * | 006 |           | 16-Dec-2011 | G. Moore    | Support retry count for failed messages being re-tried.                                 |
 * | 007 | HPQC#928  | 28-Feb-2012 | G. Moore    | Connex is now always used for sending message to Neon whether batch or individually.    |
 * |     |           |             |             | This avoids timeout if Neon requests a large batch of deals via the deal request        |
 * |     |           |             |             | interface.                                                                              |
 * | 008 |           | 03-Aug-2012 | R. Stoyanov | Added overloaded method add() so to be able to pass also object_type into objects table.|
 * | 009 |           | 11-Dec-2012 | G. Moore    | setObjects has become addObjects because the internal objects table is destroyed.       |
 * | 010 | HPQC#3992 | 12-Jun-2013 | G. Moore    | Handle sending of raw xml messages.                                                     |
 * | 011 | INC6207177| 16-Jun-2014 | Sapient L3  | Restructured code to stop creation of multiple producer                                 |
 * | 009 | 		     | 27-Jul-2017 | S34722      | CloudMigration Removed the Active MQ release code as it has been replaced by REST       |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class NeonMessageTrigger {

    /** Table of objects to be sent to Neon */
    private Table objects = createObjectsTable();

    /** Time stamp of when this object was created */
    private ODateTime creationDate = DbaseUtil.getServerDateTime();

    /** the request id which tells Neon what the original request was for this message */
    private String requestId;

    /** the recipient id which tells the middle ware which instance of Neon the message should be sent to */
    private String recipientId;

    /** Neon configuration */
    private NeonConfiguration config = new NeonConfiguration();

    /** Neon Batch Sender */
    private NeonBatchMessageSender msgSender = null;

    /** Log instance */
    private final Log log = new Log();

    /** Constructor */
    public NeonMessageTrigger() {
        msgSender = new NeonBatchMessageSender();
    }

    /**
     * Add a raw xml message to be sent to Neon.
     * 
     * @param xml Raw xml message to be sent to Neon
     * @param retryCount How many times this message has been re-tried.
     */
    public void add(String xml, int retryCount) {
        add(0, NeonMessageType.RAW_XML, null, xml, retryCount);
    }

    /**
     * Add a message of type 'message type' and with the given id to be sent to Neon.
     * 
     * @param id - the id of the type associated with message
     * @param msgType - the message type
     */
    public void add(int id, NeonMessageType msgType) {
        add(id, msgType, 0);
    }

    /**
     * Add a message of type 'message type' and with the given id to be sent to Neon.
     * 
     * @param id - the id of the type associated with message
     * @param msgType - the message type
     * @param retryCount - how many times this message has been re-tried.
     */
    public void add(int id, NeonMessageType msgType, int retryCount) {
        this.add(id, msgType, null, retryCount);
    }

    /**
     * Add a message of type 'message type' and with the given id to be sent to Neon.
     * 
     * @param id - the id of the type associated with message
     * @param msgType - the message type
     * @param msgSubType - the message sub-type, for example for Reference Data it is Party, Portfolio ... Allows null.
     * @param retryCount - how many times this message has been re-tried.
     */
    public void add(int id, NeonMessageType msgType, String msgSubType, int retryCount) {
        add(id, msgType, msgSubType, null, retryCount);
    }

    /**
     * Add a message of type 'message type' and with the given id to be sent to Neon.
     * 
     * @param id Id of the type associated with message
     * @param msgType Neon message type
     * @param msgSubType Message sub-type, for example for Reference Data it is Party, Portfolio ... Allows null.
     * @param xml Raw xml message to be sent to Neon
     * @param retryCount How many times this message has been re-tried.
     */
    private void add(int id, NeonMessageType msgType, String msgSubType, String xml, int retryCount) {
        try {
            objects.insertRowBefore(1);
            objects.setInt("id", 1, id);
            objects.setString("type", 1, msgType.name());
            objects.setString("sub_type", 1, msgSubType);
            objects.setString("xml_message", 1, xml);
            objects.setInt("retry_count", 1, retryCount);
            objects.setDateTime("op_service_start_time", 1, creationDate);
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the request id which tells Neon what the original request was for this message
     * 
     * @param requestId - request id
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Set the recipient id which tells the middle ware which instance of Neon the message should be sent to
     * 
     * @param recipientId - recipient id
     */
    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    /**
     * Trigger the process that sends messages to Neon. If the number of messages if equal or greater than that specified by the parameter
     * 'Batch Switch Over Count' in the constant repository then the messages will be batched otherwise the messages will be sent
     * individually.
     * 
     * @throws OException
     */
    public void triggerMessage() throws OException {

        if (objects.getNumRows() > 0) {
            String msgTypeValue = objects.getString("type", 1);
            NeonMessageType msgType = NeonMessageType.valueOf(msgTypeValue);
            int batchSwitchOverCount = 0;
            int maxBatchSize = 0;
            switch (msgType) {
                case REF_DATA:
                    batchSwitchOverCount = config.getMRDBatchSwitchOverCount();
                    maxBatchSize = config.getMRDBatchMaxSize();
                    break;
                default:
                    batchSwitchOverCount = config.getDealsBatchSwitchOverCount();
                    maxBatchSize = config.getDealsBatchMaxSize();
            }

            if (objects.getNumRows() >= batchSwitchOverCount) {
                msgSender.setMaxBatchSize(maxBatchSize);
                if (msgSender != null) {
                    msgSender.sendBatchMessage(objects, requestId, recipientId);
                }
            }
            else {

                NeonConnexInvoker requestor = new NeonConnexInvoker();
                requestor.sendMessage(objects, requestId, recipientId);
                DestroyableObjectStore.remove(objects);
            }
        }
        else {
            log.info("No messages prepared for triggering.");
        }
    }

    /**
     * @param objects the objects to add
     */
    public void addObjects(Table objects) {
        try {
            this.objects.select(objects, "id, type, sub_type", "id GE -1");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a table that will contain the message details to send.
     * 
     * @return object table
     */
    public static Table createObjectsTable() {
        try {
            Table objs = DestroyableObjectStore.tableNew("Objects");
            objs.addCol("id", COL_TYPE_ENUM.COL_INT);
            objs.addCol("type", COL_TYPE_ENUM.COL_STRING);
            objs.addCol("sub_type", COL_TYPE_ENUM.COL_STRING);
            objs.addCol("xml_message", COL_TYPE_ENUM.COL_STRING);
            objs.addCol("retry_count", COL_TYPE_ENUM.COL_INT);
            objs.addCol("row_creation", COL_TYPE_ENUM.COL_DATE_TIME);
            objs.addCol("op_service_start_time", COL_TYPE_ENUM.COL_DATE_TIME);
            return objs;
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

   

}