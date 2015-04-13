package ru.terra.tproxy.gui;

import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

/**
 * Date: 09.04.15
 * Time: 11:11
 */
public class ProxyRunner implements Runnable {
    @Override
    public void run() {
        final HttpProxyServer proxy = DefaultHttpProxyServer.bootstrap()
                .withPort(55555)
                .withAllowLocalOnly(false)
                .withListenOnAllAddresses(true)
                .withTransparent(true)
                .withChainProxyManager((httpRequest, chainedProxies) -> {
                    chainedProxies.add(newHttpChainedProxy());
                    chainedProxies.add(ChainedProxyAdapter.FALLBACK_TO_DIRECT_CONNECTION);
                })
                .start();
    }


    public static ChainedProxy newHttpChainedProxy() {
        return new UpstreamHttpProxy();
    }

    public static class UpstreamHttpProxy extends ChainedProxyAdapter {

    }

}
