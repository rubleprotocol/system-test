package stest.tron.wallet.common.client.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.netty.util.internal.StringUtil;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.testng.collections.Lists;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.zen.address.DiversifierT;

@Slf4j
public class HttpMethed {

  static HttpClient httpClient;
  static HttpPost httppost;

  static Integer connectionTimeout =
      Configuration.getByPath("testng.conf").getInt("defaultParameter.httpConnectionTimeout");
  static Integer soTimeout =
      Configuration.getByPath("testng.conf").getInt("defaultParameter.httpSoTimeout");


  static JSONObject transactionApprovedListContent;
  public static volatile Integer witnessNum;

  /** constructor. */
  private static volatile String httpnode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(1);

  /** constructor. */
  public static synchronized Integer getWitnessNum() {
    if (null == witnessNum) {
      witnessNum = parseResponseContent(listwitnesses(httpnode)).getJSONArray("witnesses").size();
    }

    return witnessNum;
  }

  static {
    PoolingClientConnectionManager pccm = new PoolingClientConnectionManager();
    pccm.setDefaultMaxPerRoute(80);
    pccm.setMaxTotal(100);

    httpClient = new DefaultHttpClient(pccm);
  }

  /** constructor. */
  public static synchronized HttpResponse updateAccount(
      String httpNode, byte[] updateAccountAddress, String accountName, String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/updateaccount";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("account_name", str2hex(accountName));
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(updateAccountAddress));
      HttpResponse response = createConnect(requestUrl, userBaseObj2);
      String transactionString = EntityUtils.toString(response.getEntity());
      String transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
      return response;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse setAccountId(
      String httpNode,
      byte[] setAccountIdAddress,
      String accountId,
      Boolean visable,
      String fromKey) {
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/setaccountid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("account_id", accountId);
      userBaseObj2.addProperty(
          "owner_address", Base58.encode58Check(PublicMethed.getFinalAddress(fromKey)));
      userBaseObj2.addProperty("visible", visable);
      HttpResponse response = createConnect(requestUrl, userBaseObj2);
      String transactionString = EntityUtils.toString(response.getEntity());
      String transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
      return response;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse updateWitness(
      String httpNode, byte[] witnessAddress, String updateUrl, String fromKey) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/updatewitness";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("update_url", str2hex(updateUrl));
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(witnessAddress));
      response = createConnect(requestUrl, userBaseObj2);
      String transactionString = EntityUtils.toString(response.getEntity());
      String transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse voteWitnessAccount(
      String httpNode, byte[] ownerAddress, JsonArray voteArray, String fromKey) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/votewitnessaccount";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.add("votes", voteArray);
      logger.info(userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);
      String transactionString = EntityUtils.toString(response.getEntity());
      String transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse createAccount(
      String httpNode, byte[] ownerAddress, byte[] accountAddress, String fromKey) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createaccount";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("account_address", ByteArray.toHexString(accountAddress));
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      response = createConnect(requestUrl, userBaseObj2);
      String transactionString = EntityUtils.toString(response.getEntity());
      String transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse createWitness(String httpNode, byte[] ownerAddress, String url) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createwitness";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("url", str2hex(url));
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      response = createConnect(requestUrl, userBaseObj2);
      logger.info(userBaseObj2.toString());
      // transactionString = EntityUtils.toString(response.getEntity());
      // transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      // response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse withdrawBalance(String httpNode, byte[] witnessAddress) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/withdrawbalance";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(witnessAddress));
      response = createConnect(requestUrl, userBaseObj2);
      logger.info(userBaseObj2.toString());
      // transactionString = EntityUtils.toString(response.getEntity());
      // transactionSignString = gettransactionsign(httpNode,transactionString,fromKey);
      // response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }


