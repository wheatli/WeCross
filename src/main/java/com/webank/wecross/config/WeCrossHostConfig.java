package com.webank.wecross.config;

import com.webank.wecross.account.AccountManager;
import com.webank.wecross.common.BCManager;
import com.webank.wecross.host.WeCrossHost;
import com.webank.wecross.p2p.MessageType;
import com.webank.wecross.p2p.P2PMessageEngine;
import com.webank.wecross.p2p.RequestProcessor;
import com.webank.wecross.p2p.netty.P2PService;
import com.webank.wecross.peer.PeerManager;
import com.webank.wecross.routine.RoutineManager;
import com.webank.wecross.zone.ZoneManager;
import javax.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeCrossHostConfig {

    @Resource private ZoneManager zoneManager;

    @Resource private P2PService p2pService;

    @Resource private PeerManager peerManager;

    @Resource private P2PMessageEngine p2pMessageEngine;

    @Resource private RoutineManager routineManager;

    @Resource private AccountManager accountManager;

    @Resource private BCManager bcManager;

    @Bean
    public WeCrossHost newWeCrossHost() {
        System.out.println("Initializing WeCrossHost ...");

        WeCrossHost host = new WeCrossHost();
        host.setZoneManager(zoneManager);
        host.setP2pService(p2pService);
        host.setPeerManager(peerManager);
        host.setAccountManager(accountManager);
        host.setRoutineManager(routineManager);

        // set the p2p engine here to avoid circular reference
        zoneManager.setP2PEngine(p2pMessageEngine);
        RequestProcessor processor =
                (RequestProcessor)
                        p2pService
                                .getInitializer()
                                .getMessageCallBack()
                                .getProcessor(MessageType.RESOURCE_REQUEST);
        processor.setP2pEngine(p2pMessageEngine);
        processor.setRoutineManager(routineManager);

        host.start();
        return host;
    }
}
