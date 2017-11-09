package com.eon.egc.fenix.remitfeed.utisetter.common;

import java.util.List;

import com.eon.eet.fenix.common.DestroyableObjectStore;
import com.eon.eet.fenix.common.FenixException;
import com.eon.eet.fenix.common.FenixUtil;
import com.eon.eet.fenix.common.date.DateUtil;
import com.eon.eet.fenix.common.dbase.DbaseUtil;
import com.eon.eet.fenix.common.logging.Log;
import com.eon.eet.fenix.common.table.TableRowIterator;
import com.eon.eet.fenix.common.table.UserTableEnum;
import com.eon.egc.fenix.remitfeed.enums.RemitConstantsRepositoryEnum;
import com.eon.egc.fenix.remitfeed.enums.RemitFormatTypeEnum;
import com.eon.egc.fenix.remitfeed.enums.RemitUTIStatusEnum;
import com.eon.egc.fenix.remitfeed.util.RemitConfig;
import com.eon.egc.fenix.remitfeed.util.RemitDBUtil;
import com.eon.egc.fenix.remitfeed.util.RemitDealFormatSegregator;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;




/**
 * REMIT UTI Deal Data creation plugin with common utilities
 *
 *
 * @author Sourendra Adhya
 *         Created Feb 8, 2016
 *
 */
/*
 * L1 Code Review Passed: 19-Jul-2016
 *
 * ----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | RSS No.   | Date        | Who        | Description                                                                             |
 * ----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 |           | 08-Feb-2016 | Sourendra  | REMIT UTI : Initial version                                                             |
 * | 002 |           | 14-Mar-2016 | Amit Singh | Change in Default value for Price, Quantity and corresponding units                     |
 * | 003 |           | 28-Mar-2016 | Amit Singh | Remit UTI Progressive Number enhancement                                                |
 * | 004 |INC6963119 | 24-Jun-2016 | RZachow L3 | Reproduce changes from GO by Sourendra, to cover fixes:                                  |
 * |                                             - Fix for UCI Progressive Number                                                         |
 * | 005 |           | 01-Aug-2017 | S34722     | CloudMigration fixed compilation issue in setUtiTranInfo                                |
 * ----------------------------------------------------------------------------------------------------------------------------------------
 */
public class RemitUTIDealData {

    /** Default Double Value */
    private final static double DEFAULT_DBL_VALUE = -9999999999.99999;
    /** Default UTI Price */
    private final static String DEFAULT_UTI_PRICE = "";
    /** Default UTI Volume */
    private final static String DEFAULT_UTI_QUANTITY = "";
    /** Script Logger */
    private final Log log;
    /** All the UTI Deals in scope with F1/F2 marked */
    private Table allDealData;
    /** UTI Deal data table */
    private Table utiDealData;
    /** Run Id for the Task executed */
    private int runId = 0;
    /** Execution Type */
    private String execType = "";
    /** Business date for the current run */
    private ODateTime businessdate = null;
    /** Response object */
    private List<UtiResponseBean> response;
    /** Remit Config Object */
    private RemitConfig config = null;
    /** RemitDealFormatSegregator object */
    private RemitDealFormatSegregator dealSeggregator = null;

    /**
     * Initialises all the deals with the F1/F2 classification
     *
     * @param queryId
     * @param mode
     * @param busDate
     * @throws FenixException
     * @throws OException
     */
    public RemitUTIDealData(int queryId, String mode, ODateTime busDate) throws FenixException, OException {
        this.log = new Log();
        this.dealSeggregator = new RemitDealFormatSegregator(queryId, false);
        this.allDealData = dealSeggregator.getDealDetails();
        log.info("UTI Deals classified as F1/F2.");
        runId = generateRunID();
        this.execType = mode;
        this.businessdate = busDate;
        this.config = RemitConfig.getConfigService(RemitConstantsRepositoryEnum.CONTEXT,
            RemitConstantsRepositoryEnum.SUB_CONTEXT_UTI);

    }

