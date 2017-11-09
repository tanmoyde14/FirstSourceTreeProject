package com.eon.eet.fenix.interfaces.neon.task;

import com.eon.eet.fenix.common.FenixException;
import com.eon.eet.fenix.common.dbase.DbaseUtil;
import com.eon.eet.fenix.common.script.BasicScript;
import com.eon.eet.fenix.common.table.TableRowIterator;
import com.eon.eet.fenix.interfaces.neon.NeonMessageTrigger;
import com.eon.eet.fenix.interfaces.neon.message.NeonMessageType;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/**
 * 
 * Sends the deals that are currently shown in the Trading Manager browser to Neon.
 * 
 * @author Gary Moore
 *         Created 23 Nov 2011
 * 
 */

/* 
 * L1 Code Review Passed: 25-Jun-2014
 * 
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | RSS No.   | Date        | Who        | Description                                                                              |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 |           | 23-Nov-2011 | G. Moore   | Initial version                                                                          |
 * | 002 | HPQC#3097 | 27-Feb-2013 | R.Stoyanov | Added debug performance info                                                             |
 * | 003 | HPQC#3993 | 13-Jun-2013 | G. Moore   | Allows for limiting the number of deals that can be sent.                                |
 * | 004 | HPQC#3993 | 23-Jun-2013 | G. Moore   | Improved sql in method addDealLimitFromArgtToSql.                                        |
 * | 005 | INC6207177| 16-Jun-2014 | Sapient L3 | Restructured code to stop creation of multiple producer                                  |
 * | 005 | INC6207177| 18-Aug-2017 | S34722     | remove the call to NeonMessageTrigger.releaseResource()                                  |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class NeonSendBrowserDealsMain extends BasicScript {

    @Override
    public void execute(Table argt, Table returnt) throws FenixException, OException {
        int queryId = argt.getInt("query_id", 1);
        if (queryId < 1) {
            throw new FenixException("Could not get deals from browser, a query may not have been run");
        }

        StringBuilder sql = new StringBuilder()
            .append("\n SELECT query_result AS tran_num")
            .append("\n   FROM query_result")
            .append("\n  WHERE unique_id = ").append(queryId);

        addDealLimitFromArgtToSql(sql, argt);

        Table tranNums = DbaseUtil.execISql(sql);

        NeonMessageTrigger trigger = new NeonMessageTrigger();
        long startTimeStamp = System.currentTimeMillis();
        for (int row : new TableRowIterator(tranNums)) {
            int tranNum = tranNums.getInt("tran_num", row);
            debug("Running for transaction number " + tranNum);
            trigger.add(tranNum, NeonMessageType.DEAL);
        }
        long endTimeStamp = System.currentTimeMillis();
        debug("Completed adding for sending timestamp: " + ((endTimeStamp - startTimeStamp) / 1000) + " sec.");
        info("Sending deals to Neon");
        trigger.triggerMessage();
        info("Sending deals to Neon completed");
        endTimeStamp = System.currentTimeMillis();
        info("Completed sending in " + ((endTimeStamp - startTimeStamp) / 1000) + " sec.");
    }

    /**
     * If the column 'max_deals_to_send' is present in the argt table then the original sql statement is modified as follows...
     * <p>
     * SELECT * FROM ([sql] ORDER BY tran_number) WHERE rownum <= x'
     * <p>
     * where 'x' is the value retrieved from the 'max_deals_to_send' column of argt.
     * <p>
     * This modification orders the tran_numbers from the original sql into ascending order, and then from that ordered list, selects the
     * first 'x' rows. This ensures we get the smallest 'x' tran numbers returned.
     * 
     * @param sql Sql to modify
     * @param argt Arguments table
     * @throws OException
     */
    private void addDealLimitFromArgtToSql(StringBuilder sql, Table argt) throws OException {
        if (argt.getColNum("max_deals_to_send") > 0) {
            int maxDeals = argt.getInt("max_deals_to_send", 1);
            //Insert before original SQL
            sql.insert(0, "\n SELECT * FROM (");
            //Append to end of original SQL
            sql.append(" ORDER BY tran_num) WHERE rownum <= " + maxDeals);
            info("Sending of deals limited to a maximum of " + maxDeals);
        }
    }
}