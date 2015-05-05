package ru.terra.tproxy.service;

/**
 * Date: 14.04.15
 * Time: 11:42
 */
public class ProxyServiceController {
    private static ProxyServiceController instance = new ProxyServiceController();
    private ProxyService service;

    private ProxyServiceController() {
    }

    public void stop() {
        service.stopSelf();
    }

    public ProxyService getService() {
        return service;
    }

    public void setService(ProxyService service) {
        this.service = service;
    }

    public static ProxyServiceController getInstance() {
        return instance;
    }
}