    /**
     * Creates the UTI table structure
     *
     *
     * @throws OException
     * @throws FenixException
     */
    public void setUtiFieldsInUserTable() throws OException, FenixException {

        int dealCount = allDealData.getNumRows();
        if (dealCount > 0) {
            createUTITableStructure();
            retrieveData();
        }
        else {
            log.warning("No Deals available to process.");
            throw new FenixException("No Deals available to process.");
        }
    }

    /**
     * Creates the UTI utiDealData table structure for the valid fields to be reported
     *
     * @throws OException
     */
    private void createUTITableStructure() throws OException {

        utiDealData = DestroyableObjectStore.tableNew("uti_table_data");

        utiDealData.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
        utiDealData.addCol("applicationkey", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("generationmethod", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("generationmethodversion", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("sendingsystem", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("targetsystem", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("messageid", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("sourcesystemtradeid", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("remitformat", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("buyer", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("buyertype", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("seller", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("sellertype", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("contracttype", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("commodity", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("settlement", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("tradedate", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("price", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("currency", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("quantity", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("quantityunit", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("deliverypoint", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("deliverystartdate", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("deliveryenddate", COL_TYPE_ENUM.COL_STRING);
        utiDealData.addCol("progressivenumber", COL_TYPE_ENUM.COL_INT);
        utiDealData.addCol("business_date", COL_TYPE_ENUM.COL_DATE_TIME);

        log.info("UTI table structure created");
    }

    /**
     * Retrieves the F1 and F2 required fields for processing
     *
     * @throws FenixException
     * @throws OException
     */
    private void retrieveData() throws FenixException, OException {

        utiDealData.select(allDealData, "tran_num, deal_tracking_num(sourcesystemtradeid), format_type(remitformat)",
            "tran_num GT 0");

        setDefaultValues();

        int queryIdForF1 = dealSeggregator.getQueryId(RemitFormatTypeEnum.FORMAT_F1);
        if (queryIdForF1 > 0) {

            Table f1Data = retrieveCommonFields(queryIdForF1);

            Table addlnDataF1 = retrieveAdditionalF1Fields(queryIdForF1);
            f1Data.select(addlnDataF1, "dealtprice(price), quantity, quantityunit", "tran_num EQ $tran_num");

            String f1Fields = "messageid, buyer, seller, tradedate, currency, deliverystartdate, deliveryenddate, deliverypoint, "
                + "contracttype, price, quantity, quantityunit, trade_time";

            utiDealData.select(f1Data, f1Fields, "tran_num EQ $tran_num");

            Table profile = retrieveProfileRecords(queryIdForF1);
            profile.sortCol("tran_num");

            for (int row : new TableRowIterator(utiDealData)) {

                if (utiDealData.getString("remitformat", row).equalsIgnoreCase(RemitFormatTypeEnum.FORMAT_F2.toString())) {
                    continue;
                }

                int tranNum = utiDealData.getInt("tran_num", row);
                int profileRow = profile.findInt("tran_num", tranNum, SEARCH_ENUM.FIRST_IN_GROUP);

                String price = utiDealData.getString("price", row);
                int distinctPriceCount = profile.getInt("distinct_price_count", profileRow);
                if (null == price || Double.valueOf(price) < 0.0 || distinctPriceCount > 1) {

                    utiDealData.setString("price", row, DEFAULT_UTI_PRICE);
                    utiDealData.setString("currency", row, "");
                }
                else {
                    price = price.substring(0, price.length() - 1);
                    utiDealData.setString("price", row, price);
                }

                String quantity = utiDealData.getString("quantity", row);
                int distinctQuantityCount = profile.getInt("distinct_quantity_count", profileRow);
                if (null == quantity || Double.valueOf(quantity) < 0.0 || distinctQuantityCount > 1) {

                    utiDealData.setString("quantity", row, DEFAULT_UTI_QUANTITY);
                    utiDealData.setString("quantityunit", row, "");
                }
                else {
                    quantity = quantity.substring(0, quantity.length() - 1);
                    utiDealData.setString("quantity", row, quantity);
                }
            }
        }

        int queryIdForF2 = dealSeggregator.getQueryId(RemitFormatTypeEnum.FORMAT_F2);
        if (queryIdForF2 > 0) {
            Table f2Data = retrieveCommonFields(queryIdForF2);
            String f2Fields = "messageid, buyer, seller, tradedate, deliverypoint, deliverystartdate, deliveryenddate, contracttype, "
                + "trade_time";
            utiDealData.select(f2Data, f2Fields, "tran_num EQ $tran_num");

        }

        log.info("Data retrieved successfully.");
    }

    /**
     * Retrieves distinct count of Price and Quantity associated with profile of a deal
     * This is used to determine if a given deal is shaped or flat
     *
     * @param queryId
     * @return table
     */
    private Table retrieveProfileRecords(int queryId) {

        StringBuilder sql = new StringBuilder()
            .append("\n SELECT ")
            .append("\n     ab.tran_num, ")
            .append("\n     COUNT(DISTINCT tstd.price) distinct_price_count, ")
            .append("\n     COUNT(DISTINCT tstd.schedule_rate) distinct_quantity_count ")
            .append("\n FROM ")
            .append("\n     ab_tran ab ")
            .append("\n INNER JOIN ")
            .append("\n     query_result qr ON ab.tran_num = qr.query_result ")
            .append("\n INNER JOIN ")
            .append("\n     tran_schedule_details tsd ON tsd.tran_num = ab.tran_num ")
            .append("\n INNER JOIN ")
            .append("\n     tran_schedule_time_details tstd ON tstd.schedule_id = tsd.schedule_id ")
            .append("\n     AND tstd.bav_flag =1 AND tstd.schedule_rate != 0.0 ")
            .append("\n WHERE ")
            .append("\n     qr.unique_id = (").append(queryId).append(") ")
            .append("\n GROUP BY ")
            .append("\n ab.tran_num ");

        return DbaseUtil.execISql(sql);
    }

    /**
     * Sets the Default values for all Rows
     *
     * @throws OException
     */
    private void setDefaultValues() throws OException {
        utiDealData.setColValString("sendingsystem", "Endur");
        utiDealData.setColValString("targetsystem", "MDM");
        utiDealData.setColValString("generationmethod", "ACER");
        utiDealData.setColValString("generationmethodversion", "0.9");
        utiDealData.setColValString("buyertype", "Endur");
        utiDealData.setColValString("sellertype", "Endur");
        utiDealData.setColValString("commodity", "EL");
        utiDealData.setColValString("settlement", "P");
        String dbName = DbaseUtil.getDatabaseName();
        String appKey = config.getUTIAppKey(dbName);
        utiDealData.setColValString("applicationkey", appKey);
        utiDealData.setColValDateTime("business_date", businessdate);
        utiDealData.setColValInt("progressivenumber", 1);
    }

    /**
     * Retrieves the Common fields for F1 and F2 deals
     *
     * @param queryIdF1F2
     * @return table
     */
    private Table retrieveCommonFields(int queryIdF1F2) {
        Table utiDataF1F2 = getUtiDataF1F2Deals(queryIdF1F2);
        log.info("Common fields retrieved");
        return utiDataF1F2;
    }

    /**
     * Retrieves Additional Fields like price, quantity, quantity unit for F1 deals
     *
     * @param queryIdForF1
     * @return table
     * @throws OException
     */
    private Table retrieveAdditionalF1Fields(int queryIdForF1) throws OException {
        return RemitDBUtil.retrieveF1DerivedFieldsForMaster(queryIdForF1, DEFAULT_DBL_VALUE);
    }

    /**
     * Saves the UTI Data into the user table
     *
     * @throws OException
     * @throws FenixException
     */
    private void saveUtiDataInUserTable() throws OException, FenixException {
        Table utiUserTable = DestroyableObjectStore.tableNew(UserTableEnum.USER_EGC_REMIT_UTI_DATA.toString());
        DBUserTable.structure(utiUserTable);

        utiDealData.addCol("run_id", COL_TYPE_ENUM.COL_INT);
        utiDealData.setColValInt("run_id", runId);
        utiDealData.addCol("status", COL_TYPE_ENUM.COL_STRING);
        utiDealData.setColValString("status", RemitUTIStatusEnum.REQUEST_GENERATED.toString());
        utiDealData.addCol("generated_datetime", COL_TYPE_ENUM.COL_DATE_TIME);
        utiDealData.setColValDateTime("generated_datetime", DateUtil.getCurrentServerDateAndTime());

        int dbRetVal = 0;
        try {

            // Purge data based on the retention period. Three step approach.
            /* Note: Not using delete API as it is restricted, and requires special DB privilages */
            Table toBeSaved = getFeedDataToRetain();
            if (toBeSaved.getNumRows() > 0) {
                // Step 2: Clears everything, but keeps copy of the data to retain
                dbRetVal = DBUserTable.clear(utiUserTable);
                utiUserTable.select(toBeSaved, "*", "run_id GT 0");
            }
            else {
                // Step 2: Clears everything, nothing to retain
                dbRetVal = DBUserTable.clear(utiUserTable);
            }

            // Add the data to save along with existing data if any
            utiUserTable.select(utiDealData, "*", "run_id GT 0");

            //Delete irrelevant columns
            utiUserTable.delCol("trade_time");
            utiUserTable.delCol("sent_flag");
            utiUserTable.delCol("unique_contract_identifier");

            // The id helps to save the data in parts into the table
            utiUserTable.addCol("row_count", COL_TYPE_ENUM.COL_INT);
            utiUserTable.setColIncrementInt("row_count", 1, 1);

            // Split Table to save data in parts in bcpIn operation, would be necessary when data grows in size
            Table split = DestroyableObjectStore.tableNew(UserTableEnum.USER_EGC_REMIT_UTI_DATA.toString());
            DBUserTable.structure(split);
            int splitId = 0;
            do {
                split.select(utiUserTable, "*", "row_count GT " + splitId + " AND row_count LE " + (splitId + 10000));
                split.delCol("row_count");
                long start = System.currentTimeMillis();
                // Step 3: Now inserts the data back in parts
              dbRetVal = DBUserTable.bcpIn(split);
                log.info("Insert of " + split.getNumRows() + " rows done in " + (System.currentTimeMillis() - start) + " ms. Table:"
                    + UserTableEnum.USER_EGC_REMIT_UTI_DATA.toString());
                split.clearRows();
                splitId += 10000;
            } while (splitId < utiUserTable.getNumRows());
        }
        catch (OException e) {
            log.error("Failed to update " + DBUserTable.dbRetrieveErrorInfo(dbRetVal, "Failed to insert into user table "
                + UserTableEnum.USER_EGC_REMIT_UTI_DATA.toString()), e);
        }
    }

    /**
     * <b>Step 1: Copy of the data to retain. </b>
     * Picks up the data that would be retained in the user table, the rest of the data would be deleted.
     *
     * @return table to retain
     * @throws OException
     */
    private Table getFeedDataToRetain() throws OException {

        log.info("Saving User Table Started :");
        int retaintionDays = config.getUTILogRetentionDays();
        int localJDate = businessdate.getDate() - retaintionDays;
        ODateTime localDate = DestroyableObjectStore.dtNew(localJDate);

        StringBuilder sql = new StringBuilder()
            .append("\n WITH maxdate AS (")
            .append("\n     SELECT MAX(business_date) AS maxbussdate")
            .append("\n     FROM")
            .append("\n     user_egc_remit_uti_data")
            .append("\n     )")
            .append("\n SELECT uti.* FROM ")
            .append("\n     user_egc_remit_uti_data uti,")
            .append("\n     maxdate mbd")
            .append("\n WHERE uti.business_date > TO_DATE('" + localDate.formatForDbAccess() + "', 'DD-mon-YYYY HH24:MI:SS')")
            .append("\n     AND uti.business_date <=  mbd.maxbussdate");
        return DbaseUtil.execISql(sql);

    }

    /**
     * Generates run id
     *
     * @return unique id
     * @throws OException
     */
    public static int generateRunID() throws OException {
        StringBuilder sql = new StringBuilder()
            .append(" SELECT MAX(run_id) AS runid FROM ").append(UserTableEnum.USER_EGC_REMIT_UTI_DATA.toString());
        int runid = DbaseUtil.execISql(sql).getInt(1, 1);
        if (runid == 0 || runid == 99999) {
            runid = 100000;
        }
        else {
            runid++;
        }
        return runid;
    }


    /**
     * Gets the Common F1 & F2 Mandatory fields from DB
     *
     * @param queryIdForF1F2
     * @return table
     */
    private Table getUtiDataF1F2Deals(int queryIdForF1F2) {
        StringBuilder sql = new StringBuilder()
            .append("\n SELECT DISTINCT")
            .append("\n       ab.tran_num,")
            .append("\n       ab.tran_num AS messageid,")
            .append("\n       ab.deal_tracking_num,")
            .append("\n       CASE WHEN ab.buy_sell = 0 THEN ab.internal_bunit ELSE ab.external_bunit END buyer,")
            .append("\n       CASE WHEN ab.buy_sell = 0 THEN ab.external_bunit ELSE ab.internal_bunit END seller,")
            .append("\n       TO_CHAR(ab.trade_date, 'YYYY-MM-DD') AS tradedate,")
            .append("\n       c.name AS currency,")
            .append("\n       TO_CHAR(ab.start_date, 'YYYY-MM-DD') AS deliverystartdate,")
            .append("\n       TO_CHAR(ab.maturity_date, 'YYYY-MM-DD') AS deliveryenddate,")
            .append("\n       pca.control_area_info1 deliverypoint,")
            .append("\n       mit.tradetype AS contracttype, ")
            .append("\n       ab.trade_time ")
            .append("\n FROM query_result qr")
            .append("\n INNER JOIN ab_tran ab")
            .append("\n       ON ab.tran_num = qr.query_result")
            .append("\n INNER JOIN currency c ON ab.currency = c.id_number")
            .append("\n INNER JOIN user_egc_remit_map_ins_type mit")
            .append("\n       ON mit.instrument_id = ab.ins_type")
            .append("\n INNER JOIN pwr_tran_aux_data pxd ")
            .append("\n        ON pxd.tran_num = ab.tran_num ")
            .append("\n LEFT OUTER JOIN pwr_control_area pca")
            .append("\n       ON pxd.ctl_area_id = pca.ctl_area_id ")
            .append("\n WHERE qr.unique_id =").append(queryIdForF1F2);

        return DbaseUtil.execISql(sql);
    }

    /**
     * Saves All the deal data into a CSV
     *
     *
     * @throws OException
     * @throws FenixException
     */
    public void dumpDataAsCSV() throws OException, FenixException {
        String fileName = "Remit_UTIData_" + execType + runId;
        log.info("Dumping UTI Data File in : " + Util.reportGetDirForToday() + fileName);
        FenixUtil.generateCSVReport(false, false, Util.reportGetDirForToday(), fileName,
            utiDealData);

        saveUtiDataInUserTable();
    }

    /**
     * Sends request to web client
     *
     * @throws OException
     */
    public void sendRequest() throws OException {
        UtiWebserviceClient client = new UtiWebserviceClient(utiDealData);
        response = client.sendRequest();
        log.info("Send & receive requests Completed.");
        addRemovedUCIDealsToResponse();

    }

    /**
     * Adds the UCI deals which were removed and not sent to MDM, to the response list object
     *
     * Returns the modified response object
     *
     * @throws OException
     */
    private void addRemovedUCIDealsToResponse() throws OException {

        // Copies all deals which were SENT to MDM
        Table dealsSent = DestroyableObjectStore.tableClone(utiDealData);
        dealsSent.select(utiDealData, "*", "sent_flag EQ 1");

        Table dealsNotSent = DestroyableObjectStore.tableClone(utiDealData);

        // Loop for each UCI value, there will be only one deal SENT for a UCI
        for (int row : new TableRowIterator(dealsSent)) {

            String dealNumSent = dealsSent.getString("sourcesystemtradeid", row);
            String uciValue = dealsSent.getString("unique_contract_identifier", row);

            if (!(uciValue == null || uciValue.length() == 0)) {

                // Copy the deals which were not sent, belonging to same UCI
                dealsNotSent.select(utiDealData, "sourcesystemtradeid", "unique_contract_identifier EQ " + uciValue
                    + " AND sent_flag EQ 0");

                String sendingSourceSystem = "";
                String targetSourceSystem = "";
                String messageid = "";
                String uti = "";
                String errorCode = "";
                String errorDescription = "";

                // Search in the Responses List for the SENT deal and copy the values received
                for (UtiResponseBean eachResponse : response) {
                    String responseDealNum = eachResponse.getSourceSystemTradeID();

                    if (dealNumSent.equalsIgnoreCase(responseDealNum)) {
                        sendingSourceSystem = eachResponse.getSendingSourceSystem();
                        targetSourceSystem = eachResponse.getTargetSourceSystem();
                        messageid = eachResponse.getMessageID();
                        uti = eachResponse.getUti();
                        errorCode = eachResponse.getErrorCode();
                        errorDescription = eachResponse.getErrorDescription();
                        break;
                    }
                }

                // Create Response objects for deals not Sent, with the sent values
                for (int mainRow : new TableRowIterator(dealsNotSent)) {
                    UtiResponseBean utiResponseBean = new UtiResponseBean();

                    utiResponseBean.setSourceSystemTradeID(dealsNotSent.getString("sourcesystemtradeid", mainRow));
                    utiResponseBean.setSendingSourceSystem(sendingSourceSystem);
                    utiResponseBean.setTargetSourceSystem(targetSourceSystem);
                    utiResponseBean.setMessageID(messageid);
                    utiResponseBean.setUti(uti);
                    utiResponseBean.setErrorCode(errorCode);
                    utiResponseBean.setErrorDescription(errorDescription);

                    response.add(utiResponseBean);
                }
            }
        }
    }

    /**
     * Sets the tran info fields on deal
     *
     * @throws OException
     */
    public void setUtiTranInfo() throws OException, FenixException {
        RemitUTISetter setter = new RemitUTISetter(runId, response);
        setter.setDataInTable();
        setter.setResponseInUserTable();
        setter.setUtiOnDeals();

    }

    /**
     * Handles the UCI deal merging scenario.
     *
     * Steps:
     * 1. Find all deals where there is a UCI code
     * 2. Find the max deal number for the UCI
     * 3. The deal with Max deal number should be sent to MDM for generating UTI
     * 4. All the other deals of the same UCI would be removed, and not sent to MDM
     *
     * Further steps covered in addRemovedUCIDealsToResponse()
     * 5. The same UTI value retrieved should be applied to all the deals of the UCI
     * 6. The UTI tran_info value is saved for the deal set
     *
     * Sets the modified table utiDealData after removing UCI deals
     *
     * @throws OException
     */
    public void handleUCIScenario() throws OException {

        utiDealData.addCol("sent_flag", COL_TYPE_ENUM.COL_INT);
        utiDealData.setColValInt("sent_flag", 1);

        Table f2Deals = DestroyableObjectStore.tableNew();
        f2Deals.select(utiDealData, "*", "remitformat EQ " + RemitFormatTypeEnum.FORMAT_F2.toString());
        if (f2Deals.getNumRows() < 1) {
            log.info("No F2 Deals available to merge based on Unique Contract Identifier");
            return;
        }
        int queryId = DbaseUtil.createQueryIdForColumn(f2Deals, "tran_num");

        // Retrieve All the UCI for the deal set
        Table tranWithUniqueContractId = RemitDBUtil.getUniqueContractIdenntifier(queryId);
        Table uniqueContractIdentifiers = DestroyableObjectStore.tableNew("Unique Contract Identifiers");
        uniqueContractIdentifiers.select(tranWithUniqueContractId, "DISTINCT, unique_contract_identifier, ins_type", "tran_num GT 0");

        // Set the UCI value for entire deal set
        utiDealData.select(tranWithUniqueContractId, "unique_contract_identifier", "tran_num EQ $tran_num");

        // Sorts the Columns for faster search
        tranWithUniqueContractId.sortCol("unique_contract_identifier");
        uniqueContractIdentifiers.sortCol("unique_contract_identifier");

        // Single Row that would be preserved
        Table dealsInUCI = DestroyableObjectStore.tableNew("deals_for_a_uci");
        dealsInUCI.addCol("sent_flag", COL_TYPE_ENUM.COL_INT);

        for (int rowNum : new TableRowIterator(uniqueContractIdentifiers)) {

            // Copies multiple tran num for a UCI value
            String currrentUci = uniqueContractIdentifiers.getString("unique_contract_identifier", rowNum);
            dealsInUCI.select(tranWithUniqueContractId, "*", "unique_contract_identifier EQ " + currrentUci);

            // Copies the deal_num for each tran_num
            dealsInUCI.select(utiDealData, "sourcesystemtradeid, deliverystartdate, deliveryenddate, tradedate", "tran_num EQ $tran_num");

            // Sorts the table
            dealsInUCI.clearGroupBy();
            dealsInUCI.addGroupBy("sourcesystemtradeid");
            dealsInUCI.groupBy();

            // For max dealNum sets the sent_flag as 1, rest deals are 0
            int maxDealRowNum = dealsInUCI.getNumRows();
            String maxDeal = dealsInUCI.getString("sourcesystemtradeid", maxDealRowNum);
            log.info("Max Deal Number :" + maxDeal + " For the UCI : " + currrentUci);

            //Set values for maximum deal number
            dealsInUCI.setInt("sent_flag", maxDealRowNum, 1);
            setDeliveryStartEndDate(dealsInUCI, maxDealRowNum);

            // Set the sent_flag for all deals and deliverystartdate/deliveryenddate for max deal number for given UCI
            utiDealData.select(dealsInUCI, "sent_flag, deliverystartdate, deliveryenddate, tradedate", "tran_num EQ $tran_num");

            dealsInUCI.clearRows();
        }

    }

    /**
     * Sets Delivery Start and End Date for Merged Deal as Min/Max of all deals under a given UCI value
     *
     * @param dealsInUCI all deals under a given UCI
     * @param maxDealRowNum
     * @throws OException
     */

    private void setDeliveryStartEndDate(Table dealsInUCI, int maxDealRowNum) throws OException {

        if (maxDealRowNum == 1) {
            return;
        }
        int queryId = DbaseUtil.createQueryIdForColumn(dealsInUCI, "tran_num");

        StringBuilder sql = new StringBuilder()
            .append("\n SELECT ")
            .append("\n       TO_CHAR(MIN(ab.start_date), 'YYYY-MM-DD') AS mindeliverystartdate, ")
            .append("\n       TO_CHAR(MAX(ab.maturity_date), 'YYYY-MM-DD') AS maxdeliveryenddate, ")
            .append("\n       TO_CHAR(MIN(ab.trade_date), 'YYYY-MM-DD') AS tradedate ")
            .append("\n FROM ")
            .append("\n     ab_tran ab")
            .append("\n INNER JOIN ")
            .append("\n     query_result qr")
            .append("\n     ON ab.tran_num = qr.query_result")
            .append("\n     AND qr.unique_id = ").append(queryId);

        Table result = DbaseUtil.execISql(sql);
        DestroyableObjectStore.remove(queryId);

        dealsInUCI.setString("deliverystartdate", maxDealRowNum, result.getString("mindeliverystartdate", 1));
        dealsInUCI.setString("deliveryenddate", maxDealRowNum, result.getString("maxdeliveryenddate", 1));
        dealsInUCI.setString("tradedate", maxDealRowNum, result.getString("tradedate", 1));

        DestroyableObjectStore.remove(result);
    }

    /**
     * Sets Progressive Number which will be used to differentiate deals having identical parameters
     *
     * @throws OException
     * @throws FenixException
     */
    public void setProgressiveNumber() throws OException, FenixException {

        log.info("Progressive Number Computation started...");
        RemitUTIProgessiveNumGenerator progNum = new RemitUTIProgessiveNumGenerator(utiDealData);
        progNum.setProgressiveNumber();
        log.info("Progressive Number Computation completed.");
    }

}