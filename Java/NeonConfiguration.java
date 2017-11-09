package com.eon.eet.fenix.interfaces.neon;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.eon.eet.fenix.common.logging.Log;
import com.eon.eet.fenix.common.ref.UserUtils;
import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;

/**
 * Provides access to the Neon interface configuration parameters via the Constant Repository. The parameters have a context of 'Neon' and
 * a sub-context of 'Interface'.
 * 
 * @author Gary Moore
 *         Created 22 Sep 2011
 * 
 */
/* 
 * L1 Code Review Passed: 24-06-2013
 * 
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | RSS No.   | Date        | Who        | Description                                                                              |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 |           | 22-Sep-2011 | G.Moore    | Initial version                                                                          |
 * | 002 |           | 18-Oct-2011 | G.Moore    | Added method getConnexCluster.                                                           |
 * | 003 |           | 25-Oct-2011 | G.Moore    | Added method isRefDataMappingEnabled.                                                    |
 * | 004 |           | 07-Nov-2011 | G.Moore    | Removed getConnexCluster and added getBatchMaxSize.                                      |
 * | 005 |           | 02-Feb-2012 | G.Moore    | Added method getMappingCacheExpireMinutes.                                               |
 * | 006 | HPQC#1066 | 02-Feb-2012 | G.Moore    | Added method getMaxJobsToProcess.                                                        |
 * | 007 |           | 16-Nov-2012 | R.Stoyanov | Added DealsRequestsLockId, MRDRequestsLockId, DealsRequestsTaskName, MRDRequestsTaskName |
 * | 008 |           | 16-Nov-2012 | R.Stoyanov | Split max batch size and switch over count for Deals and MRD                             |
 * | 009 | CPR2      | 21-Nov-2012 | G. Moore   | Remove entries related to mapping.                                                       |
 * | 010 | HPQC#3148 | 06-Mar-2013 | R.Stoyanov | Added getNeonSendDirectDeals                                                             |
 * | 011 | HPQC#3794 | 02-May-2013 | R.Stoyanov | Added NeonRunServiceExpirationHours                                                      |
 * | 012 | HPQC#3993 | 13-Jun-2013 | G. Moore   | Added getMaxBrowserDealsToSend().                                                        |
 * | 013 | HPQC#3993 | 24-Jun-2013 | G. Moore   | Added isSendBrowserDealsTaskAllowed() methods.                                           |
 * | 014 |     		 | 24-Jul-2017 | S34722     | CloudMigration Added get functions for Rest URL,URI and Port                             |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public final class NeonConfiguration {

    /** Instance of the constant repository */
    private ConstRepository constRepo;
  
    /**
     * Private constructor
     */
    public NeonConfiguration() {
        try {
            constRepo = new ConstRepository("Neon", "Interface");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }
    
  
    /**
     * Constant repository variable name: 'Connex Method Call Retries'
     * 
     * @return the maximum number of times the call to initiate the connex method can be re-tried (default 3).
     */
    public int getConnexMethodCallRetries() {
        try {
            return constRepo.getIntValue("Connex Method Call Retries", 3);
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'Deals Batch Switch Over Count'
     * 
     * @return when the number of messages to be sent in one hit is less than this value then send messages individually otherwise send
     *         messages as a batch (default 2).
     */
    public int getDealsBatchSwitchOverCount() {
        try {
            return constRepo.getIntValue("Deals Batch Switch Over Count", 2);
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'MRD Batch Switch Over Count'
     * 
     * @return when the number of messages to be sent in one hit is less than this value then send messages individually otherwise send
     *         messages as a batch (default 2).
     */
    public int getMRDBatchSwitchOverCount() {
        try {
            return constRepo.getIntValue("MRD Batch Switch Over Count", 2);
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'Deals Batch Max Size'
     * 
     * @return the maximum number of 'single' messages to be added to a batch message, if the number exceeds this then the batch will be
     *         split into multiple batch messages
     */
    public int getDealsBatchMaxSize() {
        try {
            return constRepo.getIntValue("Deals Batch Max Size", 0);
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'MRD Batch Max Size'
     * 
     * @return the maximum number of 'single' messages to be added to a batch message, if the number exceeds this then the batch will be
     *         split into multiple batch messages
     */
    public int getMRDBatchMaxSize() {
        try {
            return constRepo.getIntValue("MRD Batch Max Size", 0);
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'Ops Monitor Max Jobs To Process'
     * 
     * @return the maximum number of jobs the ops monitor is allowed to re-run per run
     */
    public int getMaxJobsToProcess() {
        try {
            return constRepo.getIntValue("Ops Monitor Max Jobs To Process", 20);
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'DealsRequestsLockIds'
     * 
     * @return the lock id used by utSemaphor to lock the deals requests table
     */
    public int getDealsRequestsLockId() {
        try {
            return constRepo.getIntValue("DealsRequestsLockId");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'MRDRequestsLockId'
     * 
     * @return the lock id used by utSemaphor to lock the MRD requests table
     */
    public int getMRDRequestsLockId() {
        try {
            return constRepo.getIntValue("MRDRequestsLockId");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'DealsRequestsTaskName'
     * 
     * @return the name of the task executed for processing Deals requests table
     */
    public String getDealsRequestsTaskName() {
        try {
            return constRepo.getStringValue("DealsRequestsTaskName");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'MRDRequestsTaskName'
     * 
     * @return the name of the task executed for processing MRD requests table
     */
    public String getMRDRequestsTaskName() {
        try {
            return constRepo.getStringValue("MRDRequestsTaskName");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'NeonSendDirectDeals'
     * 
     * @return true if NeonSendDirectDeals=enabled, otherwise false. By default false
     */
    public boolean getNeonSendDirectDeals() {
        try {
            return "enabled".equals(constRepo.getStringValue("NeonSendDirectDeals", "disabled"));
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'NeonRunServiceExpirationHours'
     * 
     * Defines how many hours in the past the entries are valid for waiting for second Neon ACK.
     * If the entries are older then this interval of hours then the entries are set to complete.
     * 
     * @return expiration hours
     */
    public int getNeonRunServiceExpirationHours() {
        try {
            return constRepo.getIntValue("NeonRunServiceExpirationHours");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'MaxBrowserDealsToSend'
     * 
     * Defines the maximum number of deals that can be re-sent via the re-sending task that is run by users. This prevents users re-sending
     * a ridiculous amount of deals to Neon in one hit.
     * 
     * @return maximum deal count
     */
    public int getMaxBrowserDealsToSend() {
        try {
            return constRepo.getIntValue("MaxBrowserDealsToSend");
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constant repository variable name: 'SendBrowserDealsLimitedTaskFunctionalGroups'.
     * <p>
     * Determines if the user is a member of the functional groups that can run the limited re-sending deals task.
     * 
     * @return true if allowed to run task
     */
    public boolean isSendBrowserDealsLimitedTaskAllowed() {
        try {
            List<String> allowedGroups = Arrays.asList(constRepo.getStringValue("SendBrowserDealsLimitedTaskFunctionalGroups").split(","));
            if (UserUtils.isMemberOfFunctionalGroups(allowedGroups)) {
                return true;
            }
            new Log().error("User is not a member of any of the functional groups " + allowedGroups + " as defined by the "
                + " SendBrowserDealsLimitedTaskFunctionalGroups constants repository entry.");
            return false;
        }
        catch (OException e) {
            throw new RuntimeException(e);
        }
    }

    
    //For cloud
    /**
     * Constant repository variable name: 'REST URI'.
     * <p>
     * Determines the REST URI value.
     * 
     * @return String value
     */
    public String getURI(){
        try {
            return constRepo.getStringValue("REST URI");
        }
        catch (OException e) {
            throw new RuntimeException(e);
            
            
        }
        
    }
    
    /**
     * Constant repository variable name: 'REST URL'.
     * <p>
     * Determines the REST Url value.
     * 
     * @return String value
     */
    public String getRESTUrl(){
        try {
            return constRepo.getStringValue("REST URL");
        }
        catch (OException e) {
            throw new RuntimeException(e);
            
            
        }
        
    }
    
    /**
     * Constant repository variable name: 'REST Port'.
     * <p>
     * Determines the REST port value.
     * 
     * @return int value
     */
    public int getPort(){
        try {
            return constRepo.getIntValue("REST Port");
        }
        catch (OException e) {
            throw new RuntimeException(e);
            
            
        }
        
    }
    
    
    /**
     * Constant repository variable name: 'Timeout'.
     * <p>
     * Determines the Timeout value.
     * 
     * @return int value
     */
    public int getTimeout(){
        try {
            return constRepo.getIntValue("Timeout");
        }
        catch (OException e) {
            throw new RuntimeException(e);
            
            
        }
        
    }
    

    
}