  /** constructor. */
  public static synchronized HttpResponse withdrawExpireUnfreeze(String httpNode, byte[] ownerAddress,String ownerKey) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/withdrawexpireunfreeze";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      response = createConnect(requestUrl, userBaseObj2);
      logger.info(userBaseObj2.toString());
      String transactionString = EntityUtils.toString(response.getEntity());
      String transactionSignString = gettransactionsign(httpNode,transactionString,ownerKey);
      response = broadcastTransaction(httpNode,transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse sendCoin(
      String httpNode, byte[] fromAddress, byte[] toAddress, Long amount, String fromKey) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createtransaction";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("to_address", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(fromAddress));
      userBaseObj2.addProperty("amount", amount);
      response = createConnect(requestUrl, userBaseObj2);
      String transactionString = EntityUtils.toString(response.getEntity());
      String transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized String sendCoin(
      String httpNode,
      byte[] fromAddress,
      byte[] toAddress,
      Long amount,
      String notes,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;

    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createtransaction";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("to_address", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(fromAddress));
      userBaseObj2.addProperty("amount", amount);
      userBaseObj2.addProperty("extra_data", ByteArray.toHexString(notes.getBytes()));
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    JSONObject responseContent = HttpMethed.parseStringContent(transactionString);
    return responseContent.getString("txID");
  }

  /** constructor. */
  public static synchronized HttpResponse sendCoin(
      String httpNode,
      byte[] fromAddress,
      byte[] toAddress,
      Long amount,
      Integer permissionId,
      String[] managerKeys) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createtransaction";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("to_address", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(fromAddress));
      userBaseObj2.addProperty("amount", amount);
      userBaseObj2.addProperty("Permission_id", permissionId);
      response = createConnect(requestUrl, userBaseObj2);
      String transactionSignString = EntityUtils.toString(response.getEntity());
      HttpResponse getSignWeightResponse;
      HttpResponse getTransactionApprovedListResponse;

      for (String key : managerKeys) {
        transactionSignString = gettransactionsign(httpNode, transactionSignString, key);
        getSignWeightResponse = getSignWeight(httpNode, transactionSignString);
        JSONObject signResponseContent = parseResponseContent(getSignWeightResponse);
        logger.info("-----------sign information-----------------");
        printJsonContent(signResponseContent);
        getSignWeightResponse = getTransactionApprovedList(httpNode, transactionSignString);
        signResponseContent = parseResponseContent(getSignWeightResponse);
        logger.info("-----------get Transaction Approved List-----------------");
        printJsonContent(signResponseContent);
      }
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse sendCoinReplaceTransactionType(
      String httpNode,
      byte[] fromAddress,
      byte[] toAddress,
      Long amount,
      Integer permissionId,
      String[] managerKeys,
      String originType,
      String type) {
    HttpResponse response;
    String transactionSignString;
    JSONObject signResponseContent;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createtransaction";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("to_address", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(fromAddress));
      userBaseObj2.addProperty("amount", amount);
      userBaseObj2.addProperty("Permission_id", permissionId);
      response = createConnect(requestUrl, userBaseObj2);
      transactionSignString = EntityUtils.toString(response.getEntity());
      HttpResponse getSignWeightResponse;

      for (String key : managerKeys) {
        transactionSignString = gettransactionsign(httpNode, transactionSignString, key);
        if (!type.isEmpty()) {
          transactionSignString = transactionSignString.replaceAll(originType, type);
        }
        getSignWeightResponse = getSignWeight(httpNode, transactionSignString);
        logger.info("-----------sign information-----------------");
        getSignWeightResponse = getTransactionApprovedList(httpNode, transactionSignString);
        signResponseContent = parseResponseContent(getSignWeightResponse);
        logger.info("-----------get Transaction Approved List-----------------");
        printJsonContent(signResponseContent);
        return getSignWeightResponse;
      }
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return null;
  }

  /** constructor. */
  public static synchronized String sendCoinGetTxid(
      String httpNode, byte[] fromAddress, byte[] toAddress, Long amount, String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    JSONObject responseContent;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createtransaction";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("to_address", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(fromAddress));
      userBaseObj2.addProperty("amount", amount);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseStringContent(transactionString);
    logger.info(responseContent.getString("txID"));
    return responseContent.getString("txID");
  }

  /** constructor. */
  public static synchronized HttpResponse createProposal(
      String httpNode, byte[] ownerAddress, Long proposalKey, Long proposalValue, String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/proposalcreate";
      JsonObject userBaseObj2 = new JsonObject();
      JsonObject proposalMap = new JsonObject();
      proposalMap.addProperty("key", proposalKey);
      proposalMap.addProperty("value", proposalValue);
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.add("parameters", proposalMap);

      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse approvalProposal(
      String httpNode,
      byte[] ownerAddress,
      Integer proposalId,
      Boolean isAddApproval,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/proposalapprove";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("proposal_id", proposalId);
      userBaseObj2.addProperty("is_add_approval", isAddApproval);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse deleteProposal(
      String httpNode, byte[] ownerAddress, Integer proposalId, String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/proposaldelete";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("proposal_id", proposalId);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getEnergyPric(String httpNode) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/getenergyprices";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBandPric(String httpNode) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/getbandwidthprices";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getChainParameters(String httpNode) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/getchainparameters";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  public static synchronized Boolean proposalFreezeV2IsOpen(String http) {
    return getProposalValue(http,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0;
  }

  public static synchronized Boolean proposalTronPowerIsOpen(String http) {
    return getProposalValue(http,ProposalEnum.GetAllowNewResourceModel.getProposalName()) > 0;
  }

  /** constructor. */
  public static synchronized Long getProposalValue(String httpNode,String proposalName) {
    HttpResponse response;
    JSONObject responseContent;
    response = HttpMethed.getChainParameters(httpNode);
    responseContent = HttpMethed.parseResponseContent(response);
    JSONArray temp;
    temp = responseContent.getJSONArray("chainParameter");
    for (int i = 0; i < temp.size(); i++) {
      if (temp.getJSONObject(i).get("key").equals(proposalName)) {
        if(temp.getJSONObject(i).containsKey("value")) {
          return temp.getJSONObject(i).getLong("value");
        } else {
          return 0L;
        }


      }
    }
    return 0L;

  }

  /** constructor. */
  public static synchronized HttpResponse accountPermissionUpdate(
      String httpNode,
      byte[] ownerAddress,
      JsonObject ownerObject,
      JsonObject witnessObject,
      JsonObject activesObject,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/accountpermissionupdate";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.add("owner", ownerObject);
      // userBaseObj2.add("witness", witnessObject);
      userBaseObj2.add("actives", activesObject);
      logger.info(userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);

      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse exchangeCreate(
      String httpNode,
      byte[] ownerAddress,
      String firstTokenId,
      Long firstTokenBalance,
      String secondTokenId,
      Long secondTokenBalance,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/exchangecreate";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("first_token_id", str2hex(firstTokenId));
      userBaseObj2.addProperty("first_token_balance", firstTokenBalance);
      userBaseObj2.addProperty("second_token_id", str2hex(secondTokenId));
      userBaseObj2.addProperty("second_token_balance", secondTokenBalance);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse exchangeInject(
      String httpNode,
      byte[] ownerAddress,
      Integer exchangeId,
      String tokenId,
      Long quant,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/exchangeinject";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("exchange_id", exchangeId);
      userBaseObj2.addProperty("token_id", str2hex(tokenId));
      userBaseObj2.addProperty("quant", quant);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse exchangeWithdraw(
      String httpNode,
      byte[] ownerAddress,
      Integer exchangeId,
      String tokenId,
      Long quant,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/exchangewithdraw";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("exchange_id", exchangeId);
      userBaseObj2.addProperty("token_id", str2hex(tokenId));
      userBaseObj2.addProperty("quant", quant);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse exchangeTransaction(
      String httpNode,
      byte[] ownerAddress,
      Integer exchangeId,
      String tokenId,
      Long quant,
      Long expected,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/exchangetransaction";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("exchange_id", exchangeId);
      userBaseObj2.addProperty("token_id", str2hex(tokenId));
      userBaseObj2.addProperty("quant", quant);
      userBaseObj2.addProperty("expected", expected);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse assetIssue(
      String httpNode,
      byte[] ownerAddress,
      String name,
      String abbr,
      Long totalSupply,
      Integer trxNum,
      Integer num,
      Long startTime,
      Long endTime,
      Integer voteScore,
      Integer precision,
      String description,
      String url,
      Long freeAssetNetLimit,
      Long publicFreeAssetNetLimit,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createassetissue";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("name", str2hex(name));
      userBaseObj2.addProperty("abbr", str2hex(abbr));
      userBaseObj2.addProperty("total_supply", totalSupply);
      userBaseObj2.addProperty("trx_num", trxNum);
      userBaseObj2.addProperty("num", num);
      userBaseObj2.addProperty("precision", precision);
      userBaseObj2.addProperty("start_time", startTime);
      userBaseObj2.addProperty("end_time", endTime);
      userBaseObj2.addProperty("vote_score", voteScore);
      userBaseObj2.addProperty("description", str2hex(description));
      userBaseObj2.addProperty("url", str2hex(url));
      userBaseObj2.addProperty("free_asset_net_limit", freeAssetNetLimit);
      userBaseObj2.addProperty("public_free_asset_net_limit", publicFreeAssetNetLimit);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse transferAsset(
      String httpNode,
      byte[] ownerAddress,
      byte[] toAddress,
      String assetIssueById,
      Long amount,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/transferasset";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("to_address", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("asset_name", str2hex(assetIssueById));
      userBaseObj2.addProperty("amount", amount);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse deployContract(
      String httpNode,
      String name,
      String abi,
      String bytecode,
      Long bandwidthLimit,
      Long feeLimit,
      Integer consumeUserResourcePercent,
      Long originEnergyLimit,
      Long callValue,
      Integer tokenId,
      Long tokenValue,
      byte[] ownerAddress,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/deploycontract";
      JsonObject userBaseObj2 = new JsonObject();
      // userBaseObj2.addProperty("name", str2hex(name));
      userBaseObj2.addProperty("name", name);
      userBaseObj2.addProperty("abi", abi);
      userBaseObj2.addProperty("bytecode", bytecode);
      userBaseObj2.addProperty("bandwidth_limit", bandwidthLimit);
      userBaseObj2.addProperty("fee_limit", feeLimit);
      userBaseObj2.addProperty("consume_user_resource_percent", consumeUserResourcePercent);
      userBaseObj2.addProperty("origin_energy_limit", originEnergyLimit);
      userBaseObj2.addProperty("call_value", callValue);
      userBaseObj2.addProperty("token_id", tokenId);
      userBaseObj2.addProperty("tokenValue", tokenValue);
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized String deployContractGetTxid(
      String httpNode,
      String name,
      String abi,
      String bytecode,
      Long bandwidthLimit,
      Long feeLimit,
      Integer consumeUserResourcePercent,
      Long originEnergyLimit,
      Long callValue,
      Integer tokenId,
      Long tokenValue,
      byte[] ownerAddress,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    JSONObject responseContent;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/deploycontract";
      JsonObject userBaseObj2 = new JsonObject();
      // userBaseObj2.addProperty("name", str2hex(name));
      userBaseObj2.addProperty("name", name);
      userBaseObj2.addProperty("abi", abi);
      userBaseObj2.addProperty("bytecode", bytecode);
      userBaseObj2.addProperty("bandwidth_limit", bandwidthLimit);
      userBaseObj2.addProperty("fee_limit", feeLimit);
      userBaseObj2.addProperty("consume_user_resource_percent", consumeUserResourcePercent);
      userBaseObj2.addProperty("origin_energy_limit", originEnergyLimit);
      userBaseObj2.addProperty("call_value", callValue);
      userBaseObj2.addProperty("token_id", tokenId);
      userBaseObj2.addProperty("call_token_value", tokenValue);
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));

      logger.info(userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseStringContent(transactionString);
    return responseContent.getString("txID");
  }

  /** constructor. */
  public static synchronized HttpResponse deployContractGetTxidWithTooBigLong(
      String httpNode,
      String name,
      String abi,
      String bytecode,
      Long bandwidthLimit,
      Long feeLimit,
      Integer consumeUserResourcePercent,
      Long originEnergyLimit,
      Long callValue,
      Integer tokenId,
      Long tokenValue,
      byte[] ownerAddress,
      String fromKey) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/deploycontract";

      String text = "{\"call_token_value\": 10000000e100000000}";
      JSONObject jsonObject = JSONObject.parseObject(text);
      logger.info("jsonObject: " + jsonObject.toString());
      jsonObject.put("name", name);
      jsonObject.put("abi", abi);
      jsonObject.put("bytecode", bytecode);
      jsonObject.put("bandwidth_limit", bandwidthLimit);
      jsonObject.put("fee_limit", feeLimit);
      jsonObject.put("consume_user_resource_percent", consumeUserResourcePercent);
      jsonObject.put("origin_energy_limit", originEnergyLimit);
      jsonObject.put("call_value", callValue);
      jsonObject.put("token_id", tokenId);
      jsonObject.put("owner_address", ByteArray.toHexString(ownerAddress));

      logger.info(jsonObject.toString());
      response = createConnect1(requestUrl, jsonObject);
      /*      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);*/
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized String triggerContractGetTxid(
      String httpNode,
      byte[] ownerAddress,
      String contractAddress,
      String functionSelector,
      String parameter,
      Long feeLimit,
      Long callValue,
      Integer tokenId,
      Long tokenValue,
      String data,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    JSONObject responseContent;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/triggersmartcontract";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("contract_address", contractAddress);
      userBaseObj2.addProperty("function_selector", functionSelector);
      userBaseObj2.addProperty("parameter", parameter);
      userBaseObj2.addProperty("fee_limit", feeLimit);
      userBaseObj2.addProperty("call_value", callValue);
      userBaseObj2.addProperty("token_id", tokenId);
      userBaseObj2.addProperty("call_token_value", tokenValue);
      userBaseObj2.addProperty("data", data);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);

      transactionSignString =
          gettransactionsign(
              httpNode, parseStringContent(transactionString).getString("transaction"), fromKey);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseStringContent(transactionSignString);
    return responseContent.getString("txID");
  }

  /** constructor. */
  public static synchronized String triggerContractGetTxidWithVisibleTrue(
      String httpNode,
      String ownerAddress,
      String contractAddress,
      String functionSelector,
      String parameter,
      Long feeLimit,
      Long callValue,
      Integer tokenId,
      Long tokenValue,
      String fromKey) {
    return triggerContractGetTxidWithVisibleTrue(
        httpNode,
        "",
        ownerAddress,
        contractAddress,
        functionSelector,
        parameter,
        feeLimit,
        callValue,
        tokenId,
        tokenValue,
        fromKey);
  }

  /** constructor. */
  public static synchronized String triggerContractGetTxidWithVisibleTrue(
      String httpNode,
      String anotherHttpNode,
      String ownerAddress,
      String contractAddress,
      String functionSelector,
      String parameter,
      Long feeLimit,
      Long callValue,
      Integer tokenId,
      Long tokenValue,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    JSONObject responseContent;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/triggersmartcontract";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ownerAddress);
      userBaseObj2.addProperty("contract_address", contractAddress);
      userBaseObj2.addProperty("function_selector", functionSelector);
      userBaseObj2.addProperty("parameter", parameter);
      userBaseObj2.addProperty("fee_limit", feeLimit);
      userBaseObj2.addProperty("call_value", callValue);
      userBaseObj2.addProperty("token_id", tokenId);
      userBaseObj2.addProperty("call_token_value", tokenValue);
      userBaseObj2.addProperty("visible", true);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);

      transactionSignString =
          gettransactionsign(
              httpNode, parseStringContent(transactionString).getString("transaction"), fromKey);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
      if (!anotherHttpNode.isEmpty()) {
        broadcastTransaction(anotherHttpNode, transactionSignString);
      }
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseStringContent(transactionSignString);
    return responseContent.getString("txID");
  }

  /** constructor. */
  public static synchronized HttpResponse triggerConstantContract(
      String httpNode,
      byte[] ownerAddress,
      String contractAddress,
      String functionSelector,
      String parameter,
      Long feeLimit,
      String fromKey) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/triggerconstantcontract";
      JsonObject userBaseObj2 = new JsonObject();

      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("contract_address", contractAddress);
      userBaseObj2.addProperty("function_selector", functionSelector);
      userBaseObj2.addProperty("parameter", parameter);
      userBaseObj2.addProperty("fee_limit", feeLimit);

      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse triggerConstantContract(
      String httpNode,
      byte[] ownerAddress,
      String contractAddress,
      String functionSelector,
      String parameter) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/triggerconstantcontract";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("contract_address", contractAddress);
      userBaseObj2.addProperty("function_selector", functionSelector);
      userBaseObj2.addProperty("parameter", parameter);
      response = createConnect(requestUrl, userBaseObj2);
      return response;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse triggerConstantContractWithData(
      String httpNode,
      byte[] ownerAddress,
      String contractAddress,
      String functionSelector,
      String parameter,
      String data,
      long call_value,
      long call_token_value,
      long token_id) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/triggerconstantcontract";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      if(contractAddress == null) {
        String tem = null;
        userBaseObj2.addProperty("contract_address", tem);
      }else {
        userBaseObj2.addProperty("contract_address", contractAddress);
      }
      userBaseObj2.addProperty("function_selector", functionSelector);
      userBaseObj2.addProperty("parameter", parameter);
      userBaseObj2.addProperty("data", data);
      userBaseObj2.addProperty("call_value", call_value);

      userBaseObj2.addProperty("token_id", token_id);
      userBaseObj2.addProperty("call_token_value", call_token_value);

      response = createConnect(requestUrl, userBaseObj2);
      return response;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse triggerConstantContractFromSolidity(
      String httSoliditypNode,
      byte[] ownerAddress,
      String contractAddress,
      String functionSelector,
      String parameter) {
    HttpResponse response;
    try {
      final String requestUrl =
          "http://" + httSoliditypNode + "/walletsolidity/triggerconstantcontract";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("contract_address", contractAddress);
      userBaseObj2.addProperty("function_selector", functionSelector);
      userBaseObj2.addProperty("parameter", parameter);
      response = createConnect(requestUrl, userBaseObj2);
      return response;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse triggerConstantContractFromPbft(
      String httpPbftNode,
      byte[] ownerAddress,
      String contractAddress,
      String functionSelector,
      String parameter) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpPbftNode + "/walletpbft/triggerconstantcontract";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("contract_address", contractAddress);
      userBaseObj2.addProperty("function_selector", functionSelector);
      userBaseObj2.addProperty("parameter", parameter);
      response = createConnect(requestUrl, userBaseObj2);
      return response;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse participateAssetIssue(
      String httpNode,
      byte[] toAddress,
      byte[] ownerAddress,
      String assetIssueById,
      Long amount,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/participateassetissue";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("to_address", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("asset_name", str2hex(assetIssueById));
      userBaseObj2.addProperty("amount", amount);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse updateAssetIssue(
      String httpNode,
      byte[] ownerAddress,
      String description,
      String url,
      Long newLimit,
      Long newPublicLimit,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/updateasset";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("url", str2hex(url));
      userBaseObj2.addProperty("description", str2hex(description));
      userBaseObj2.addProperty("new_limit", newLimit);
      userBaseObj2.addProperty("new_public_limit", newPublicLimit);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized Boolean verificationResult(HttpResponse response) {
    if (response.getStatusLine().getStatusCode() != 200) {
      return false;
    }
    Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    JSONObject responseContent = HttpMethed.parseResponseContent(response);
    // HttpMethed.printJsonContent(responseContent);
    return Boolean.valueOf(responseContent.getString("result")).booleanValue();
  }

  /** constructor. */
  public static synchronized HttpResponse freezeBalance(
      String httpNode,
      byte[] ownerAddress,
      Long frozenBalance,
      Integer frozenDuration,
      Integer resourceCode,
      String fromKey) {
    if(getProposalValue(httpNode,ProposalEnum.GetAllowNewResourceModel.getProposalName()) != 0
    && getProposalValue(httpNode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) == 0) {
      return freezeBalance(
          httpNode, ownerAddress, frozenBalance, frozenDuration, resourceCode, null, fromKey);
    }

    if(getProposalValue(httpNode,ProposalEnum.GetAllowNewResourceModel.getProposalName()) == 0
        && getProposalValue(httpNode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) == 0) {
      return freezeBalance(
          httpNode, ownerAddress, frozenBalance, frozenDuration, resourceCode, null, fromKey);
    }

    if(getProposalValue(httpNode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0) {
      return freezeBalanceV2(
          httpNode, ownerAddress, frozenBalance, resourceCode, null,fromKey);
    }


    return null;



  }

  /** constructor. */
  public static synchronized HttpResponse freezeBalance(
      String httpNode,
      byte[] ownerAddress,
      Long frozenBalance,
      Integer frozenDuration,
      Integer resourceCode,
      byte[] receiverAddress,
      String fromKey) {
    if(getProposalValue(httpNode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) != 0) {
      return freezeBalanceV2(httpNode,ownerAddress,frozenBalance,resourceCode,receiverAddress,fromKey);
    } else {
      return freezeBalanceV1(httpNode,ownerAddress,frozenBalance,frozenDuration,resourceCode,receiverAddress,fromKey);
    }
  }


  /** constructor. */
  public static synchronized HttpResponse freezeBalanceV1(
      String httpNode,
      byte[] ownerAddress,
      Long frozenBalance,
      Integer frozenDuration,
      Integer resourceCode,
      byte[] receiverAddress,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/freezebalance";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("frozen_balance", frozenBalance);
      userBaseObj2.addProperty("frozen_duration", frozenDuration);
      if (resourceCode == 0) {
        userBaseObj2.addProperty("resource", "BANDWIDTH");
      }
      if (resourceCode == 1) {
        userBaseObj2.addProperty("resource", "ENERGY");
      }
      if (resourceCode == 2) {
        userBaseObj2.addProperty("resource", "TRON_POWER");
      }
      if (receiverAddress != null) {
        userBaseObj2.addProperty("receiver_address", ByteArray.toHexString(receiverAddress));
      }
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse freezeBalanceV2(
      String httpNode,
      byte[] ownerAddress,
      Long frozenBalance,
      Integer resourceCode,
      byte[] receiverAddress,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/freezebalancev2";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("frozen_balance", frozenBalance);
      if (resourceCode == 0) {
        userBaseObj2.addProperty("resource", "BANDWIDTH");
      }
      if (resourceCode == 1) {
        userBaseObj2.addProperty("resource", "ENERGY");
      }
      if (resourceCode == 2) {
        if(getProposalValue(httpNode,ProposalEnum.GetAllowNewResourceModel.getProposalName()) == 1) {
          userBaseObj2.addProperty("resource", "TRON_POWER");
        } else {
          userBaseObj2.addProperty("resource", "ENERGY");
        }

      }

      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);

      if(receiverAddress != null) {
        waitToProduceOneBlock(httpNode);
        delegateresource(httpNode,ownerAddress,frozenBalance / 2,resourceCode,receiverAddress,fromKey);

      }
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse delegateresource(
      String httpNode, byte[] ownerAddress, Long delegateAmount, Integer resourceCode,byte[] receiverAddress,String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/delegateresource";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("receiver_address", ByteArray.toHexString(receiverAddress));
      userBaseObj2.addProperty("balance", delegateAmount);
      if (resourceCode == 0) {
        userBaseObj2.addProperty("resource", "BANDWIDTH");
      }
      if (resourceCode == 1) {
        userBaseObj2.addProperty("resource", "ENERGY");
      }
      if (resourceCode == 2) {
        if(getProposalValue(httpNode,ProposalEnum.GetAllowNewResourceModel.getProposalName()) == 1) {
          userBaseObj2.addProperty("resource", "TRON_POWER");
        } else {
          userBaseObj2.addProperty("resource", "ENERGY");
        }
      }

      response = createConnect(requestUrl, userBaseObj2);

      transactionString = EntityUtils.toString(response.getEntity());
      logger.info("transactionString:" + transactionString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);


    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }

    return response;


  }


  /** constructor. */
  public static synchronized HttpResponse unDelegateresource(
      String httpNode, byte[] ownerAddress, Long delegteAmount, Integer resourceCode,byte[] receiverAddress,String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/undelegateresource";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("receiver_address", ByteArray.toHexString(receiverAddress));
      userBaseObj2.addProperty("balance", delegteAmount);
      if (resourceCode == 0) {
        userBaseObj2.addProperty("resource", "BANDWIDTH");
      }
      if (resourceCode == 1) {
        userBaseObj2.addProperty("resource", "ENERGY");
      }
      if (resourceCode == 2) {
        if(getProposalValue(httpNode,ProposalEnum.GetAllowNewResourceModel.getProposalName()) == 1) {
          userBaseObj2.addProperty("resource", "TRON_POWER");
        } else {
          userBaseObj2.addProperty("resource", "ENERGY");
        }
      }

      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);

      printJsonContent(parseResponseContent(response));

    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }

    return response;


  }

  /** constructor. */
  public static synchronized HttpResponse unFreezeBalance(
      String httpNode, byte[] ownerAddress, Long unfreezeBalance,Integer resourceCode, String fromKey) {
    if(getProposalValue(httpNode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) == 0) {
      return unFreezeBalance(httpNode, ownerAddress, unfreezeBalance,resourceCode, null, fromKey);
    }

    if(getProposalValue(httpNode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) != 0) {
      return unFreezeBalanceV2(
          httpNode, ownerAddress,unfreezeBalance, resourceCode, fromKey);
    }

    return null;


  }

  /** constructor. */
  public static synchronized HttpResponse unFreezeBalance(
      String httpNode,
      byte[] ownerAddress,
      Long unfreezeBalance,
      Integer resourceCode,
      byte[] receiverAddress,
      String fromKey) {
    if(getProposalValue(httpNode,ProposalEnum.GetAllowNewResourceModel.getProposalName()) == 0 && resourceCode == 2) {
      resourceCode = 1;
    }
    if(getProposalValue(httpnode,ProposalEnum.GetUnfreezeDelayDays.getProposalName()) > 0) {
      unDelegateresource(httpNode,ownerAddress,unfreezeBalance / 2,resourceCode,receiverAddress,fromKey);
      waitToProduceOneBlock(httpNode);
      return unFreezeBalanceV2(httpnode,ownerAddress,unfreezeBalance,resourceCode,fromKey);
    } else {
      return unFreezeBalanceV1(httpNode,ownerAddress,resourceCode,receiverAddress,fromKey);
    }
  }

  /** constructor. */
  public static synchronized HttpResponse unFreezeBalanceV1(
      String httpNode,
      byte[] ownerAddress,
      Integer resourceCode,
      byte[] receiverAddress,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/unfreezebalance";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      if (resourceCode == 0) {
        userBaseObj2.addProperty("resource", "BANDWIDTH");
      }
      if (resourceCode == 1) {
        userBaseObj2.addProperty("resource", "ENERGY");
      }
      if (resourceCode == 2) {
        userBaseObj2.addProperty("resource", "TRON_POWER");
      }
      if (receiverAddress != null) {
        userBaseObj2.addProperty("receiver_address", ByteArray.toHexString(receiverAddress));
      }
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse unFreezeBalanceV2(
      String httpNode,
      byte[] ownerAddress,
      Long unfreezeBalance,
      Integer resourceCode,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/unfreezebalancev2";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("unfreeze_balance", unfreezeBalance);
      if (resourceCode == 0) {
        userBaseObj2.addProperty("resource", "BANDWIDTH");
      }
      if (resourceCode == 1) {
        userBaseObj2.addProperty("resource", "ENERGY");
      }
      if (resourceCode == 2) {
        userBaseObj2.addProperty("resource", "TRON_POWER");
      }

      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      //logger.info(transactionSignString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
      logger.info(response.getEntity().toString());
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized String gettransactionsign(
      String httpNode, String transactionString, String privateKey) {
    String transactionSignString;
    boolean visible = transactionString.contains("visible\":true");
    transactionSignString = TransactionUtils.getTransactionSign(transactionString, privateKey,
        visible);
    return transactionSignString;
  }

  /** constructor. */
  public static synchronized HttpResponse broadcastTransaction(String httpNode, String transactionSignString) {
    HttpResponse response;
    JSONObject responseContent;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/broadcasttransaction";
      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(requestUrl);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (transactionSignString != null) {
        StringEntity entity = new StringEntity(transactionSignString, Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }

    responseContent = HttpMethed.parseResponseContent(response);
    Integer times = 0;

    while (times++ <= 10
        && responseContent.getString("code") != null
        && responseContent.getString("code").equalsIgnoreCase("SERVER_BUSY")) {
      logger.info("retry num are " + times);
      try {
        response = httpClient.execute(httppost);
      } catch (Exception e) {
        e.printStackTrace();
        httppost.releaseConnection();
        return null;
      }
      responseContent = HttpMethed.parseResponseContent(response);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    httppost.releaseConnection();
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse broadcasthex(String httpNode, String transactionHex) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/broadcasthex";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("transaction", transactionHex);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAccountById(String httpNode, String accountId, Boolean visable) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getaccountbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("account_id", accountId);
      userBaseObj2.addProperty("visible", visable);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAccountByIdFromSolidity(
      String httpSolidityNode, String accountId, Boolean visable) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getaccountbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("account_id", accountId);
      userBaseObj2.addProperty("visible", visable);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAccountByIdFromPbft(
      String httpSolidityNode, String accountId, Boolean visable) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getaccountbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("account_id", accountId);
      userBaseObj2.addProperty("visible", visable);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAccount(String httpNode, byte[] queryAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getaccount";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }



  /** constructor. */
  public static synchronized Long getAccountForResponse(String httpNode, byte[] queryAddress, Integer times) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getaccount";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      Long duration = createConnectForResponse(requestUrl, userBaseObj2, times);
      return duration;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return -1L;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse getAccountFromSolidity(String httpSolidityNode, byte[] queryAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getaccount";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      logger.info(requestUrl);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAccountFromPbft(String httpSolidityNode, byte[] queryAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getaccount";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getContract(String httpNode, String contractAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getcontract";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", contractAddress);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getSignWeight(String httpNode, String transactionSignString) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getsignweight";
      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(requestUrl);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (transactionSignString != null) {
        StringEntity entity = new StringEntity(transactionSignString, Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    // httppost.releaseConnection();
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionApprovedList(
      String httpNode, String transactionSignString) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getapprovedlist";
      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(requestUrl);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (transactionSignString != null) {
        StringEntity entity = new StringEntity(transactionSignString, Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    // httppost.releaseConnection();
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse listExchanges(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/listexchanges";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse listExchangesFromSolidity(String httpSolidityNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/listexchanges";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse listExchangesFromPbft(String httpSolidityNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/listexchanges";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse listNodes(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/listnodes";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getStatsInfo(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/monitor/getstatsinfo";
      response = createConnectForGet(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getNextmaintenanceTime(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getnextmaintenancetime";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getChainParameter(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getchainparameters";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getNodeInfo(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getnodeinfo";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse listwitnesses(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/listwitnesses";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse listwitnesses(String httpNode, Boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/listwitnesses";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse listwitnessesFromSolidity(String httpSolidityNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/listwitnesses";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse listwitnessesFromPbft(String httpSolidityNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/listwitnesses";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse listProposals(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/listproposals";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getExchangeById(String httpNode, Integer exchangeId) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getexchangebyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("id", exchangeId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getExchangeByIdFromSolidity(
      String httpSolidityNode, Integer exchangeId) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getexchangebyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("id", exchangeId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getExchangeByIdFromPbft(String httpSolidityNode, Integer exchangeId) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getexchangebyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("id", exchangeId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getProposalById(String httpNode, Integer proposalId) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getproposalbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("id", proposalId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueListByName(String httpNode, String name) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getassetissuelistbyname";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", str2hex(name));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueListByNameFromSolidity(
      String httpSolidityNode, String name) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getassetissuelistbyname";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", str2hex(name));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueListByNameFromPbft(String httpPbftNode, String name) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpPbftNode + "/walletpbft/getassetissuelistbyname";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", str2hex(name));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueById(String httpNode, String assetIssueId) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getassetissuebyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", assetIssueId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueByIdFromSolidity(
      String httpSolidityNode, String assetIssueId) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getassetissuebyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", assetIssueId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueByIdFromPbft(
      String httpSolidityNode, String assetIssueId) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getassetissuebyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", assetIssueId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionById(String httpNode, String txid) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/gettransactionbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", txid);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized Long getTransactionByIdForResponse(String httpNode, String txid, Integer times) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/gettransactionbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", txid);
      Long duration = createConnectForResponse(requestUrl, userBaseObj2, times);
      return duration;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionByIdFromSolidity(String httpSolidityNode, String txid) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/gettransactionbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", txid);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionByIdFromPbft(String httpSolidityNode, String txid) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/gettransactionbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", txid);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionInfoById(String httpNode, String txid) {
    return getTransactionInfoById(httpNode, txid, false);
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionInfoById(String httpNode, String txid, Boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/gettransactioninfobyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", txid);
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionInfoByIdFromSolidity(
      String httpSolidityNode, String txid) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/gettransactioninfobyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", txid);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionInfoByIdFromPbft(String httpSolidityNode, String txid) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/gettransactioninfobyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", txid);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionInfoByBlocknum(String httpNode, long blocknum) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/gettransactioninfobyblocknum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blocknum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionInfoByBlocknumFromSolidity(
      String httpSolidityNode, long blocknum) {
    HttpResponse response;
    try {
      String requestUrl =
          "http://" + httpSolidityNode + "/walletsolidity/gettransactioninfobyblocknum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blocknum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionCountByBlocknumFromSolidity(
      String httpSolidityNode, long blocknum) {
    HttpResponse response;
    try {
      String requestUrl =
          "http://" + httpSolidityNode + "/walletsolidity/gettransactioncountbyblocknum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blocknum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionCountByBlocknumFromPbft(
      String httpSolidityNode, long blocknum) {
    HttpResponse response;
    try {
      String requestUrl =
          "http://" + httpSolidityNode + "/walletpbft/gettransactioncountbyblocknum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blocknum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionsFromThisFromSolidity(
      String httpSolidityNode, byte[] fromAddress, long offset, long limit) {
    HttpResponse response;
    try {
      Map<String, String> map1 = new HashMap<String, String>();
      Map<String, Object> map = new HashMap<String, Object>();
      map1.put("address", ByteArray.toHexString(fromAddress));
      map.put("account", map1);
      map.put("offset", offset);
      map.put("limit", limit);
      String requestUrl = "http://" + httpSolidityNode + "/walletextension/gettransactionsfromthis";
      String jsonStr = new Gson().toJson(map);
      JsonObject jsonObj = new JsonParser().parse(jsonStr).getAsJsonObject();
      response = createConnect(requestUrl, jsonObj);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionsToThisFromSolidity(
      String httpSolidityNode, byte[] toAddress, long offset, long limit) {
    HttpResponse response;
    try {
      Map<String, String> map1 = new HashMap<String, String>();
      Map<String, Object> map = new HashMap<String, Object>();
      map1.put("address", ByteArray.toHexString(toAddress));
      map.put("account", map1);
      map.put("offset", offset);
      map.put("limit", limit);
      String requestUrl = "http://" + httpSolidityNode + "/walletextension/gettransactionstothis";
      String jsonStr = new Gson().toJson(map);
      JsonObject jsonObj = new JsonParser().parse(jsonStr).getAsJsonObject();
      response = createConnect(requestUrl, jsonObj);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueByName(String httpNode, String name) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getassetissuebyname";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", str2hex(name));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueByNameFromSolidity(String httpSolidityNode, String name) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getassetissuebyname";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", str2hex(name));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueByNameFromPbft(String httpSolidityNode, String name) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getassetissuebyname";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", str2hex(name));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized Long getBalance(String httpNode, byte[] queryAddress) {
    HttpResponse response;
    JSONObject responseContent;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getaccount";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseResponseContent(response);
    // HttpMethed.printJsonContent(responseContent);
    // httppost.releaseConnection();
    if (!responseContent.containsKey("balance")) {
      return 0L;
    }
    return Long.parseLong(responseContent.get("balance").toString());
  }

  /** constructor. */
  public static synchronized HttpResponse getAccountNet(String httpNode, byte[] queryAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getaccountnet";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAccountReource(String httpNode, byte[] queryAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getaccountresource";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAccountBalance(
      String httpNode, byte[] queryAddress, Long blockNum, String blockHash) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/getaccountbalance";
      JsonObject addressObj = new JsonObject();
      addressObj.addProperty("address", Base58.encode58Check(queryAddress));
      JsonObject blockObj = new JsonObject();
      blockObj.addProperty("hash", blockHash);
      blockObj.addProperty("number", blockNum);
      JsonObject accountBalanceObj = new JsonObject();
      accountBalanceObj.add("account_identifier", addressObj);
      accountBalanceObj.add("block_identifier", blockObj);
      accountBalanceObj.addProperty("visible", true);
      logger.info(accountBalanceObj.toString());
      response = createConnect(requestUrl, accountBalanceObj);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockBalance(String httpNode, Long blockNum, String blockHash) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/getblockbalance";
      JsonObject blockObj = new JsonObject();
      blockObj.addProperty("hash", blockHash);
      blockObj.addProperty("number", blockNum);
      blockObj.addProperty("visible", true);
      response = createConnect(requestUrl, blockObj);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized Long getBurnTrx(String httpNode) {
    HttpResponse response;
    JSONObject responseContent;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/getburntrx";
      JsonObject blockObj = new JsonObject();
      response = createConnect(requestUrl, blockObj);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseResponseContent(response);
    return responseContent.getLong("burnTrxAmount");
  }

  /** constructor. */
  public static synchronized Long getBurnTrxFromSolidity(String httpNode) {
    HttpResponse response;
    JSONObject responseContent;
    try {
      final String requestUrl = "http://" + httpNode + "/walletsolidity/getburntrx";
      JsonObject blockObj = new JsonObject();
      response = createConnect(requestUrl, blockObj);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseResponseContent(response);
    return responseContent.getLong("burnTrxAmount");
  }

  /** constructor. */
  public static synchronized Long getBurnTrxFromPbft(String httpNode) {
    HttpResponse response;
    JSONObject responseContent;
    try {
      final String requestUrl = "http://" + httpNode + "/walletpbft/getburntrx";
      JsonObject blockObj = new JsonObject();
      response = createConnect(requestUrl, blockObj);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseResponseContent(response);
    return responseContent.getLong("burnTrxAmount");
  }

  /** constructor. */
  public static synchronized HttpResponse getNowBlock(String httpNode) {
    return getNowBlock(httpNode, false);
  }

  /** constructor. */
  public static synchronized HttpResponse getNowBlock(String httpNode, Boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getnowblock";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized Long getNowBlockNum(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getnowblock";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return parseResponseContent(response)
        .getJSONObject("block_header")
        .getJSONObject("raw_data")
        .getLong("number");
  }

  /** constructor. */
  public static synchronized Long getNowBlockNumOnSolidity(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletsolidity/getnowblock";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return parseResponseContent(response)
        .getJSONObject("block_header")
        .getJSONObject("raw_data")
        .getLong("number");
  }

  /** constructor. */
  public static synchronized HttpResponse getNowBlockFromSolidity(String httpSolidityNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getnowblock";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getNowBlockFromPbft(String httpSolidityNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getnowblock";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized void waitToProduceOneBlock(String httpNode) {
    HttpResponse response;
    JSONObject responseContent;
    response = HttpMethed.getNowBlock(httpNode);
    responseContent = HttpMethed.parseResponseContent(response);
    responseContent = HttpMethed.parseStringContent(responseContent.get("block_header").toString());
    responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
    Integer currentBlockNum = Integer.parseInt(responseContent.get("number").toString());
    Integer nextBlockNum = 0;
    Integer times = 0;
    while (nextBlockNum <= currentBlockNum + 1 && times++ <= 10) {
      response = HttpMethed.getNowBlock(httpNode);
      responseContent = HttpMethed.parseResponseContent(response);
      responseContent =
          HttpMethed.parseStringContent(responseContent.get("block_header").toString());
      responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
      nextBlockNum = Integer.parseInt(responseContent.get("number").toString());
      try {
        Thread.sleep(1200);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /** constructor. */
  public static synchronized void waitToProduceOneBlockFromSolidity(String httpNode, String httpSolidityNode) {
    HttpResponse response = HttpMethed.getNowBlock(httpNode);
    JSONObject responseContent = HttpMethed.parseResponseContent(response);
    responseContent = HttpMethed.parseStringContent(responseContent.get("block_header").toString());
    responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
    Integer currentBlockNum = Integer.parseInt(responseContent.get("number").toString());
    Integer nextBlockNum = 0;
    Integer times = 0;
    while (nextBlockNum <= currentBlockNum
        && times++ <= ((getWitnessNum() >= 27) ? 27 : getWitnessNum() + 4)) {
      response = HttpMethed.getNowBlockFromSolidity(httpSolidityNode);
      responseContent = HttpMethed.parseResponseContent(response);
      responseContent =
          HttpMethed.parseStringContent(responseContent.get("block_header").toString());
      responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
      nextBlockNum = Integer.parseInt(responseContent.get("number").toString());
      try {
        Thread.sleep(3500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    logger.info("currentBlockNum2:" + currentBlockNum);
    logger.info("nextBlockNum2:" + nextBlockNum);
  }

  /** constructor. */
  public static synchronized void waitToProduceOneBlockFromPbft(String httpNode, String httpSolidityNode) {
    HttpResponse response = HttpMethed.getNowBlock(httpNode);
    JSONObject responseContent = HttpMethed.parseResponseContent(response);
    responseContent = HttpMethed.parseStringContent(responseContent.get("block_header").toString());
    responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
    Integer currentBlockNum = Integer.parseInt(responseContent.get("number").toString());
    Integer nextBlockNum = 0;
    Integer times = 0;
    while (nextBlockNum <= currentBlockNum && times++ <= 3) {
      response = HttpMethed.getNowBlockFromPbft(httpSolidityNode);
      responseContent = HttpMethed.parseResponseContent(response);
      responseContent =
          HttpMethed.parseStringContent(responseContent.get("block_header").toString());
      responseContent = HttpMethed.parseStringContent(responseContent.get("raw_data").toString());
      nextBlockNum = Integer.parseInt(responseContent.get("number").toString());
      try {
        Thread.sleep(3500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /** constructor. */
  public static synchronized HttpResponse getBlock(String httpNode, String idOrNum, Boolean detail) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblock";
      JsonObject userBaseObj2 = new JsonObject();
      if(null != idOrNum) {
        userBaseObj2.addProperty("id_or_num", idOrNum);
      }
      if(null != detail) {
        userBaseObj2.addProperty("detail", detail);
      }

      if(userBaseObj2.size() == 0) {
        userBaseObj2 = null;
      }


      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }




  /** constructor. */
  public static synchronized HttpResponse getBlockByNum(String httpNode, Long blockNUm) {
    return getBlockByNum(httpNode, blockNUm, false);
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByNum(String httpNode, Long blockNUm, Boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblockbynum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blockNUm);
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized String convertJsonObjectToGetParam(HashMap<String, String> hashMap) {
    StringBuilder result = new StringBuilder("?");
    Iterator<Map.Entry<String, String>> entries = hashMap.entrySet().iterator();

    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry) entries.next();
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      result.append(key);
      result.append("=");
      result.append(value);
      result.append("&");
    }
    return result.substring(0, result.length() - 1);
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByNumWithType(
      String httpNode, HashMap<String, String> hashMap) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblockbynum";
      requestUrl = requestUrl + convertJsonObjectToGetParam(hashMap);
      logger.info("requestUrl:" + requestUrl);
      response = createConnectForGet(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized Long getBlockByNumForResponse(String httpNode, Integer blockNUm, Integer times) {
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblockbynum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blockNUm);
      Long duration = createConnectForResponse(requestUrl, userBaseObj2, times);
      return duration;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return -1L;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByNumFromSolidity(String httpSolidityNode, Long blockNum) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getblockbynum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blockNum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByNumFromPbft(String httpSolidityNode, Long blockNum) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getblockbynum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blockNum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByLimitNext(
      String httpNode, Long startNum, Long endNum) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblockbylimitnext";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("startNum", startNum);
      userBaseObj2.addProperty("endNum", endNum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByLimitNextFromSolidity(
      String httpNode, Long startNum, Long endNum) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletsolidity/getblockbylimitnext";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("startNum", startNum);
      userBaseObj2.addProperty("endNum", endNum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByLimitNextFromPbft(
      String httpNode, Long startNum, Long endNum) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletpbft/getblockbylimitnext";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("startNum", startNum);
      userBaseObj2.addProperty("endNum", endNum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByLastNum(String httpNode, Integer num) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblockbylatestnum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", num);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByLastNum2(String httpNode, Integer num) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletsolidity/getblockbylatestnum2";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", num);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByLastNumFromSolidity(String httpNode, Integer num) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletsolidity/getblockbylatestnum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", num);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByLastNumFromPbft(String httpNode, Integer num) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletpbft/getblockbylatestnum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", num);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockById(String httpNode, String blockId) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getblockbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", blockId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByIdFromSolidity(String httpNode, String blockId) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletsolidity/getblockbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", blockId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByIdFromSolidity(
      String httpNode, String blockId, Integer type) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletsolidity/getblockbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", blockId);
      userBaseObj2.addProperty("type", type);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBlockByIdFromPbft(String httpNode, String blockId) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletpbft/getblockbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", blockId);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getDelegatedResource(
      String httpNode, byte[] fromAddress, byte[] toAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getdelegatedresource";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("fromAddress", ByteArray.toHexString(fromAddress));
      userBaseObj2.addProperty("toAddress", ByteArray.toHexString(toAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceFromSolidity(
      String httpSolidityNode, byte[] fromAddress, byte[] toAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getdelegatedresource";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("fromAddress", ByteArray.toHexString(fromAddress));
      userBaseObj2.addProperty("toAddress", ByteArray.toHexString(toAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceFromPbft(
      String httpSolidityNode, byte[] fromAddress, byte[] toAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getdelegatedresource";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("fromAddress", ByteArray.toHexString(fromAddress));
      userBaseObj2.addProperty("toAddress", ByteArray.toHexString(toAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceAccountIndex(
      String httpNode, byte[] queryAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getdelegatedresourceaccountindex";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceAccountIndexFromSolidity(
      String httpSolidityNode, byte[] queryAddress) {
    HttpResponse response;
    try {
      String requestUrl =
          "http://" + httpSolidityNode + "/walletsolidity/getdelegatedresourceaccountindex";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceAccountIndexFromPbft(
      String httpSolidityNode, byte[] queryAddress) {
    HttpResponse response;
    try {
      String requestUrl =
          "http://" + httpSolidityNode + "/walletpbft/getdelegatedresourceaccountindex";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", ByteArray.toHexString(queryAddress));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse createConnect(String url) {
    HttpResponse response;
    try {
      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(url);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      logger.info(httppost.toString());
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse createConnect(String url, JsonObject requestBody) {
    HttpResponse response;
    try {
      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(url);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (requestBody != null) {
        StringEntity entity = new StringEntity(requestBody.toString(), Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }

      logger.info(httppost.toString());
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse createConnect(String url, JsonArray requestBody) {
    HttpResponse response;
    try {
      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(url);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (requestBody != null) {
        StringEntity entity = new StringEntity(requestBody.toString(), Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }

      logger.info(httppost.toString());
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse createConnectForGet(String url) {
    HttpResponse response;
    try {
      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      HttpGet httppost;
      httppost = new HttpGet(url);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");

      logger.info(httppost.toString());
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse createConnectForShieldTrc20(String url, JSONObject requestBody) {
    HttpResponse response;
    try {
      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httppost = new HttpPost(url);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (requestBody != null) {
        StringEntity entity = new StringEntity(requestBody.toString(), Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized Long createConnectForResponse(String url, JsonObject requestBody, Integer times) {
    HttpResponse response;
    try {

      Long start = 0L;
      Long end = 0L;
      Long duration = 0L;
      while (times-- > 0) {
        httpClient
            .getParams()
            .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
        httppost = new HttpPost(url);
        httppost.setHeader("Content-type", "application/json; charset=utf-8");
        httppost.setHeader("Connection", "Close");
        if (requestBody != null) {
          StringEntity entity = new StringEntity(requestBody.toString(), Charset.forName("UTF-8"));
          entity.setContentEncoding("UTF-8");
          entity.setContentType("application/json");
          httppost.setEntity(entity);
        }

        start = System.currentTimeMillis();
        response = httpClient.execute(httppost);
        /*        responseContent = HttpMethed.parseResponseContent(response);
        logger.info(responseContent.toString());*/
        end = System.currentTimeMillis();
        duration = duration + end - start;
        httppost.releaseConnection();
      }
      return duration;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return -1L;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse createConnect1(String url, JSONObject requestBody) {
    HttpResponse response;
    try {
      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout);
      httpClient
          .getParams()
          .setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, connectionTimeout * 10000);
      httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, soTimeout * 10000);
      httppost = new HttpPost(url);
      httppost.setHeader("Content-type", "application/json; charset=utf-8");
      httppost.setHeader("Connection", "Close");
      if (requestBody != null) {
        StringEntity entity = new StringEntity(requestBody.toString(), Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httppost.setEntity(entity);
      }
      response = httpClient.execute(httppost);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetissueList(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getassetissuelist";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized Long getAssetIssueValue(
      String httpNode, byte[] accountAddress, String assetIssueId) {
    HttpResponse response = HttpMethed.getAccount(httpNode, accountAddress);
    JSONObject responseContent = HttpMethed.parseResponseContent(response);
    JSONArray assetV2 = responseContent.getJSONArray("assetV2");
    if (assetV2 != null && assetV2.size() > 0) {
      for (int i = 0; i < assetV2.size(); i++) {
        String assetString = assetV2.get(i).toString();
        logger.info("assetString:" + assetString);
        String assetKey = HttpMethed.parseStringContent(assetString).getString("key");
        if (assetKey.equals(assetIssueId)) {
          return HttpMethed.parseStringContent(assetString).getLong("value");
        }
      }
    }
    return 0L;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueListFromSolidity(String httpSolidityNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getassetissuelist";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAssetIssueListFromPbft(String httpSolidityNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getassetissuelist";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getPaginatedAssetissueList(
      String httpNode, Integer offset, Integer limit) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/getpaginatedassetissuelist";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("offset", offset);
      userBaseObj2.addProperty("limit", limit);
      userBaseObj2.addProperty("visible", "true");
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getPaginatedAssetissueListFromSolidity(
      String httpSolidityNode, Integer offset, Integer limit) {
    HttpResponse response;
    try {
      String requestUrl =
          "http://" + httpSolidityNode + "/walletsolidity/getpaginatedassetissuelist";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("offset", offset);
      userBaseObj2.addProperty("limit", limit);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getPaginatedAssetissueListFromPbft(
      String httpSolidityNode, Integer offset, Integer limit) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getpaginatedassetissuelist";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("offset", offset);
      userBaseObj2.addProperty("limit", limit);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getPaginatedProposalList(
      String httpNode, Integer offset, Integer limit) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getpaginatedproposallist";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("offset", offset);
      userBaseObj2.addProperty("limit", limit);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getPaginatedExchangeList(
      String httpNode, Integer offset, Integer limit) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getpaginatedexchangelist";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("offset", offset);
      userBaseObj2.addProperty("limit", limit);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse updateSetting(
      String httpNode,
      byte[] ownerAddress,
      String contractAddress,
      Integer consumeUserResourcePercent,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/updatesetting";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("contract_address", contractAddress);
      userBaseObj2.addProperty("consume_user_resource_percent", consumeUserResourcePercent);
      logger.info(userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse updateEnergyLimit(
      String httpNode,
      byte[] ownerAddress,
      String contractAddress,
      Integer originEnergyLimit,
      String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/updateenergylimit";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("contract_address", contractAddress);
      userBaseObj2.addProperty("origin_energy_limit", originEnergyLimit);
      logger.info(userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      logger.info(transactionString);
      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse createAddress(String httpNode, String value) {

    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createaddress";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", str2hex(value));
      response = createConnect(requestUrl, userBaseObj2);
      logger.info(userBaseObj2.toString());
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse generateAddress(String httpNode) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/generateaddress";
      JsonObject userBaseObj2 = new JsonObject();
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionCountByBlocknum(String httpNode, long blocknum) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/gettransactioncountbyblocknum";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("num", blocknum);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse validateAddress(String httpNode, String address) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/validateaddress";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", address);
      response = createConnect(requestUrl, userBaseObj2);
      logger.info(userBaseObj2.toString());
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse easyTransfer(
      String httpNode, String value, byte[] toAddress, Long amount) {
    HttpResponse response;
    String transactionString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/easytransfer";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("toAddress", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("passPhrase", str2hex(value));
      userBaseObj2.addProperty("amount", amount);
      response = createConnect(requestUrl, userBaseObj2);
      logger.info(userBaseObj2.toString());
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse easyTransferByPrivate(
      String httpNode, String privateKey, byte[] toAddress, Long amount) {
    HttpResponse response;
    String transactionString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/easytransferbyprivate";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("privateKey", privateKey);
      userBaseObj2.addProperty("toAddress", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("amount", amount);
      response = createConnect(requestUrl, userBaseObj2);
      logger.info(userBaseObj2.toString());
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse easyTransferAsset(
      String httpNode, String value, byte[] toAddress, Long amount, String assetId) {
    HttpResponse response;
    String transactionString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/easytransferasset";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("toAddress", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("passPhrase", str2hex(value));
      userBaseObj2.addProperty("amount", amount);
      userBaseObj2.addProperty("assetId", assetId);
      response = createConnect(requestUrl, userBaseObj2);
      logger.info(userBaseObj2.toString());
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse easyTransferAssetByPrivate(
      String httpNode, String privateKey, byte[] toAddress, Long amount, String assetId) {
    HttpResponse response;
    String transactionString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/easytransferassetbyprivate";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("privateKey", privateKey);
      userBaseObj2.addProperty("toAddress", ByteArray.toHexString(toAddress));
      userBaseObj2.addProperty("amount", amount);
      userBaseObj2.addProperty("assetId", assetId);
      response = createConnect(requestUrl, userBaseObj2);
      logger.info(userBaseObj2.toString());
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getContractInfo(String httpNode, String contractAddress) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getcontractinfo";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", contractAddress);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized void disConnect() {
    httppost.releaseConnection();
  }

  /** constructor. */
  public static synchronized JSONObject parseResponseContent(HttpResponse response) {
    try {
      String result = EntityUtils.toString(response.getEntity());
      StringEntity entity = new StringEntity(result, Charset.forName("UTF-8"));
      response.setEntity(entity);
      JSONObject obj = JSONObject.parseObject(result);
      return obj;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /** constructor. */
  public static synchronized List<JSONObject> parseResponseContentArray(HttpResponse response) {
    try {
      String result = EntityUtils.toString(response.getEntity());
      StringEntity entity = new StringEntity(result, Charset.forName("UTF-8"));
      response.setEntity(entity);
      List<JSONObject> list = new ArrayList<JSONObject>();
      JSONArray objects = JSONArray.parseArray(result);
      for (int i = 0; i < objects.size(); i++) {
        list.add(objects.getJSONObject(i));
      }
      return list;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /** constructor. */
  public static synchronized JSONObject parseStringContent(String content) {
    try {
      JSONObject obj = JSONObject.parseObject(content);
      return obj;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /** constructor. */
  public static synchronized void printJsonContent(JSONObject responseContent) {
    logger.info("----------------------------Print JSON Start---------------------------");
    for (String str : responseContent.keySet()) {
      logger.info(str + ":" + responseContent.get(str));
    }
    logger.info("JSON content size are: " + responseContent.size());
    logger.info("----------------------------Print JSON End-----------------------------");
  }

  /** constructor. */
  public static synchronized String str2hex(String str) {
    char[] chars = "0123456789ABCDEF".toCharArray();
    StringBuilder sb = new StringBuilder();
    byte[] bs = str.getBytes();
    int bit;
    for (int i = 0; i < bs.length; i++) {
      bit = (bs[i] & 0x0f0) >> 4;
      sb.append(chars[bit]);
      bit = bs[i] & 0x0f;
      sb.append(chars[bit]);
      // sb.append(' ');
    }
    return sb.toString().trim();
  }

  /** constructor. */
  public static synchronized HttpResponse clearABiGetTxid(
      String httpNode, byte[] ownerAddress, String contractAddress, String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/clearabi";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("contract_address", contractAddress);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);

      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);

      logger.info(transactionSignString);
      response = broadcastTransaction(httpNode, transactionSignString);

    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized Optional<ShieldAddressInfo> generateShieldAddress(String httpnode) {
    HttpResponse response;
    JSONObject responseContent;
    ShieldAddressInfo addressInfo = new ShieldAddressInfo();
    String sk;
    String d;
    String ask;
    String nsk;
    String ovk;
    String ak;
    String nk;
    String ivk;
    String pkD;
    try {
      response = HttpMethed.getSpendingKey(httpnode);
      responseContent = HttpMethed.parseResponseContent(response);
      sk = responseContent.getString("value");

      response = HttpMethed.getDiversifier(httpnode);
      responseContent = HttpMethed.parseResponseContent(response);
      d = responseContent.getString("d");

      response = HttpMethed.getExpandedSpendingKey(httpnode, sk);
      responseContent = HttpMethed.parseResponseContent(response);
      ask = responseContent.getString("ask");
      nsk = responseContent.getString("nsk");
      ovk = responseContent.getString("ovk");

      response = HttpMethed.getAkFromAsk(httpnode, ask);
      responseContent = HttpMethed.parseResponseContent(response);
      ak = responseContent.getString("value");

      response = HttpMethed.getNkFromNsk(httpnode, nsk);
      responseContent = HttpMethed.parseResponseContent(response);
      nk = responseContent.getString("value");

      response = HttpMethed.getIncomingViewingKey(httpnode, ak, nk);
      responseContent = HttpMethed.parseResponseContent(response);
      ivk = responseContent.getString("ivk");

      response = HttpMethed.getZenPaymentAddress(httpnode, ivk, d);
      responseContent = HttpMethed.parseResponseContent(response);
      pkD = responseContent.getString("pkD");

      addressInfo.setSk(ByteArray.fromHexString(sk));
      addressInfo.setD(new DiversifierT(ByteArray.fromHexString(d)));
      addressInfo.setIvk(ByteArray.fromHexString(ivk));
      addressInfo.setOvk(ByteArray.fromHexString(ovk));
      addressInfo.setPkD(ByteArray.fromHexString(pkD));
      logger.info("sk:" + sk);

      if (addressInfo.validateCheck()) {
        return Optional.of(addressInfo);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }

  /** constructor. */
  public static synchronized List<GrpcAPI.Note> addShieldOutputList(
      String httpNode,
      List<GrpcAPI.Note> shieldOutList,
      String shieldToAddress,
      String toAmountString,
      String menoString) {
    HttpResponse response;
    JSONObject responseContent;
    String shieldAddress = shieldToAddress;
    String amountString = toAmountString;
    if (menoString.equals("null")) {
      menoString = "";
    }
    long shieldAmount = 0;
    if (!StringUtil.isNullOrEmpty(amountString)) {
      shieldAmount = Long.valueOf(amountString);
    }

    GrpcAPI.Note.Builder noteBuild = GrpcAPI.Note.newBuilder();
    noteBuild.setPaymentAddress(shieldAddress);
    noteBuild.setPaymentAddress(shieldAddress);
    noteBuild.setValue(shieldAmount);

    response = HttpMethed.getRcm(httpNode);
    responseContent = HttpMethed.parseResponseContent(response);
    String rcm = responseContent.getString("value");

    noteBuild.setRcm(ByteString.copyFrom(rcm.getBytes()));
    noteBuild.setMemo(ByteString.copyFrom(menoString.getBytes()));
    shieldOutList.add(noteBuild.build());
    return shieldOutList;
  }

  /** constructor. */
  public static synchronized HttpResponse getSpendingKey(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getspendingkey";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getDiversifier(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getdiversifier";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getRcm(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getrcm";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getExpandedSpendingKey(String httpNode, String spendingKey) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getexpandedspendingkey";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", spendingKey);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getAkFromAsk(String httpNode, String ask) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getakfromask";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", ask);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getNkFromNsk(String httpNode, String nsk) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getnkfromnsk";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", nsk);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getIncomingViewingKey(String httpNode, String ak, String nk) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getincomingviewingkey";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("ak", ak);
      userBaseObj2.addProperty("nk", nk);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getNewShieldedAddress(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getnewshieldedaddress";
      response = createConnect(requestUrl);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getZenPaymentAddress(String httpNode, String ivk, String d) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getzenpaymentaddress";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("ivk", ivk);
      userBaseObj2.addProperty("d", d);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMerkleTreeVoucherInfo(
      String httpNode, String hash, Integer index, int blockNum) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getmerkletreevoucherinfo";
      JSONObjectWarp jsonObjectWarp = new JSONObjectWarp();
      jsonObjectWarp
          .put(
              "out_points",
              Lists.newArrayList(new JSONObjectWarp().put("hash", hash).put("index", index)))
          .put("block_num", blockNum);
      String jsonStr = jsonObjectWarp.toJSONString();
      JsonObject jsonObj = new JsonParser().parse(jsonStr).getAsJsonObject();
      logger.info("jsonObj:" + jsonObj.toString());
      response = createConnect(requestUrl, jsonObj);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMerkleTreeVoucherInfoFromSolidity(
      String httpSolidityNode, String hash, Integer index, int blockNum) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getmerkletreevoucherinfo";
      JSONObjectWarp jsonObjectWarp = new JSONObjectWarp();
      jsonObjectWarp
          .put(
              "out_points",
              Lists.newArrayList(new JSONObjectWarp().put("hash", hash).put("index", index)))
          .put("block_num", blockNum);
      String jsonStr = jsonObjectWarp.toJSONString();
      JsonObject jsonObj = new JsonParser().parse(jsonStr).getAsJsonObject();
      logger.info("jsonObj:" + jsonObj.toString());
      response = createConnect(requestUrl, jsonObj);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMerkleTreeVoucherInfoFromPbft(
      String httpSolidityNode, String hash, Integer index, int blockNum) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getmerkletreevoucherinfo";
      JSONObjectWarp jsonObjectWarp = new JSONObjectWarp();
      jsonObjectWarp
          .put(
              "out_points",
              Lists.newArrayList(new JSONObjectWarp().put("hash", hash).put("index", index)))
          .put("block_num", blockNum);
      String jsonStr = jsonObjectWarp.toJSONString();
      JsonObject jsonObj = new JsonParser().parse(jsonStr).getAsJsonObject();
      logger.info("jsonObj:" + jsonObj.toString());
      response = createConnect(requestUrl, jsonObj);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse sendShieldCoin(
      String httpNode,
      byte[] publicZenTokenOwnerAddress,
      long fromAmount,
      ShieldAddressInfo shieldAddressInfo,
      ShieldNoteInfo noteTx,
      List<GrpcAPI.Note> shieldOutputList,
      byte[] publicZenTokenToAddress,
      long toAmount,
      String zenTokenOwnerKey) {
    JSONObject responseContent;
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/createshieldedtransaction";

      Map<String, Object> map = new HashMap<String, Object>();
      if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
        map.put("transparent_from_address", ByteArray.toHexString(publicZenTokenOwnerAddress));
        map.put("from_amount", fromAmount);
      }
      if (shieldAddressInfo != null) {
        HttpResponse expandedSpendingKey =
            HttpMethed.getExpandedSpendingKey(
                httpNode, ByteArray.toHexString(shieldAddressInfo.getSk()));
        responseContent = HttpMethed.parseResponseContent(expandedSpendingKey);
        HttpMethed.printJsonContent(responseContent);
        String ovk = responseContent.getString("ovk");
        map.put("ask", responseContent.getString("ask"));
        map.put("nsk", responseContent.getString("nsk"));
        map.put("ovk", ovk);

        response =
            HttpMethed.getMerkleTreeVoucherInfo(httpNode, noteTx.getTrxId(), noteTx.getIndex(), 1);
        responseContent = HttpMethed.parseResponseContent(response);
        JSONArray vouchers = responseContent.getJSONArray("vouchers");
        JSONArray paths = responseContent.getJSONArray("paths");

        List shieldedSpends =
            Lists.newArrayList(
                new JSONObjectWarp()
                    .put(
                        "note",
                        new JSONObjectWarp()
                            .put("value", noteTx.getValue())
                            .put("payment_address", noteTx.getPaymentAddress())
                            .put("rcm", ByteArray.toHexString(noteTx.getR()))
                            .put("memo", ByteArray.toHexString(noteTx.getMemo())))
                    .put(
                        "alpha",
                        ByteArray.toHexString(
                            stest.tron.wallet.common.client.utils.zen.note.Note.generateR()))
                    .put("voucher", Lists.newArrayList(vouchers))
                    .put("path", Lists.newArrayList(paths)));

        map.put("shielded_spends", shieldedSpends);
      } else {
        map.put("ovk", "030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
      }

      if (!ByteUtil.isNullOrZeroArray(publicZenTokenToAddress)) {
        map.put("transparent_to_address", ByteArray.toHexString(publicZenTokenToAddress));
        map.put("to_amount", toAmount);
      }

      if (shieldOutputList.size() > 0) {
        ArrayList<Object> noteList = new ArrayList<>();
        for (int i = 0; i < shieldOutputList.size(); ++i) {

          Map<String, Object> noteInfo = new HashMap<String, Object>();
          noteInfo.put("value", shieldOutputList.get(i).getValue());
          noteInfo.put("payment_address", shieldOutputList.get(i).getPaymentAddress());
          noteInfo.put("rcm", shieldOutputList.get(i).getRcm().toStringUtf8());
          noteInfo.put(
              "memo",
              ByteArray.toHexString(shieldOutputList.get(i).getMemo().toStringUtf8().getBytes()));
          final Map<String, Object> note = new HashMap<String, Object>();
          note.put("note", noteInfo);
          noteList.add(note);
        }
        map.put("shielded_receives", noteList);
      }

      String jsonStr = new Gson().toJson(map);
      JsonObject jsonObj = new JsonParser().parse(jsonStr).getAsJsonObject();
      logger.info("jsonObj:" + jsonObj.toString());
      response = createConnect(requestUrl, jsonObj);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info(transactionString);
      if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
        transactionSignString = gettransactionsign(httpNode, transactionString, zenTokenOwnerKey);
        logger.info(transactionSignString);
        response = broadcastTransaction(httpNode, transactionSignString);
      } else {
        response = broadcastTransaction(httpNode, transactionString);
      }
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized Boolean getSpendResult(
      String httpNode, ShieldAddressInfo shieldAddressInfo, ShieldNoteInfo noteTx) {
    JSONObject responseContent;
    HttpResponse response;

    try {
      final String requestUrl = "http://" + httpNode + "/wallet/isspend";

      response =
          HttpMethed.getExpandedSpendingKey(httpNode, ByteArray.toHexString(shieldAddressInfo.sk));
      responseContent = HttpMethed.parseResponseContent(response);
      String ask = responseContent.getString("ask");
      response = HttpMethed.getAkFromAsk(httpNode, ask);
      responseContent = HttpMethed.parseResponseContent(response);
      JSONObjectWarp jsonObjectWarp = new JSONObjectWarp();
      jsonObjectWarp.put("ak", responseContent.getString("value"));

      String nsk = responseContent.getString("nsk");
      response = HttpMethed.getNkFromNsk(httpNode, nsk);
      responseContent = HttpMethed.parseResponseContent(response);
      jsonObjectWarp.put("nk", responseContent.getString("value"));

      jsonObjectWarp
          .put(
              "note",
              new JSONObjectWarp()
                  .put("value", noteTx.getValue())
                  .put("payment_address", noteTx.getPaymentAddress())
                  .put("rcm", ByteArray.toHexString(noteTx.getR()))
                  .put("memo", ByteArray.toHexString(noteTx.getMemo())))
          .put("txid", noteTx.getTrxId());

      String jsonStr = jsonObjectWarp.toJSONString();
      JsonObject jsonObj = new JsonParser().parse(jsonStr).getAsJsonObject();
      logger.info("jsonObj:" + jsonObj.toString());
      response = createConnect(requestUrl, jsonObj);

      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String jsonString = responseContent.toJSONString();
      if (jsonString.contains("result")
          && (responseContent.getString("result").equals("true")
              && responseContent.getString("message").equals("Input note has been spent"))) {
        return Boolean.TRUE;
      } else {
        return Boolean.FALSE;
      }
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized Boolean getSpendResultFromSolidity(
      String httpNode,
      String httpSolidityNode,
      ShieldAddressInfo shieldAddressInfo,
      ShieldNoteInfo noteTx) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/isspend";

      response =
          HttpMethed.getExpandedSpendingKey(httpNode, ByteArray.toHexString(shieldAddressInfo.sk));
      responseContent = HttpMethed.parseResponseContent(response);
      String ask = responseContent.getString("ask");
      response = HttpMethed.getAkFromAsk(httpNode, ask);
      responseContent = HttpMethed.parseResponseContent(response);
      JSONObjectWarp jsonObjectWarp = new JSONObjectWarp();
      jsonObjectWarp.put("ak", responseContent.getString("value"));

      String nsk = responseContent.getString("nsk");
      response = HttpMethed.getNkFromNsk(httpNode, nsk);
      responseContent = HttpMethed.parseResponseContent(response);
      jsonObjectWarp.put("nk", responseContent.getString("value"));

      jsonObjectWarp
          .put(
              "note",
              new JSONObjectWarp()
                  .put("value", noteTx.getValue())
                  .put("payment_address", noteTx.getPaymentAddress())
                  .put("rcm", ByteArray.toHexString(noteTx.getR()))
                  .put("memo", ByteArray.toHexString(noteTx.getMemo())))
          .put("txid", noteTx.getTrxId());

      String jsonStr = jsonObjectWarp.toJSONString();
      JsonObject jsonObj = new JsonParser().parse(jsonStr).getAsJsonObject();
      logger.info("jsonObj:" + jsonObj.toString());
      response = createConnect(requestUrl, jsonObj);
      responseContent = HttpMethed.parseResponseContent(response);

      HttpMethed.printJsonContent(responseContent);
      String jsonString = responseContent.toJSONString();
      if (jsonString.contains("result")
          && (responseContent.getString("result").equals("true")
              && responseContent.getString("message").equals("Input note has been spent"))) {
        return Boolean.TRUE;
      } else {
        return Boolean.FALSE;
      }
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized Boolean getSpendResultFromPbft(
      String httpNode,
      String httpSolidityNode,
      ShieldAddressInfo shieldAddressInfo,
      ShieldNoteInfo noteTx) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpSolidityNode + "/walletpbft/isspend";
      final JSONObjectWarp jsonObjectWarp = new JSONObjectWarp();

      response =
          HttpMethed.getExpandedSpendingKey(httpNode, ByteArray.toHexString(shieldAddressInfo.sk));
      responseContent = HttpMethed.parseResponseContent(response);
      String ask = responseContent.getString("ask");
      final String nsk = responseContent.getString("nsk");
      response = HttpMethed.getAkFromAsk(httpNode, ask);
      responseContent = HttpMethed.parseResponseContent(response);
      jsonObjectWarp.put("ak", responseContent.getString("value"));
      response = HttpMethed.getNkFromNsk(httpNode, nsk);
      responseContent = HttpMethed.parseResponseContent(response);
      jsonObjectWarp.put("nk", responseContent.getString("value"));
      jsonObjectWarp
          .put(
              "note",
              new JSONObjectWarp()
                  .put("value", noteTx.getValue())
                  .put("payment_address", noteTx.getPaymentAddress())
                  .put("rcm", ByteArray.toHexString(noteTx.getR()))
                  .put("memo", ByteArray.toHexString(noteTx.getMemo())))
          .put("txid", noteTx.getTrxId());
      String jsonStr = jsonObjectWarp.toJSONString();
      JsonObject jsonObj = new JsonParser().parse(jsonStr).getAsJsonObject();
      logger.info("jsonObj:" + jsonObj.toString());
      response = createConnect(requestUrl, jsonObj);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String jsonString = responseContent.toJSONString();
      if (jsonString.contains("result")
          && (responseContent.getString("result").equals("true")
              && responseContent.getString("message").equals("Input note has been spent"))) {
        return Boolean.TRUE;
      } else {
        return Boolean.FALSE;
      }
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized List<ShieldNoteInfo> scanNoteByOvkFromPbft(
      String httpSolidityNode, ShieldAddressInfo shieldAddressInfo) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      response = HttpMethed.getNowBlockFromPbft(httpSolidityNode);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String blockHeaderString = responseContent.getString("block_header");
      String rawDataString =
          HttpMethed.parseStringContent(blockHeaderString).get("raw_data").toString();
      Integer currentBlockNum = HttpMethed.parseStringContent(rawDataString).getInteger("number");
      Integer startBlockNum = 0;
      if (currentBlockNum > 100) {
        startBlockNum = currentBlockNum - 100;
      }

      final String requestUrl = "http://" + httpSolidityNode + "/walletpbft/scannotebyovk";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("start_block_index", startBlockNum);
      userBaseObj2.addProperty("end_block_index", currentBlockNum);
      userBaseObj2.addProperty("ovk", ByteArray.toHexString(shieldAddressInfo.getOvk()));
      logger.info("userBaseObj2:" + userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      List<ShieldNoteInfo> shieldNoteInfoList = new ArrayList<>();
      JSONArray jsonArray = responseContent.getJSONArray("noteTxs");
      for (int i = 0; i < jsonArray.size(); i++) {
        ShieldNoteInfo noteTx = new ShieldNoteInfo();
        String noteTxs = jsonArray.get(0).toString();
        String noteString = HttpMethed.parseStringContent(noteTxs).getString("note");
        noteTx.setValue(HttpMethed.parseStringContent(noteString).getLong("value"));
        noteTx.setPaymentAddress(
            HttpMethed.parseStringContent(noteString).getString("payment_address"));
        noteTx.setR(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("rcm")));
        noteTx.setMemo(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("memo")));
        noteTx.setTrxId(HttpMethed.parseStringContent(noteTxs).getString("txid"));
        noteTx.setIndex(HttpMethed.parseStringContent(noteTxs).getIntValue("index"));
        shieldNoteInfoList.add(noteTx);
      }
      return shieldNoteInfoList;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized List<ShieldNoteInfo> scanNoteByIvk(
      String httpNode, ShieldAddressInfo shieldAddressInfo) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      response = HttpMethed.getNowBlock(httpNode);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String blockHeaderString = responseContent.getString("block_header");
      String rawDataString =
          HttpMethed.parseStringContent(blockHeaderString).get("raw_data").toString();
      Integer currentBlockNum = HttpMethed.parseStringContent(rawDataString).getInteger("number");
      Integer startBlockNum = 0;
      if (currentBlockNum > 100) {
        startBlockNum = currentBlockNum - 100;
      }

      final String requestUrl = "http://" + httpNode + "/wallet/scannotebyivk";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("start_block_index", startBlockNum);
      userBaseObj2.addProperty("end_block_index", currentBlockNum);
      userBaseObj2.addProperty("ivk", ByteArray.toHexString(shieldAddressInfo.ivk));
      logger.info("userBaseObj2:" + userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);

      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      List<ShieldNoteInfo> shieldNoteInfoList = new ArrayList<>();
      JSONArray jsonArray = responseContent.getJSONArray("noteTxs");
      if (jsonArray != null) {
        for (int i = 0; i < jsonArray.size(); i++) {
          ShieldNoteInfo noteTx = new ShieldNoteInfo();
          String noteTxs = jsonArray.get(i).toString();
          String noteString = HttpMethed.parseStringContent(noteTxs).getString("note");
          noteTx.setValue(HttpMethed.parseStringContent(noteString).getLong("value"));
          noteTx.setPaymentAddress(
              HttpMethed.parseStringContent(noteString).getString("payment_address"));
          noteTx.setR(
              ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("rcm")));
          noteTx.setMemo(
              ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("memo")));
          noteTx.setTrxId(HttpMethed.parseStringContent(noteTxs).getString("txid"));
          noteTx.setIndex(HttpMethed.parseStringContent(noteTxs).getIntValue("index"));
          shieldNoteInfoList.add(noteTx);
        }
      }
      return shieldNoteInfoList;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized List<ShieldNoteInfo> scanNoteByIvkFromSolidity(
      String httpSolidityNode, ShieldAddressInfo shieldAddressInfo) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      response = HttpMethed.getNowBlockFromSolidity(httpSolidityNode);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String blockHeaderString = responseContent.getString("block_header");
      String rawDataString =
          HttpMethed.parseStringContent(blockHeaderString).get("raw_data").toString();
      Integer currentBlockNum = HttpMethed.parseStringContent(rawDataString).getInteger("number");
      Integer startBlockNum = 0;
      if (currentBlockNum > 100) {
        startBlockNum = currentBlockNum - 100;
      }

      final String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/scannotebyivk";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("start_block_index", startBlockNum);
      userBaseObj2.addProperty("end_block_index", currentBlockNum);
      userBaseObj2.addProperty("ivk", ByteArray.toHexString(shieldAddressInfo.ivk));
      logger.info("userBaseObj2:" + userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);

      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      List<ShieldNoteInfo> shieldNoteInfoList = new ArrayList<>();
      JSONArray jsonArray = responseContent.getJSONArray("noteTxs");
      for (int i = 0; i < jsonArray.size(); i++) {
        ShieldNoteInfo noteTx = new ShieldNoteInfo();
        String noteTxs = jsonArray.get(i).toString();
        String noteString = HttpMethed.parseStringContent(noteTxs).getString("note");
        noteTx.setValue(HttpMethed.parseStringContent(noteString).getLong("value"));
        noteTx.setPaymentAddress(
            HttpMethed.parseStringContent(noteString).getString("payment_address"));
        noteTx.setR(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("rcm")));
        noteTx.setMemo(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("memo")));
        noteTx.setTrxId(HttpMethed.parseStringContent(noteTxs).getString("txid"));
        noteTx.setIndex(HttpMethed.parseStringContent(noteTxs).getIntValue("index"));
        shieldNoteInfoList.add(noteTx);
      }
      return shieldNoteInfoList;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized List<ShieldNoteInfo> scanNoteByIvkFromPbft(
      String httpSolidityNode, ShieldAddressInfo shieldAddressInfo) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      response = HttpMethed.getNowBlockFromPbft(httpSolidityNode);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String blockHeaderString = responseContent.getString("block_header");
      String rawDataString =
          HttpMethed.parseStringContent(blockHeaderString).get("raw_data").toString();
      Integer currentBlockNum = HttpMethed.parseStringContent(rawDataString).getInteger("number");
      Integer startBlockNum = 0;
      if (currentBlockNum > 100) {
        startBlockNum = currentBlockNum - 100;
      }

      final String requestUrl = "http://" + httpSolidityNode + "/walletpbft/scannotebyivk";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("start_block_index", startBlockNum);
      userBaseObj2.addProperty("end_block_index", currentBlockNum);
      userBaseObj2.addProperty("ivk", ByteArray.toHexString(shieldAddressInfo.ivk));
      logger.info("userBaseObj2:" + userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      List<ShieldNoteInfo> shieldNoteInfoList = new ArrayList<>();
      JSONArray jsonArray = responseContent.getJSONArray("noteTxs");
      for (int i = 0; i < jsonArray.size(); i++) {
        ShieldNoteInfo noteTx = new ShieldNoteInfo();
        String noteTxs = jsonArray.get(i).toString();
        String noteString = HttpMethed.parseStringContent(noteTxs).getString("note");
        noteTx.setValue(HttpMethed.parseStringContent(noteString).getLong("value"));
        noteTx.setPaymentAddress(
            HttpMethed.parseStringContent(noteString).getString("payment_address"));
        noteTx.setR(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("rcm")));
        noteTx.setMemo(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("memo")));
        noteTx.setTrxId(HttpMethed.parseStringContent(noteTxs).getString("txid"));
        noteTx.setIndex(HttpMethed.parseStringContent(noteTxs).getIntValue("index"));
        shieldNoteInfoList.add(noteTx);
      }
      return shieldNoteInfoList;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized List<ShieldNoteInfo> scanAndMarkNoteByIvk(
      String httpNode, ShieldAddressInfo shieldAddressInfo) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      response =
          HttpMethed.getExpandedSpendingKey(httpNode, ByteArray.toHexString(shieldAddressInfo.sk));
      responseContent = HttpMethed.parseResponseContent(response);
      String ask = responseContent.getString("ask");
      String nsk = responseContent.getString("nsk");
      response = HttpMethed.getAkFromAsk(httpNode, ask);
      responseContent = HttpMethed.parseResponseContent(response);
      final String ak = responseContent.getString("value");

      response = HttpMethed.getNkFromNsk(httpNode, nsk);
      responseContent = HttpMethed.parseResponseContent(response);
      final String nk = responseContent.getString("value");

      response = HttpMethed.getNowBlock(httpNode);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String blockHeaderString = responseContent.getString("block_header");
      String rawDataString =
          HttpMethed.parseStringContent(blockHeaderString).get("raw_data").toString();
      Integer currentBlockNum = HttpMethed.parseStringContent(rawDataString).getInteger("number");
      Integer startBlockNum = 0;
      if (currentBlockNum > 100) {
        startBlockNum = currentBlockNum - 100;
      }

      final String requestUrl = "http://" + httpNode + "/wallet/scanandmarknotebyivk";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("start_block_index", startBlockNum);
      userBaseObj2.addProperty("end_block_index", currentBlockNum);
      userBaseObj2.addProperty("ivk", ByteArray.toHexString(shieldAddressInfo.ivk));
      userBaseObj2.addProperty("ak", ak);
      userBaseObj2.addProperty("nk", nk);
      logger.info("userBaseObj2:" + userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);

      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      List<ShieldNoteInfo> shieldNoteInfoList = new ArrayList<>();
      JSONArray jsonArray = responseContent.getJSONArray("noteTxs");
      logger.info("jsonArray:" + jsonArray.toJSONString());
      for (int i = 0; i < jsonArray.size(); i++) {
        ShieldNoteInfo noteTx = new ShieldNoteInfo();
        String noteTxs = jsonArray.get(i).toString();
        String noteString = HttpMethed.parseStringContent(noteTxs).getString("note");
        noteTx.setValue(HttpMethed.parseStringContent(noteString).getLong("value"));
        noteTx.setPaymentAddress(
            HttpMethed.parseStringContent(noteString).getString("payment_address"));
        noteTx.setR(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("rcm")));
        noteTx.setMemo(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("memo")));
        noteTx.setTrxId(HttpMethed.parseStringContent(noteTxs).getString("txid"));
        noteTx.setIsSpend(HttpMethed.parseStringContent(noteTxs).getBoolean("is_spend"));
        noteTx.setIndex(HttpMethed.parseStringContent(noteTxs).getIntValue("index"));
        shieldNoteInfoList.add(noteTx);
      }
      return shieldNoteInfoList;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized List<ShieldNoteInfo> scanAndMarkNoteByIvkFromSolidity(
      String httpNode, String httpSolidityNode, ShieldAddressInfo shieldAddressInfo) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      response =
          HttpMethed.getExpandedSpendingKey(httpNode, ByteArray.toHexString(shieldAddressInfo.sk));
      responseContent = HttpMethed.parseResponseContent(response);
      String ask = responseContent.getString("ask");
      String nsk = responseContent.getString("nsk");
      response = HttpMethed.getAkFromAsk(httpNode, ask);
      responseContent = HttpMethed.parseResponseContent(response);
      final String ak = responseContent.getString("value");

      response = HttpMethed.getNkFromNsk(httpNode, nsk);
      responseContent = HttpMethed.parseResponseContent(response);
      final String nk = responseContent.getString("value");

      response = HttpMethed.getNowBlock(httpNode);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String blockHeaderString = responseContent.getString("block_header");
      String rawDataString =
          HttpMethed.parseStringContent(blockHeaderString).get("raw_data").toString();
      Integer currentBlockNum = HttpMethed.parseStringContent(rawDataString).getInteger("number");
      Integer startBlockNum = 0;
      if (currentBlockNum > 100) {
        startBlockNum = currentBlockNum - 100;
      }

      final String requestUrl =
          "http://" + httpSolidityNode + "/walletsolidity/scanandmarknotebyivk";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("start_block_index", startBlockNum);
      userBaseObj2.addProperty("end_block_index", currentBlockNum);
      userBaseObj2.addProperty("ivk", ByteArray.toHexString(shieldAddressInfo.ivk));
      userBaseObj2.addProperty("ak", ak);
      userBaseObj2.addProperty("nk", nk);
      logger.info("userBaseObj2:" + userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);

      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      List<ShieldNoteInfo> shieldNoteInfoList = new ArrayList<>();
      JSONArray jsonArray = responseContent.getJSONArray("noteTxs");
      logger.info("jsonArray:" + jsonArray.toJSONString());
      for (int i = 0; i < jsonArray.size(); i++) {
        ShieldNoteInfo noteTx = new ShieldNoteInfo();
        String noteTxs = jsonArray.get(i).toString();
        String noteString = HttpMethed.parseStringContent(noteTxs).getString("note");
        noteTx.setValue(HttpMethed.parseStringContent(noteString).getLong("value"));
        noteTx.setPaymentAddress(
            HttpMethed.parseStringContent(noteString).getString("payment_address"));
        noteTx.setR(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("rcm")));
        noteTx.setMemo(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("memo")));
        noteTx.setTrxId(HttpMethed.parseStringContent(noteTxs).getString("txid"));
        noteTx.setIsSpend(HttpMethed.parseStringContent(noteTxs).getBoolean("is_spend"));
        noteTx.setIndex(HttpMethed.parseStringContent(noteTxs).getIntValue("index"));
        shieldNoteInfoList.add(noteTx);
      }
      return shieldNoteInfoList;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized List<ShieldNoteInfo> scanAndMarkNoteByIvkFromPbft(
      String httpNode, String httpSolidityNode, ShieldAddressInfo shieldAddressInfo) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      response =
          HttpMethed.getExpandedSpendingKey(httpNode, ByteArray.toHexString(shieldAddressInfo.sk));
      responseContent = HttpMethed.parseResponseContent(response);
      String ask = responseContent.getString("ask");
      String nsk = responseContent.getString("nsk");
      response = HttpMethed.getAkFromAsk(httpNode, ask);
      responseContent = HttpMethed.parseResponseContent(response);
      final String ak = responseContent.getString("value");

      response = HttpMethed.getNkFromNsk(httpNode, nsk);
      responseContent = HttpMethed.parseResponseContent(response);
      final String nk = responseContent.getString("value");

      response = HttpMethed.getNowBlock(httpNode);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String blockHeaderString = responseContent.getString("block_header");
      String rawDataString =
          HttpMethed.parseStringContent(blockHeaderString).get("raw_data").toString();
      Integer currentBlockNum = HttpMethed.parseStringContent(rawDataString).getInteger("number");
      Integer startBlockNum = 0;
      if (currentBlockNum > 100) {
        startBlockNum = currentBlockNum - 100;
      }
      final String requestUrl = "http://" + httpSolidityNode + "/walletpbft/scanandmarknotebyivk";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("start_block_index", startBlockNum);
      userBaseObj2.addProperty("end_block_index", currentBlockNum);
      userBaseObj2.addProperty("ivk", ByteArray.toHexString(shieldAddressInfo.ivk));
      userBaseObj2.addProperty("ak", ak);
      userBaseObj2.addProperty("nk", nk);
      logger.info("userBaseObj2:" + userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      List<ShieldNoteInfo> shieldNoteInfoList = new ArrayList<>();
      JSONArray jsonArray = responseContent.getJSONArray("noteTxs");
      logger.info("jsonArray:" + jsonArray.toJSONString());
      for (int i = 0; i < jsonArray.size(); i++) {
        ShieldNoteInfo noteTx = new ShieldNoteInfo();
        String noteTxs = jsonArray.get(i).toString();
        String noteString = HttpMethed.parseStringContent(noteTxs).getString("note");
        noteTx.setValue(HttpMethed.parseStringContent(noteString).getLong("value"));
        noteTx.setPaymentAddress(
            HttpMethed.parseStringContent(noteString).getString("payment_address"));
        noteTx.setR(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("rcm")));
        noteTx.setMemo(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("memo")));
        noteTx.setTrxId(HttpMethed.parseStringContent(noteTxs).getString("txid"));
        noteTx.setIsSpend(HttpMethed.parseStringContent(noteTxs).getBoolean("is_spend"));
        noteTx.setIndex(HttpMethed.parseStringContent(noteTxs).getIntValue("index"));
        shieldNoteInfoList.add(noteTx);
      }
      return shieldNoteInfoList;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized List<ShieldNoteInfo> scanNoteByOvk(
      String httpNode, ShieldAddressInfo shieldAddressInfo) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      response = HttpMethed.getNowBlock(httpNode);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String blockHeaderString = responseContent.getString("block_header");
      String rawDataString =
          HttpMethed.parseStringContent(blockHeaderString).get("raw_data").toString();
      Integer currentBlockNum = HttpMethed.parseStringContent(rawDataString).getInteger("number");
      Integer startBlockNum = 0;
      if (currentBlockNum > 100) {
        startBlockNum = currentBlockNum - 100;
      }

      final String requestUrl = "http://" + httpNode + "/wallet/scannotebyovk";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("start_block_index", startBlockNum);
      userBaseObj2.addProperty("end_block_index", currentBlockNum);
      userBaseObj2.addProperty("ovk", ByteArray.toHexString(shieldAddressInfo.getOvk()));
      logger.info("userBaseObj2:" + userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);

      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      List<ShieldNoteInfo> shieldNoteInfoList = new ArrayList<>();
      JSONArray jsonArray = responseContent.getJSONArray("noteTxs");
      for (int i = 0; i < jsonArray.size(); i++) {
        ShieldNoteInfo noteTx = new ShieldNoteInfo();
        String noteTxs = jsonArray.get(i).toString();
        String noteString = HttpMethed.parseStringContent(noteTxs).getString("note");
        noteTx.setValue(HttpMethed.parseStringContent(noteString).getLong("value"));
        noteTx.setPaymentAddress(
            HttpMethed.parseStringContent(noteString).getString("payment_address"));
        noteTx.setR(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("rcm")));
        noteTx.setMemo(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("memo")));
        noteTx.setTrxId(HttpMethed.parseStringContent(noteTxs).getString("txid"));
        noteTx.setIndex(HttpMethed.parseStringContent(noteTxs).getIntValue("index"));
        shieldNoteInfoList.add(noteTx);
      }
      return shieldNoteInfoList;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized List<ShieldNoteInfo> scanNoteByOvkFromSolidity(
      String httpSolidityNode, ShieldAddressInfo shieldAddressInfo) {
    JSONObject responseContent;
    HttpResponse response;
    try {
      response = HttpMethed.getNowBlockFromSolidity(httpSolidityNode);
      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      String blockHeaderString = responseContent.getString("block_header");
      String rawDataString =
          HttpMethed.parseStringContent(blockHeaderString).get("raw_data").toString();
      Integer currentBlockNum = HttpMethed.parseStringContent(rawDataString).getInteger("number");
      Integer startBlockNum = 0;
      if (currentBlockNum > 100) {
        startBlockNum = currentBlockNum - 100;
      }

      final String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/scannotebyovk";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("start_block_index", startBlockNum);
      userBaseObj2.addProperty("end_block_index", currentBlockNum);
      userBaseObj2.addProperty("ovk", ByteArray.toHexString(shieldAddressInfo.getOvk()));
      logger.info("userBaseObj2:" + userBaseObj2.toString());
      response = createConnect(requestUrl, userBaseObj2);

      responseContent = HttpMethed.parseResponseContent(response);
      HttpMethed.printJsonContent(responseContent);
      List<ShieldNoteInfo> shieldNoteInfoList = new ArrayList<>();
      JSONArray jsonArray = responseContent.getJSONArray("noteTxs");
      for (int i = 0; i < jsonArray.size(); i++) {
        ShieldNoteInfo noteTx = new ShieldNoteInfo();
        String noteTxs = jsonArray.get(0).toString();
        String noteString = HttpMethed.parseStringContent(noteTxs).getString("note");
        noteTx.setValue(HttpMethed.parseStringContent(noteString).getLong("value"));
        noteTx.setPaymentAddress(
            HttpMethed.parseStringContent(noteString).getString("payment_address"));
        noteTx.setR(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("rcm")));
        noteTx.setMemo(
            ByteArray.fromHexString(HttpMethed.parseStringContent(noteString).getString("memo")));
        noteTx.setTrxId(HttpMethed.parseStringContent(noteTxs).getString("txid"));
        noteTx.setIndex(HttpMethed.parseStringContent(noteTxs).getIntValue("index"));
        shieldNoteInfoList.add(noteTx);
      }
      return shieldNoteInfoList;
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
  }

  /** constructor. */
  public static synchronized HttpResponse sendShieldCoinWithoutAsk(
      String httpNode,
      String httpSolidityNode,
      String httpPbftNode,
      byte[] publicZenTokenOwnerAddress,
      long fromAmount,
      ShieldAddressInfo shieldAddressInfo,
      ShieldNoteInfo noteTx,
      List<GrpcAPI.Note> shieldOutputList,
      byte[] publicZenTokenToAddress,
      long toAmount,
      String zenTokenOwnerKey) {
    JSONObject responseContent;
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl =
          "http://" + httpNode + "/wallet/createshieldedtransactionwithoutspendauthsig";

      Map<String, Object> map = new HashMap<String, Object>();
      if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
        map.put("transparent_from_address", ByteArray.toHexString(publicZenTokenOwnerAddress));
        map.put("from_amount", fromAmount);
      }
      String ask = "";
      byte[] alpha = null;
      if (shieldAddressInfo != null) {
        HttpResponse expandedSpendingKey =
            HttpMethed.getExpandedSpendingKey(
                httpNode, ByteArray.toHexString(shieldAddressInfo.getSk()));
        responseContent = HttpMethed.parseResponseContent(expandedSpendingKey);
        HttpMethed.printJsonContent(responseContent);
        map.put("nsk", responseContent.getString("nsk"));
        map.put("ovk", responseContent.getString("ovk"));
        ask = responseContent.getString("ask");
        response = HttpMethed.getAkFromAsk(httpNode, ask);
        responseContent = HttpMethed.parseResponseContent(response);
        HttpMethed.printJsonContent(responseContent);
        map.put("ak", responseContent.getString("value"));

        logger.info("noteTx.getTrxId():" + noteTx.getTrxId());
        response =
            HttpMethed.getMerkleTreeVoucherInfoFromSolidity(
                httpSolidityNode, noteTx.getTrxId(), noteTx.getIndex(), 1);
        responseContent = HttpMethed.parseResponseContent(response);
        HttpMethed.printJsonContent(responseContent);
        final JSONArray vouchers = responseContent.getJSONArray("vouchers");
        final JSONArray paths = responseContent.getJSONArray("paths");

        response =
            HttpMethed.getMerkleTreeVoucherInfoFromPbft(
                httpPbftNode, noteTx.getTrxId(), noteTx.getIndex(), 1);
        responseContent = HttpMethed.parseResponseContent(response);
        HttpMethed.printJsonContent(responseContent);
        JSONArray vouchersPbft = responseContent.getJSONArray("vouchers");
        JSONArray pathsPbft = responseContent.getJSONArray("paths");
        Assert.assertTrue(pathsPbft.equals(paths));
        alpha = stest.tron.wallet.common.client.utils.zen.note.Note.generateR();

        List shieldedSpends =
            Lists.newArrayList(
                new JSONObjectWarp()
                    .put(
                        "note",
                        new JSONObjectWarp()
                            .put("value", noteTx.getValue())
                            .put("payment_address", noteTx.getPaymentAddress())
                            .put("rcm", ByteArray.toHexString(noteTx.getR()))
                            .put("memo", ByteArray.toHexString(noteTx.getMemo())))
                    .put("alpha", ByteArray.toHexString(alpha))
                    .put("voucher", Lists.newArrayList(vouchers))
                    .put("path", Lists.newArrayList(paths)));

        map.put("shielded_spends", shieldedSpends);
      } else {
        map.put("ovk", "030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
      }

      if (!ByteUtil.isNullOrZeroArray(publicZenTokenToAddress)) {
        map.put("transparent_to_address", ByteArray.toHexString(publicZenTokenToAddress));
        map.put("to_amount", toAmount);
      }

      if (shieldOutputList.size() > 0) {
        ArrayList<Object> noteList = new ArrayList<>();
        for (int i = 0; i < shieldOutputList.size(); ++i) {
          Map<String, Object> noteInfo = new HashMap<String, Object>();
          noteInfo.put("value", shieldOutputList.get(i).getValue());
          noteInfo.put("payment_address", shieldOutputList.get(i).getPaymentAddress());
          noteInfo.put("rcm", shieldOutputList.get(i).getRcm().toStringUtf8());
          noteInfo.put(
              "memo",
              ByteArray.toHexString(shieldOutputList.get(i).getMemo().toStringUtf8().getBytes()));
          final Map<String, Object> note = new HashMap<String, Object>();
          note.put("note", noteInfo);
          noteList.add(note);
        }
        map.put("shielded_receives", noteList);
      }

      String jsonStr = new Gson().toJson(map);
      JsonObject jsonObj = new JsonParser().parse(jsonStr).getAsJsonObject();
      logger.info("jsonObj:" + jsonObj.toString());
      response = createConnect(requestUrl, jsonObj);
      transactionString = EntityUtils.toString(response.getEntity());
      logger.info("transactionString1:" + transactionString);

      if (shieldAddressInfo != null) {
        String getHashUrl = "http://" + httpNode + "/wallet/getshieldtransactionhash";
        JsonObject gethashObject = new JsonParser().parse(transactionString).getAsJsonObject();
        logger.info("gethashObject:" + gethashObject);
        response = createConnect(getHashUrl, gethashObject);
        responseContent = HttpMethed.parseResponseContent(response);
        HttpMethed.printJsonContent(responseContent);
        String hash = responseContent.getString("value");

        String spendauthsigUrl = "http://" + httpNode + "/wallet/createspendauthsig";
        JSONObjectWarp spendauthsigJson =
            new JSONObjectWarp()
                .put("ask", ask)
                .put("alpha", ByteArray.toHexString(alpha))
                .put("tx_hash", hash);
        String spendauthsigStr = spendauthsigJson.toJSONString();
        JsonObject spendauthsigObj = new JsonParser().parse(spendauthsigStr).getAsJsonObject();
        logger.info("spendauthsigObj:" + spendauthsigObj.toString());
        response = createConnect(spendauthsigUrl, spendauthsigObj);
        responseContent = HttpMethed.parseResponseContent(response);
        HttpMethed.printJsonContent(responseContent);
        String spendauthsig = responseContent.getString("value");

        JSONObject jsonObject = HttpMethed.parseStringContent(transactionString);
        jsonObject
            .getJSONObject("raw_data")
            .getJSONArray("contract")
            .getJSONObject(0)
            .getJSONObject("parameter")
            .getJSONObject("value")
            .getJSONArray("spend_description")
            .getJSONObject(0)
            .put("spend_authority_signature", spendauthsig);
        transactionString = jsonObject.toString();
        logger.info("transactionString2:" + transactionString);
      }

      if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
        transactionSignString = gettransactionsign(httpNode, transactionString, zenTokenOwnerKey);
        logger.info(transactionSignString);
        response = broadcastTransaction(httpNode, transactionSignString);
      } else {
        response = broadcastTransaction(httpNode, transactionString);
      }
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized void freedResource(
      String httpNode, byte[] fromAddress, byte[] toAddress, String fromKey) {
    long balance = HttpMethed.getBalance(httpNode, fromAddress);
    // System.out.println("剩余资源：" + balance);
    sendCoin(httpNode, fromAddress, toAddress, balance - 50000, fromKey);
    balance = HttpMethed.getBalance(httpNode, fromAddress);
    // System.out.println("之后资源：" + balance);
  }

  /** constructor. */
  public static synchronized HttpResponse updateBrokerage(
      String httpNode, byte[] ownerAddress, Long brokerage, String fromKey) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/updateBrokerage";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      userBaseObj2.addProperty("brokerage", brokerage);
      logger.info("userBaseObj2:" + userBaseObj2);
      response = createConnect(requestUrl, userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse updateBrokerageOnVisible(
      String httpNode, byte[] ownerAddress, Long brokerage, String fromKey, String visible) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/updateBrokerage";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("brokerage", brokerage);
      if (visible.equals("true")) {
        userBaseObj2.addProperty("owner_address", Base58.encode58Check(ownerAddress));
      } else if (visible.equals("false")) {
        userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      }
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
      logger.info("userBaseObj2:" + userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getReward(String httpNode, byte[] address) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getReward";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(address));
      logger.info("userBaseObj2:" + userBaseObj2);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getRewardFromSolidity(String httpSolidityNode, byte[] address) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getReward";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(address));
      logger.info("userBaseObj2:" + userBaseObj2);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getRewardFromPbft(String httpSolidityNode, byte[] address) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getReward";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(address));
      logger.info("userBaseObj2:" + userBaseObj2);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBrokerage(String httpNode, byte[] address) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getBrokerage";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(address));
      logger.info("userBaseObj2:" + userBaseObj2);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBrokerageFromSolidity(String httpSolidityNode, byte[] address) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getBrokerage";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(address));
      logger.info("userBaseObj2:" + userBaseObj2);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getRewardOnVisible(String httpNode, byte[] address, String visible) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/getReward";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("address", Base58.encode58Check(address));
      } else if (visible.equals("false")) {
        userBaseObj2.addProperty("address", ByteArray.toHexString(address));
      }
      userBaseObj2.addProperty("visible", visible);
      logger.info("userBaseObj2:" + userBaseObj2);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getRewardFromSolidityOnVisible(
      String httpSolidityNode, byte[] address, String visible) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getReward";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("address", Base58.encode58Check(address));
      } else if (visible.equals("false")) {
        userBaseObj2.addProperty("address", ByteArray.toHexString(address));
      }
      userBaseObj2.addProperty("visible", visible);
      logger.info("userBaseObj2:" + userBaseObj2);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBrokerageOnVisible(
      String httpNode, byte[] address, String visible) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/getBrokerage";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("address", Base58.encode58Check(address));
      } else if (visible.equals("false")) {
        userBaseObj2.addProperty("address", ByteArray.toHexString(address));
      }
      userBaseObj2.addProperty("visible", visible);
      logger.info("userBaseObj2:" + userBaseObj2);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBrokerageFromSolidityOnVisible(
      String httpSolidityNode, byte[] address, String visible) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getBrokerage";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("address", Base58.encode58Check(address));
      } else if (visible.equals("false")) {
        userBaseObj2.addProperty("address", ByteArray.toHexString(address));
      }
      userBaseObj2.addProperty("visible", visible);
      logger.info("userBaseObj2:" + userBaseObj2);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getBrokerageFromPbft(String httpSolidityNode, byte[] address) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpSolidityNode + "/walletpbft/getBrokerage";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("address", ByteArray.toHexString(address));
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized String marketSellAssetGetTxId(
      String httpNode,
      byte[] ownerAddress,
      String sellTokenId,
      Long sellTokenQuantity,
      String buyTokenId,
      Long buyTokenQuantity,
      String fromKey,
      String visible) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    JSONObject responseContent;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/marketsellasset";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("owner_address", Base58.encode58Check(ownerAddress));
        userBaseObj2.addProperty("sell_token_id", sellTokenId);
        userBaseObj2.addProperty("buy_token_id", buyTokenId);
      } else {
        userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
        userBaseObj2.addProperty("sell_token_id", str2hex(sellTokenId));
        userBaseObj2.addProperty("buy_token_id", str2hex(buyTokenId));
      }
      userBaseObj2.addProperty("sell_token_quantity", sellTokenQuantity);
      userBaseObj2.addProperty("buy_token_quantity", buyTokenQuantity);
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
      System.out.println("userBaseObj2: " + userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      System.out.println("transactionString: " + transactionString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseStringContent(transactionSignString);
    return responseContent.getString("txID");
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketOrderById(String httpNode, String orderId, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getmarketorderbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", orderId);
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketOrderByIdFromSolidity(
      String httpSolidityNode, String orderId, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getmarketorderbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", orderId);
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketOrderByIdFromPbft(
      String httpPbftNode, String orderId, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpPbftNode + "/walletpbft/getmarketorderbyid";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", orderId);
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized String marketCancelOrder(
      String httpNode, byte[] ownerAddress, String orderId, String fromKey, String visible) {
    HttpResponse response;
    String transactionString;
    String transactionSignString;
    JSONObject responseContent;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/marketcancelorder";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("owner_address", Base58.encode58Check(ownerAddress));
      } else {
        userBaseObj2.addProperty("owner_address", ByteArray.toHexString(ownerAddress));
      }
      userBaseObj2.addProperty("order_id", orderId);
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
      System.out.println("userBaseObj2: " + userBaseObj2);
      transactionString = EntityUtils.toString(response.getEntity());
      System.out.println("transactionString: " + transactionString);
      transactionSignString = gettransactionsign(httpNode, transactionString, fromKey);
      response = broadcastTransaction(httpNode, transactionSignString);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    responseContent = HttpMethed.parseStringContent(transactionSignString);
    return responseContent.getString("txID");
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketOrderByAccount(
      String httpNode, byte[] ownerAddress, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getmarketorderbyaccount";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("value", Base58.encode58Check(ownerAddress));
      } else {
        userBaseObj2.addProperty("value", ByteArray.toHexString(ownerAddress));
      }
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketOrderByAccountFromSolidity(
      String httpSolidityNode, byte[] ownerAddress, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getmarketorderbyaccount";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("value", Base58.encode58Check(ownerAddress));
      } else {
        userBaseObj2.addProperty("value", ByteArray.toHexString(ownerAddress));
      }
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketOrderByAccountFromPbft(
      String httpPbftNode, byte[] ownerAddress, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpPbftNode + "/walletpbft/getmarketorderbyaccount";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("value", Base58.encode58Check(ownerAddress));
      } else {
        userBaseObj2.addProperty("value", ByteArray.toHexString(ownerAddress));
      }
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketPairList(String httpNode, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getmarketpairlist";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketPairListFromSolidity(
      String httpSolidityNode, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getmarketpairlist";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketPairListFromPbft(String httpPbftNode, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpPbftNode + "/walletpbft/getmarketpairlist";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketOrderListByPair(
      String httpNode, String sellTokenId, String buyTokenId, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getmarketorderlistbypair";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("sell_token_id", sellTokenId);
        userBaseObj2.addProperty("buy_token_id", buyTokenId);
      } else {
        userBaseObj2.addProperty("sell_token_id", str2hex(sellTokenId));
        userBaseObj2.addProperty("buy_token_id", str2hex(buyTokenId));
      }
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketOrderListByPairFromSolidity(
      String httpSolidityNode, String sellTokenId, String buyTokenId, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getmarketorderlistbypair";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("sell_token_id", sellTokenId);
        userBaseObj2.addProperty("buy_token_id", buyTokenId);
      } else {
        userBaseObj2.addProperty("sell_token_id", str2hex(sellTokenId));
        userBaseObj2.addProperty("buy_token_id", str2hex(buyTokenId));
      }
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketOrderListByPairFromPbft(
      String httpPbftNode, String sellTokenId, String buyTokenId, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpPbftNode + "/walletpbft/getmarketorderlistbypair";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("sell_token_id", sellTokenId);
        userBaseObj2.addProperty("buy_token_id", buyTokenId);
      } else {
        userBaseObj2.addProperty("sell_token_id", str2hex(sellTokenId));
        userBaseObj2.addProperty("buy_token_id", str2hex(buyTokenId));
      }
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketPriceByPair(
      String httpNode, String sellTokenId, String buyTokenId, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getmarketpricebypair";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("sell_token_id", sellTokenId);
        userBaseObj2.addProperty("buy_token_id", buyTokenId);
      } else {
        userBaseObj2.addProperty("sell_token_id", str2hex(sellTokenId));
        userBaseObj2.addProperty("buy_token_id", str2hex(buyTokenId));
      }
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketPriceByPairFromSolidity(
      String httpSolidityNode, String sellTokenId, String buyTokenId, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpSolidityNode + "/walletsolidity/getmarketpricebypair";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("sell_token_id", sellTokenId);
        userBaseObj2.addProperty("buy_token_id", buyTokenId);
      } else {
        userBaseObj2.addProperty("sell_token_id", str2hex(sellTokenId));
        userBaseObj2.addProperty("buy_token_id", str2hex(buyTokenId));
      }
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getMarketPriceByPairFromPbft(
      String httpPbftNode, String sellTokenId, String buyTokenId, String visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpPbftNode + "/walletpbft/getmarketpricebypair";
      JsonObject userBaseObj2 = new JsonObject();
      if (visible.equals("true")) {
        userBaseObj2.addProperty("sell_token_id", sellTokenId);
        userBaseObj2.addProperty("buy_token_id", buyTokenId);
      } else {
        userBaseObj2.addProperty("sell_token_id", str2hex(sellTokenId));
        userBaseObj2.addProperty("buy_token_id", str2hex(buyTokenId));
      }
      userBaseObj2.addProperty("visible", visible);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized int getTransactionPendingSize(String httpNode) {
    HttpResponse response;
    JSONObject responseContent;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getpendingsize";
      JsonObject userBaseObj2 = new JsonObject();
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return 0;
    }
    responseContent = HttpMethed.parseResponseContent(response);
    return responseContent.getInteger("pendingSize");
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionListFromPending(String httpNode) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/gettransactionlistfrompending";
      JsonObject userBaseObj2 = new JsonObject();
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getTransactionFromPending(String httpNode, String txid) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/gettransactionfrompending";
      JsonObject userBaseObj2 = new JsonObject();
      userBaseObj2.addProperty("value", txid);
      response = createConnect(requestUrl, userBaseObj2);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getCanDelegatedMaxSize(String httpNode,  byte[] ownerAddress, Long type, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getcandelegatedmaxsize";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("type", type);
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getAvailableUnfreezeCount(String httpNode, byte[] ownerAddress, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getavailableunfreezecount";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getCanWithdrawUnfreezeAmount(
      String httpNode, byte[] ownerAddress, Long timestamp, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getcanwithdrawunfreezeamount";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("timestamp", timestamp);
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceV2(
      String httpNode, byte[] fromAddress, byte[] toAddress, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getdelegatedresourcev2";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("fromAddress",
          visible ? Base58.encode58Check(fromAddress) : ByteArray.toHexString(fromAddress));
      requestParam.addProperty("toAddress",
          visible ? Base58.encode58Check(toAddress) : ByteArray.toHexString(toAddress));
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceAccountIndexV2(String httpNode, byte[] address, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/getdelegatedresourceaccountindexv2";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("value",
          visible ? Base58.encode58Check(address) : ByteArray.toHexString(address));
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return  response;
  }

  /** constructor. */
  public static synchronized HttpResponse getCanDelegatedMaxSizeSolidity(
      String httpNodeSolidity,  byte[] ownerAddress, Long type, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNodeSolidity + "/walletsolidity/getcandelegatedmaxsize";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("type", type);
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    }catch(Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getAvailableUnfreezeCountSolidity(
      String httpNodeSolidity, byte[] ownerAddress, boolean visible){
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNodeSolidity + "/walletsolidity/getavailableunfreezecount";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getCanWithdrawUnfreezeAmountSolidity(
      String httpNodeSolidity, byte[] ownerAddress, Long timestamp, boolean visible){
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNodeSolidity + "/walletsolidity/getcanwithdrawunfreezeamount";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("timestamp", timestamp);
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceV2Solidity(
      String httpNodeSolidity, byte[] fromAddress, byte[] toAddress,boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNodeSolidity + "/walletsolidity/getdelegatedresourcev2";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("fromAddress",
          visible ? Base58.encode58Check(fromAddress) : ByteArray.toHexString(fromAddress));
      requestParam.addProperty("toAddress",
          visible ? Base58.encode58Check(toAddress) : ByteArray.toHexString(toAddress));
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }

  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceAccountIndexV2Solidity(
      String httpNodeSolidity, byte[] address, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNodeSolidity + "/walletsolidity/getdelegatedresourceaccountindexv2";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("value",
          visible ? Base58.encode58Check(address) : ByteArray.toHexString(address));
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return  response;
  }
  /** constructor. */
  public static synchronized HttpResponse getCanDelegatedMaxSizePbft(String httpNode,  byte[] ownerAddress, Long type, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletpbft/getcandelegatedmaxsize";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("type", type);
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getAvailableUnfreezeCountPbft(String httpNode, byte[] ownerAddress, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletpbft/getavailableunfreezecount";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getCanWithdrawUnfreezeAmountPbft(
      String httpNode, byte[] ownerAddress, Long timestamp, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletpbft/getcanwithdrawunfreezeamount";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("timestamp", timestamp);
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceV2Pbft(
      String httpNode, byte[] fromAddress, byte[] toAddress, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletpbft/getdelegatedresourcev2";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("fromAddress",
          visible ? Base58.encode58Check(fromAddress) : ByteArray.toHexString(fromAddress));
      requestParam.addProperty("toAddress",
          visible ? Base58.encode58Check(toAddress) : ByteArray.toHexString(toAddress));
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return response;
  }
  /** constructor. */
  public static synchronized HttpResponse getDelegatedResourceAccountIndexV2Pbft(String httpNode, byte[] address, boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletpbft/getdelegatedresourceaccountindexv2";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("value",
          visible ? Base58.encode58Check(address) : ByteArray.toHexString(address));
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return  response;
  }

  /** constructor. */
  public static synchronized HttpResponse getEstimateEnergy(
      String httpNode,
      byte[] ownerAddress,
      byte[] contractAddress,
      String functionSelector,
      String parameter,
      String data,
      boolean visible,
      long call_value,
      long call_token_value,
      long token_id) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/wallet/estimateenergy";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      if(contractAddress == null){
        String tem = null;
        requestParam.addProperty("contract_address", tem);
      }else {
        requestParam.addProperty("contract_address",
            visible ? Base58.encode58Check(contractAddress) : ByteArray.toHexString(contractAddress));
      }
      requestParam.addProperty("function_selector", functionSelector);
      requestParam.addProperty("parameter", parameter);
      requestParam.addProperty("data", data);
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return  response;
  }

  /** constructor. */
  public static synchronized HttpResponse getEstimateEnergySolidity(
      String httpNode,
      byte[] ownerAddress,
      byte[] contractAddress,
      String functionSelector,
      String parameter,
      boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletsolidity/estimateenergy";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("contract_address",
          visible ? Base58.encode58Check(contractAddress) : ByteArray.toHexString(contractAddress));
      requestParam.addProperty("function_selector", functionSelector);
      requestParam.addProperty("parameter", parameter);
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return  response;
  }

  /** constructor. */
  public static synchronized HttpResponse getEstimateEnergyPBFT(
      String httpNode,
      byte[] ownerAddress,
      byte[] contractAddress,
      String functionSelector,
      String parameter,
      boolean visible) {
    HttpResponse response;
    try {
      String requestUrl = "http://" + httpNode + "/walletpbft/estimateenergy";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
          visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      requestParam.addProperty("contract_address",
          visible ? Base58.encode58Check(contractAddress) : ByteArray.toHexString(contractAddress));
      requestParam.addProperty("function_selector", functionSelector);
      requestParam.addProperty("parameter", parameter);
      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return  response;
  }
  /** constructor. */
  public static synchronized HttpResponse getEstimateEnergyDeployContract(
          String httpNode,
          byte[] ownerAddress,
          byte[] contractAddress,
          String functionSelector,
          String parameter,
          String data,
          long call_value,
          long call_token_value,
          long token_id,
          boolean visible) {
    HttpResponse response;
    try {
      final String requestUrl = "http://" + httpNode + "/wallet/estimateenergy";
      JsonObject requestParam = new JsonObject();
      requestParam.addProperty("owner_address",
              visible ? Base58.encode58Check(ownerAddress) : ByteArray.toHexString(ownerAddress));
      if (contractAddress == null) {
        String tem = null;
        requestParam.addProperty("contract_address", tem);
      } else {
        requestParam.addProperty("contract_address",
                visible ? Base58.encode58Check(contractAddress) : ByteArray.toHexString(contractAddress));
      }
      requestParam.addProperty("function_selector", functionSelector);
      requestParam.addProperty("parameter", parameter);
      requestParam.addProperty("data", data);
      requestParam.addProperty("call_value", call_value);

      requestParam.addProperty("call_token_value", call_token_value);
      requestParam.addProperty("token_id", token_id);

      requestParam.addProperty("visible", visible);
      response = createConnect(requestUrl, requestParam);
    } catch (Exception e) {
      e.printStackTrace();
      httppost.releaseConnection();
      return null;
    }
    return  response;
  }

}
