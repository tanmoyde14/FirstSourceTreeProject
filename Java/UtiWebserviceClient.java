package com.eon.egc.fenix.remitfeed.utisetter.common;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.apache.http.HttpEntity;
// Apache
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import com.eon.eet.fenix.common.FenixRuntimeException;
import com.eon.eet.fenix.common.logging.Log;
import com.eon.eet.fenix.common.table.TableRowIterator;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.XString;
import com.openlink.util.constrepository.ConstRepository;

/**
 * REMIT UTI Web service client plugin : Creates Connection, Sends Request AS SOAPXML, Receives Responses synchronously.
 * 
 * @author Sourendra Adhya
 *         Created Feb 16, 2016
 * 
 */
/* 
 * L1 Code Review Passed: 26-Apr-2016
 * 
 * ----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | RSS No.   | Date        | Who        | Description                                                                             |
 * ----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 |           | 16-Feb-2016 | Sourendra  | REMIT UTI : Initial version                                                             |
 * | 002 |           | 28-Mar-2016 | Amit Singh | Remit UTI Progressive Number enhancement                                                |
 * | 003 |           | 26-Apr-2016 | Amit Singh | Change in UTI Error handling                                                            |
 * | 004 |           | 01-Aug-2017 | S34722     | CloudMigration send data via REST to middleware                                         |
 * ----------------------------------------------------------------------------------------------------------------------------------------
 */
public class UtiWebserviceClient {

    /** Script Logger */
    private final Log log;
    /** Error message */
    private final String errorMsg = "ERROR";
    /** Error message */
    private String errorFullMessage = "";
    /** Main Deal data Table */
    private Table utiDealData;
    /** Container for all Request Messages */
    private List<Map<String, String>> listOfAllDeals = new ArrayList<Map<String, String>>();
    /** Container for all Response Messages */
    private List<UtiResponseBean> utiResponseBeanList = new ArrayList<UtiResponseBean>();
    /** If something goes wrong with the Fuse connection how many times should a retry happen */
    private int maxRetries = 0;
    /** values from cons repository */
    private String url;
    private String uri;
    private int timeout;
    /** Constant Repository value for certificate key */
    private final String CONST_REPO_NAME_CERTIFICATE_KEY = "certificate password";
    /** Constant Repository value for certificate path */
    private final String CONST_REPO_NAME_SSL_CERTIFCATE_PATH = "ssl certifcate";
    /** cons repo context */
    private final String CONST_REPO_SUB_CONTEXT_OUTBOUND = "outbound";
    /** cons repo sub context */
    private final String CONST_REPO_REST_CONTEXT = "REST";
    private final String CONST_REPO_MDM_UTI = "MDM UTI";
    private final String CONST_MAX_RETRIES = "Max Retries";
    private final String CONST_TIME_OUT = "Timeout";
    private final String CONST_REST_URL = "REST URL";
    private final String CONST_REST_URI = "REST URI";

    /**
     * Constructor
     * 
     * @param utiData
     */
    public UtiWebserviceClient(Table utiData) {
        this.log = new Log();
        this.utiDealData = utiData;
    }

    /**
     * Sends the request to MDM
     * 
     * @return void
     * 
     * @throws OException
     */
    public List<UtiResponseBean> sendRequest() throws OException {

        createHashMapForAllDeals();

        generateUti();

        return utiResponseBeanList;

    }

