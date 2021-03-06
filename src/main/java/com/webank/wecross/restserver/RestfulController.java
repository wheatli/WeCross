package com.webank.wecross.restserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.wecross.account.AccountManager;
import com.webank.wecross.common.NetworkQueryStatus;
import com.webank.wecross.exception.WeCrossException;
import com.webank.wecross.host.WeCrossHost;
import com.webank.wecross.resource.Resource;
import com.webank.wecross.resource.ResourceDetail;
import com.webank.wecross.restserver.request.ResourceRequest;
import com.webank.wecross.restserver.request.StateRequest;
import com.webank.wecross.restserver.response.AccountResponse;
import com.webank.wecross.restserver.response.ResourceResponse;
import com.webank.wecross.restserver.response.StateResponse;
import com.webank.wecross.restserver.response.StubResponse;
import com.webank.wecross.routine.htlc.HTLCManager;
import com.webank.wecross.stub.Account;
import com.webank.wecross.stub.Path;
import com.webank.wecross.stub.StubManager;
import com.webank.wecross.stub.TransactionContext;
import com.webank.wecross.stub.TransactionRequest;
import com.webank.wecross.stub.TransactionResponse;
import com.webank.wecross.zone.ZoneManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestfulController {

    @javax.annotation.Resource private WeCrossHost host;

    private Logger logger = LoggerFactory.getLogger(RestfulController.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping("/test")
    public String test() {
        return "OK!";
    }

    @RequestMapping(value = "/supportedStubs", method = RequestMethod.POST)
    public RestResponse<StubResponse> supportedStubs(@RequestBody String restRequestString) {
        RestResponse<StubResponse> restResponse = new RestResponse<>();
        restResponse.setVersion(Versions.currentVersion);
        restResponse.setErrorCode(NetworkQueryStatus.SUCCESS);
        restResponse.setMessage(NetworkQueryStatus.getStatusMessage(NetworkQueryStatus.SUCCESS));

        logger.debug("request string: {}", restRequestString);

        try {
            RestRequest restRequest =
                    objectMapper.readValue(restRequestString, new TypeReference<RestRequest>() {});
            restRequest.checkRestRequest("", "supportedStubs");
            StubResponse stubResponse = new StubResponse();
            ZoneManager zoneManager = host.getZoneManager();
            StubManager stubManager = zoneManager.getStubManager();
            stubResponse.setStubTypes(stubManager);
            restResponse.setData(stubResponse);
        } catch (WeCrossException e) {
            logger.warn("Process request error", e);
            restResponse.setErrorCode(NetworkQueryStatus.EXCEPTION_FLAG + e.getErrorCode());
            restResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            logger.warn("Process request error", e);
            restResponse.setErrorCode(NetworkQueryStatus.INTERNAL_ERROR);
            restResponse.setMessage(e.getMessage());
        }
        return restResponse;
    }

    @RequestMapping(value = "/listResources", method = RequestMethod.POST)
    public RestResponse<ResourceResponse> listResources(@RequestBody String restRequestString) {
        RestResponse<ResourceResponse> restResponse = new RestResponse<>();
        restResponse.setVersion(Versions.currentVersion);
        restResponse.setErrorCode(NetworkQueryStatus.SUCCESS);
        restResponse.setMessage(NetworkQueryStatus.getStatusMessage(NetworkQueryStatus.SUCCESS));

        logger.debug("request string: {}", restRequestString);

        try {
            RestRequest<ResourceRequest> restRequest =
                    objectMapper.readValue(
                            restRequestString,
                            new TypeReference<RestRequest<ResourceRequest>>() {});
            restRequest.checkRestRequest("", "listResources");
            ResourceRequest resourceRequest = restRequest.getData();
            ZoneManager zoneManager = host.getZoneManager();
            ResourceResponse resourceResponse = new ResourceResponse();
            resourceResponse.setResourceInfos(zoneManager, resourceRequest.isIgnoreRemote());
            restResponse.setData(resourceResponse);
        } catch (WeCrossException e) {
            logger.warn("Process request error", e);
            restResponse.setErrorCode(NetworkQueryStatus.EXCEPTION_FLAG + e.getErrorCode());
            restResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            logger.warn("Process request error", e);
            restResponse.setErrorCode(NetworkQueryStatus.INTERNAL_ERROR);
            restResponse.setMessage(e.getMessage());
        }
        return restResponse;
    }

    @RequestMapping(value = "/listAccounts", method = RequestMethod.POST)
    public RestResponse<AccountResponse> listAccounts(@RequestBody String restRequestString) {
        RestResponse<AccountResponse> restResponse = new RestResponse<>();
        restResponse.setVersion(Versions.currentVersion);
        restResponse.setErrorCode(NetworkQueryStatus.SUCCESS);
        restResponse.setMessage(NetworkQueryStatus.getStatusMessage(NetworkQueryStatus.SUCCESS));

        logger.debug("request string: {}", restRequestString);

        try {
            AccountManager accountManager = host.getAccountManager();
            RestRequest restRequest =
                    objectMapper.readValue(restRequestString, new TypeReference<RestRequest>() {});
            restRequest.checkRestRequest("", "listAccounts");
            Map<String, com.webank.wecross.stub.Account> accounts = accountManager.getAccounts();
            List<Map<String, String>> accountInfos = new ArrayList<Map<String, String>>();
            for (Account account : accounts.values()) {
                Map<String, String> accountInfo = new HashMap<String, String>();
                accountInfo.put("name", account.getName());
                accountInfo.put("type", account.getType());

                accountInfos.add(accountInfo);
            }

            AccountResponse accountResponse = new AccountResponse();
            accountResponse.setAccountInfos(accountInfos);
            restResponse.setData(accountResponse);
        } catch (WeCrossException e) {
            logger.warn("Process request error", e);
            restResponse.setErrorCode(NetworkQueryStatus.EXCEPTION_FLAG + e.getErrorCode());
            restResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            logger.warn("Process request error", e);
            restResponse.setErrorCode(NetworkQueryStatus.INTERNAL_ERROR);
            restResponse.setMessage(e.getMessage());
        }
        return restResponse;
    }

    @RequestMapping(value = "/state")
    public RestResponse<StateResponse> handlesState() {
        RestResponse<StateResponse> restResponse = new RestResponse<StateResponse>();

        StateResponse stateResponse = host.getState(new StateRequest());
        restResponse.setVersion(Versions.currentVersion);
        restResponse.setErrorCode(NetworkQueryStatus.SUCCESS);
        restResponse.setMessage(NetworkQueryStatus.getStatusMessage(NetworkQueryStatus.SUCCESS));
        restResponse.setData(stateResponse);

        return restResponse;
    }

    @RequestMapping(value = "/{network}/{stub}/{resource}/{method}", method = RequestMethod.GET)
    public RestResponse<Object> handleResource(
            @PathVariable("network") String network,
            @PathVariable("stub") String stub,
            @PathVariable("resource") String resource,
            @PathVariable("method") String method) {
        return handleResource(network, stub, resource, method, "");
    }

    @RequestMapping(
            value = {
                "/{network}/{stub}/{resource}/{method}",
            },
            method = RequestMethod.POST)
    public RestResponse<Object> handleResource(
            @PathVariable("network") String network,
            @PathVariable("stub") String stub,
            @PathVariable("resource") String resource,
            @PathVariable("method") String method,
            @RequestBody String restRequestString) {
        Path path = new Path();
        path.setNetwork(network);
        path.setChain(stub);
        path.setResource(resource);

        RestResponse<Object> restResponse = new RestResponse<Object>();
        restResponse.setVersion(Versions.currentVersion);
        restResponse.setErrorCode(NetworkQueryStatus.SUCCESS);
        restResponse.setMessage(NetworkQueryStatus.getStatusMessage(NetworkQueryStatus.SUCCESS));

        logger.debug("request string: {}", restRequestString);

        try {
            AccountManager accountManager = host.getAccountManager();
            Resource resourceObj = host.getResource(path);
            if (resourceObj == null) {
                logger.warn("Unable to find resource: {}", path.toString());
            } else {
                HTLCManager htlcManager = host.getRoutineManager().getHtlcManager();
                resourceObj =
                        htlcManager.filterHTLCResource(host.getZoneManager(), path, resourceObj);
            }

            switch (method) {
                case "status":
                    {
                        if (resourceObj == null) {
                            restResponse.setData("not exists");
                        } else {
                            restResponse.setData("exists");
                        }
                        break;
                    }
                case "detail":
                    {
                        if (resourceObj == null) {
                            throw new WeCrossException(
                                    WeCrossException.ErrorCode.RESOURCE_ERROR,
                                    "Resource not found");
                        } else {
                            ResourceDetail resourceDetail = new ResourceDetail();
                            restResponse.setData(
                                    resourceDetail.initResourceDetail(
                                            resourceObj, path.toString()));
                        }
                        break;
                    }
                case "call":
                    {
                        if (resourceObj == null) {
                            throw new WeCrossException(
                                    WeCrossException.ErrorCode.RESOURCE_ERROR,
                                    "Resource not found");
                        }

                        RestRequest<TransactionRequest> restRequest =
                                objectMapper.readValue(
                                        restRequestString,
                                        new TypeReference<RestRequest<TransactionRequest>>() {});

                        restRequest.checkRestRequest(path.toString(), method);

                        TransactionRequest transactionRequest =
                                (TransactionRequest) restRequest.getData();

                        String accountName = restRequest.getAccountName();
                        Account account = accountManager.getAccount(accountName);
                        logger.trace(
                                "call request: {}, account: {}",
                                transactionRequest.toString(),
                                accountName);

                        TransactionResponse transactionResponse =
                                (TransactionResponse)
                                        resourceObj.call(
                                                new TransactionContext<TransactionRequest>(
                                                        transactionRequest,
                                                        account,
                                                        resourceObj.getResourceInfo(),
                                                        resourceObj
                                                                .getResourceBlockHeaderManager()));
                        logger.trace("call response: {}", transactionResponse.toString());
                        restResponse.setData(transactionResponse);
                        break;
                    }
                case "sendTransaction":
                    {
                        if (resourceObj == null) {
                            throw new WeCrossException(
                                    WeCrossException.ErrorCode.RESOURCE_ERROR,
                                    "Resource not found");
                        }
                        RestRequest<TransactionRequest> restRequest =
                                objectMapper.readValue(
                                        restRequestString,
                                        new TypeReference<RestRequest<TransactionRequest>>() {});

                        restRequest.checkRestRequest(path.toString(), method);

                        TransactionRequest transactionRequest =
                                (TransactionRequest) restRequest.getData();
                        String accountName = restRequest.getAccountName();
                        Account account = accountManager.getAccount(accountName);
                        logger.trace(
                                "sendTransaction request: {}, account: {}",
                                transactionRequest.toString(),
                                accountName);

                        TransactionResponse transactionResponse =
                                (TransactionResponse)
                                        resourceObj.sendTransaction(
                                                new TransactionContext<TransactionRequest>(
                                                        transactionRequest,
                                                        account,
                                                        resourceObj.getResourceInfo(),
                                                        resourceObj
                                                                .getResourceBlockHeaderManager()));
                        logger.trace(
                                "sendTransaction response: {}", transactionResponse.toString());
                        restResponse.setData(transactionResponse);
                        break;
                    }
                default:
                    {
                        logger.warn("Unsupported method: {}", method);
                        restResponse.setErrorCode(NetworkQueryStatus.METHOD_ERROR);
                        restResponse.setMessage("Unsupported method: " + method);
                        break;
                    }
            }
        } catch (WeCrossException e) {
            logger.warn("Process request error", e);
            restResponse.setErrorCode(NetworkQueryStatus.EXCEPTION_FLAG + e.getErrorCode());
            restResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            logger.warn("Process request error:", e);
            restResponse.setErrorCode(NetworkQueryStatus.INTERNAL_ERROR);
            restResponse.setMessage(e.getLocalizedMessage());
        }

        return restResponse;
    }
}
