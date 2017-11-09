package com.eon.eet.fenix.interfaces.neon.connex;

import com.eon.eet.fenix.common.FenixException;
import com.eon.eet.fenix.common.script.BasicScript;
import com.eon.eet.fenix.interfaces.neon.NeonBatchMessageSender;
import com.eon.eet.fenix.interfaces.neon.NeonConfiguration;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/**
 * Connex method script that sends batch messages to Neon via ActiveMQ.
 * 
 * @author Gary Moore
 *         Created 28 Feb 2012
 * 
 */
/* 
 * L1 Code Review Passed: 25-Jun-2014
 * 
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | RSS No.   | Date        | Who         | Description                                                                             |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | HPQC#928  | 28-Feb-2012 | G. Moore    | Initial version                                                                         |
 * | 002 |           | 07-Mar-2012 | G. Moore    | Re-factoring around log messages.                                                       |
 * | 003 | HPQC#1063 | 14-Mar-2012 | G. Moore    | If batch send fails log message, clear batch and continue with next batch.              |
 * | 004 | HPQC#2174 | 26-Sep-2012 | R. Stoyanov | Added handling of null value when no data is found for a specific MRD entity ID         |
 * | 005 |           | 02-Nov-2012 | R. Stoyanov | Moved sendMessage to NeonMessageSender class                                            |
 * | 006 |           | 09-Nov-2012 | R. Stoyanov | Renamed class from NeonMessageSender to NeonBatchMessageSender                          |
 * | 007 |           | 21-Nov-2012 | G. Moore    | Remove all Xenon mappings as Neon will now use Endur id's.                              |
 * | 008 | INC6207177| 16-Jun-2014 | Sapient L3  | Restructured code to stop creation of multiple producer                                 |
 * | 009 | Cloud     | 27-Jul-2017 | S34722      | Removed the Active MQ release code as it has been replaced by REST                      |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class OcNeonBatchDealSend extends BasicScript {

    @Override
    public void execute(Table argt, Table returnt) throws FenixException, OException {
        // Extract request table
        Table request = argt.getTable("request_table", 1).getTable("body", 1);
        Table objects = request.getTable("objects", 1);
        String requestId = request.getString("request_id", 1);
        String recipientId = request.getString("recipient_id", 1);
        NeonBatchMessageSender msgSender = new NeonBatchMessageSender();
        msgSender.setMaxBatchSize(new NeonConfiguration().getDealsBatchMaxSize());
        msgSender.sendBatchMessage(objects, requestId, recipientId);
    }

}