    /**
     * Generates the UTI value from MDM
     * 
     * @param mdmServiceSOAP
     * @throws OException
     */
    public void generateUti() throws OException {

        // Loop through all the deals and send the request
        for (Map<String, String> eachDealInHashMap : listOfAllDeals) {

            StringBuffer myRequest = new StringBuffer();
            // append xml header
            myRequest.append("<GenerateUTI xmlns=\"http://www.eon-uk.com/masterdatahub\"><request>");
            String sourcesystemtradeid = "";

            Set<String> keySet = eachDealInHashMap.keySet();
            for (Iterator<String> iterator = keySet.iterator(); iterator.hasNext();) {

                String colName = (String) iterator.next();
                String value = (String) eachDealInHashMap.get(colName);

                if ("sourcesystemtradeid".equalsIgnoreCase(colName)) {
                    myRequest.append("<sourcesystemtradeid>").append(value).append("</sourcesystemtradeid>");// Samik - need to structure the tags and value in string format.
                    sourcesystemtradeid = value;
                }
                else if ("sendingsystem".equalsIgnoreCase(colName)) {
                    myRequest.append("<sendingsystem>").append(value).append("</sendingsystem>");
                }
                else if ("targetsystem".equalsIgnoreCase(colName)) {
                    myRequest.append("<targetsystem>").append(value).append("</targetsystem>");
                }
                else if ("messageid".equalsIgnoreCase(colName)) {
                    myRequest.append("<messageid>").append(value).append("</messageid>");
                }
                else if ("generationmethod".equalsIgnoreCase(colName)) {
                    myRequest.append("<generationmethod>").append(value).append("</generationmethod>");
                }
                else if ("generationmethodversion".equalsIgnoreCase(colName)) {
                    myRequest.append("<generationmethodversion>").append(value).append("</generationmethodversion>");
                }
                else if ("remitformat".equalsIgnoreCase(colName)) {
                    myRequest.append("<remitformat>").append(value).append("</remitformat>");
                }
                else if ("buyer".equalsIgnoreCase(colName)) {
                    myRequest.append("<buyer>").append(value).append("</buyer>");
                }
                else if ("buyertype".equalsIgnoreCase(colName)) {
                    myRequest.append("<buyertype>").append(value).append("</buyertype>");
                }
                else if ("seller".equalsIgnoreCase(colName)) {
                    myRequest.append("<seller>").append(value).append("</seller>");
                }
                else if ("sellertype".equalsIgnoreCase(colName)) {
                    myRequest.append("<sellertype>").append(value).append("</sellertype>");
                }
                else if ("contracttype".equalsIgnoreCase(colName)) {
                    myRequest.append("<contracttype>").append(value).append("</contracttype>");
                }
                else if ("indexname".equalsIgnoreCase(colName)) {
                    myRequest.append("<indexname>").append(value).append("</indexname>");
                }
                else if ("commodity".equalsIgnoreCase(colName)) {
                    myRequest.append("<commodity>").append(value).append("</commodity>");
                }
                else if ("settlement".equalsIgnoreCase(colName)) {
                    myRequest.append("<settlement>").append(value).append("</settlement>");
                }
                else if ("tradedate".equalsIgnoreCase(colName)) {
                    myRequest.append("<tradedate>").append(value).append("</tradedate>");
                }
                else if ("price".equalsIgnoreCase(colName)) {
                    myRequest.append("<price>").append(value).append("</price>");
                }
                else if ("currency".equalsIgnoreCase(colName)) {
                    myRequest.append("<currency>").append(value).append("</currency>");
                }
                else if ("quantity".equalsIgnoreCase(colName)) {
                    myRequest.append("<quantity>").append(value).append("</quantity>");
                }
                else if ("quantityunit".equalsIgnoreCase(colName)) {
                    myRequest.append("<quantityunit>").append(value).append("</quantityunit>");
                }
                else if ("progressivenumber".equalsIgnoreCase(colName)) {
                    myRequest.append("<progressivenumber>").append(value).append("</progressivenumber>");
                }
                else if ("deliverypoint".equalsIgnoreCase(colName)) {
                    myRequest.append("<deliverypoint>").append(value).append("</deliverypoint>");
                }
                else if ("deliverystartdate".equalsIgnoreCase(colName)) {
                    myRequest.append("<deliverystartdate>").append(value).append("</deliverystartdate>");
                }
                else if ("deliveryenddate".equalsIgnoreCase(colName)) {
                    myRequest.append("<deliveryenddate>").append(value).append("</deliveryenddate>");
                }
                else if ("applicationkey".equalsIgnoreCase(colName)) {
                    myRequest.append("<applicationkey>").append(value).append("</applicationkey>");
                }

            }
            myRequest.append("</request></GenerateUTI>");

            log.debug("Sending Request For :" + sourcesystemtradeid);
            sendRequestToMDM(sourcesystemtradeid, myRequest.toString());
        }

    }

