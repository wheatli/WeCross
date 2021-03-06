package com.webank.wecross.remote;

import com.webank.wecross.p2p.P2PMessage;
import com.webank.wecross.p2p.P2PMessageEngine;
import com.webank.wecross.peer.Peer;
import com.webank.wecross.restserver.Versions;
import com.webank.wecross.stub.Connection;
import com.webank.wecross.stub.Request;
import com.webank.wecross.stub.ResourceInfo;
import com.webank.wecross.stub.Response;
import com.webank.wecross.stub.StubQueryStatus;
import java.util.List;

public class RemoteConnection implements Connection {
    private Peer peer;
    private String path;
    private P2PMessageEngine p2pEngine;

    @Override
    public Response send(Request request) {
        try {
            String errorHistory = "[";
            try {
                P2PMessage<Request> p2pReq = new P2PMessage<Request>();
                p2pReq.setVersion(Versions.currentVersion);
                p2pReq.setMethod(path.replace(".", "/") + "/transaction");
                p2pReq.newSeq();

                request.setResourceInfo(null);
                p2pReq.setData(request);

                RemoteConnectionSemaphoreCallback callback =
                        new RemoteConnectionSemaphoreCallback();

                p2pEngine.asyncSendMessage(peer, p2pReq, callback);

                return callback.getResponseData();

            } catch (Exception e) {
                errorHistory +=
                        "{" + peer.toString() + ", exception:" + e.getLocalizedMessage() + "}";

                throw new Exception("Not an available peer to request: " + errorHistory + "]");
            }
        } catch (Exception e) {
            Response response = new Response();
            response.setErrorCode(StubQueryStatus.REMOTE_QUERY_FAILED);
            response.setErrorMessage("Call remote resource exception: " + e.getMessage());
            return response;
        }
    }

    @Override
    public List<ResourceInfo> getResources() {
        return null;
    }

    public Peer getPeer() {
        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public P2PMessageEngine getP2pEngine() {
        return p2pEngine;
    }

    public void setP2pEngine(P2PMessageEngine p2pEngine) {
        this.p2pEngine = p2pEngine;
    }
}