    /**
     * Send the request to MDM and receive responses
     * 
     * @param mdmServiceSOAP
     * @param utiRequest
     * @param sourcesystemtradeid
     * @throws OException
     */
    private void sendRequestToMDM(String sourcesystemtradeid, String myXmlRequest) throws OException {

        UtiResponseBean utiResponseBean = new UtiResponseBean();
        Table utiResponse = Table.tableNew();
        try {

            //Http
            log.debug("Preparing the connection: ");
            CloseableHttpClient httpclient = null;
            CloseableHttpResponse response = null;
            
            try {
                KeyStore keyStore;
                ConstRepository constRepoRest = new ConstRepository(CONST_REPO_REST_CONTEXT, CONST_REPO_SUB_CONTEXT_OUTBOUND);
                ConstRepository constRepo = new ConstRepository(CONST_REPO_MDM_UTI, CONST_REPO_REST_CONTEXT);
                maxRetries = constRepo.getIntValue(CONST_MAX_RETRIES);
                timeout = constRepo.getIntValue(CONST_TIME_OUT);
                // initialising http client and fetching Repository values.
                url = constRepo.getStringValue(CONST_REST_URL);
                uri = constRepo.getStringValue(CONST_REST_URI);

                // Fetching the SSL credentials
                String certificatePath = constRepoRest.getStringValue(CONST_REPO_NAME_SSL_CERTIFCATE_PATH);
                String certificateKey = constRepoRest.getStringValue(CONST_REPO_NAME_CERTIFICATE_KEY);

                keyStore = KeyStore.getInstance("JKS");
                final InputStream inputStream = new FileInputStream(certificatePath);
                keyStore.load(inputStream, (certificateKey).toCharArray());

                SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
                    .build();
                //                inputStream.close();// Closing the file object
                HttpPost httpPost = new HttpPost(url + uri); //"http://sm05720:8082/neon/trades"

                RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build();

                httpclient = HttpClients.custom()
                    .setRetryHandler(retryHandler())
                    .setDefaultRequestConfig(requestConfig)
                    .setSSLContext(sslContext)
                    .build();

                httpPost.setEntity(new StringEntity(myXmlRequest));
                httpPost.addHeader("content-type", "application/xml");
                log.debug("sending XML message:\n" + myXmlRequest);
                response = httpclient.execute(httpPost);

                if (response.getStatusLine().getStatusCode() != 200 
                    || !response.getStatusLine().getReasonPhrase().equalsIgnoreCase("ok")) {
                    log.error("failed to send transaction for the following reason: " + response.getStatusLine().getReasonPhrase());
                }
                else {
                    log.debug(response.getStatusLine().getReasonPhrase());
                    log.info("message successfully sent!");
                }

                //Read the response and send it in the process response message
                HttpEntity resEntity = response.getEntity();
                String strResp = EntityUtils.toString(resEntity);
                utiResponse = processResponse(strResp);
            }
            catch (Exception e) {
                log.error("Failed to send message: " + e.getMessage());
                e.printStackTrace();
            }

            String responseSourceSystemTradeId = utiResponse.getString("sourcesystemtradeid", 1);//utiResponse.getSourceSystemTradeID();
            log.debug("Response Received For : " + responseSourceSystemTradeId);
            if (!responseSourceSystemTradeId.equalsIgnoreCase(sourcesystemtradeid)) {
                log.error("Sender and receiver Source system tradeId does not match! Exiting..");
            }
            else {
                utiResponseBean.setSourceSystemTradeID(responseSourceSystemTradeId);
            }
            utiResponseBean.setSendingSourceSystem(utiResponse.getString("sendingsystem", 1));
            utiResponseBean.setTargetSourceSystem(utiResponse.getString("targetsystem", 1));
            utiResponseBean.setMessageID(utiResponse.getString("messageid", 1));
            utiResponseBean.setUti(utiResponse.getString("uti", 1)); /// Have to implement the  UTI

            String errorCode = String.valueOf(response.getStatusLine().getStatusCode());
           
            utiResponseBean.setErrorCode(errorCode);
            errorFullMessage = response.getStatusLine().getReasonPhrase();
            utiResponseBean.setErrorDescription(errorFullMessage); //End Response 
        }
        catch (Exception ex) {
            errorFullMessage = ex.getMessage();
            errorFullMessage = errorFullMessage.replace(",", "_");
            log.error("Error Occured: \n" + ex.getMessage());
            log.error("Problem occured While Sending Sourcesystemtradeid : " + sourcesystemtradeid);
            utiResponseBean.setSourceSystemTradeID(sourcesystemtradeid);
            utiResponseBean.setTargetSourceSystem(errorMsg);
            utiResponseBean.setMessageID(errorMsg);
            utiResponseBean.setUti("");
            utiResponseBean.setErrorCode(errorMsg);
            utiResponseBean.setErrorDescription(errorFullMessage);
        }
        finally {

            utiResponseBeanList.add(utiResponseBean);
        }
    }

    /**
     * Processes the incoming response xml
     * 
     * @param response xml
     * @return Table with the values in the response xml
     * @throws OException
     */
    private Table processResponse(String responseXml) throws OException {

        String response;
        Table respTbl = Table.tableNew();
        Table respTblChild = Table.tableNew();
        Table respTblInnerChild = Table.tableNew();
        Table retTbl = null;
        response = "<?xml version=\"1.0\"?>" // <----- Mandatory Piece of XML
            + "<start>"
            + responseXml
            + "</start>";

        XString xstring;
        try {
            xstring = Str.xstringNew();

            respTbl = Table.xmlStringToTable(response, xstring, 1, 0);
            respTblChild = respTbl.getTable(1, 1);
            respTblInnerChild = respTblChild.getTable(1, 1);
            retTbl = respTblInnerChild.copyTable();

        }
        catch (OException e) {
            log.error("unable to process response xml: \n" + e.getMessage());
            e.printStackTrace();
        }
        finally {
            respTbl.destroy();
            respTblChild.destroy();
        }
        return retTbl;
    }

    /**
     * Creates the Request Data
     * 
     * @throws OException
     */
    private void createHashMapForAllDeals() throws OException {

        for (int row : new TableRowIterator(utiDealData)) {

            HashMap<String, String> hashMapPerDeal = new HashMap<String, String>();

            if (1 == utiDealData.getInt("sent_flag", row)) {
                hashMapPerDeal.put("tran_num", utiDealData.getString("tran_num", row));
                hashMapPerDeal.put("applicationkey", utiDealData.getString("applicationkey", row));
                hashMapPerDeal.put("generationmethod", utiDealData.getString("generationmethod", row));
                hashMapPerDeal.put("generationmethodversion", utiDealData.getString("generationmethodversion", row));
                hashMapPerDeal.put("sendingsystem", utiDealData.getString("sendingsystem", row));
                hashMapPerDeal.put("targetsystem", utiDealData.getString("targetsystem", row));
                hashMapPerDeal.put("messageid", utiDealData.getString("messageid", row));
                hashMapPerDeal.put("sourcesystemtradeid", utiDealData.getString("sourcesystemtradeid", row));

                hashMapPerDeal.put("remitformat", utiDealData.getString("remitformat", row));
                hashMapPerDeal.put("buyer", utiDealData.getString("buyer", row));
                hashMapPerDeal.put("buyertype", utiDealData.getString("buyertype", row));
                hashMapPerDeal.put("seller", utiDealData.getString("seller", row));
                hashMapPerDeal.put("sellertype", utiDealData.getString("sellertype", row));
                hashMapPerDeal.put("contracttype", utiDealData.getString("contracttype", row));
                hashMapPerDeal.put("commodity", utiDealData.getString("commodity", row));
                hashMapPerDeal.put("settlement", utiDealData.getString("settlement", row));

                hashMapPerDeal.put("tradedate", utiDealData.getString("tradedate", row));

                String remitFormat = utiDealData.getString("remitformat", row);
                if (!"F2".equalsIgnoreCase(remitFormat)) {
                    hashMapPerDeal.put("price", utiDealData.getString("price", row));
                    hashMapPerDeal.put("currency", utiDealData.getString("currency", row));
                    hashMapPerDeal.put("quantity", utiDealData.getString("quantity", row));
                    hashMapPerDeal.put("quantityunit", utiDealData.getString("quantityunit", row));
                }

                hashMapPerDeal.put("deliverypoint", utiDealData.getString("deliverypoint", row));
                hashMapPerDeal.put("deliverystartdate", utiDealData.getString("deliverystartdate", row));
                hashMapPerDeal.put("deliveryenddate", utiDealData.getString("deliveryenddate", row));
                hashMapPerDeal.put("progressivenumber", String.valueOf(utiDealData.getInt("progressivenumber", row)));

                listOfAllDeals.add(hashMapPerDeal);
            }
        }

    }

    /**
     * handles the number of retrys to send the message
     * 
     * @return true or false
     * 
     */
    private HttpRequestRetryHandler retryHandler() {
        return (exception, executionCount, context) -> {
            if (executionCount >= maxRetries) {
                throw new FenixRuntimeException("Failed to initialise HTTPS connection in " + executionCount
                    + " attepmts. See log file for more details.");
            }
            if (exception instanceof InterruptedIOException) {
                log.error("failed to send message, Timeout!");
                exception.printStackTrace();
                return false;
            }
            if (exception instanceof UnknownHostException) {
                log.error("failed to send message, unknown host!");
                exception.printStackTrace();
                return false;
            }
            if (exception instanceof ConnectTimeoutException) {
                log.error("failed to send message, connection refused!");
                exception.printStackTrace();
                return false;
            }
            if (exception instanceof SSLException) {
                log.error("SSL handshake exception");
                log.error("failed to send message, invalid SSL!");
                exception.printStackTrace();
                return false;
            }

            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();

            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent) {
                // Retry if the request is considered idempotent
                return true;
            }
            return false;
        };
    }

}
