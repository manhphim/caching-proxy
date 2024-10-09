package org.minhpham;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

public class ProxyServer {
    private final int port;
    private final String originUrl;
    private final CacheManager cacheManager;
    private Tomcat tomcat;

    public ProxyServer(int port, String originUrl) {
        this.port = port;
        this.originUrl = originUrl;
        this.cacheManager = new CacheManager();
    }

    public void start() throws LifecycleException {
        tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        Context ctx = tomcat.addContext("", null);

        // Create the ProxyServlet and add it to Tomcat
        ProxyServlet proxyServlet = new ProxyServlet(cacheManager, originUrl);
        Tomcat.addServlet(ctx, "proxyServlet", proxyServlet);
        ctx.addServletMappingDecoded("/*", "proxyServlet");

        tomcat.start();
        tomcat.getServer().await();
    }

    public void stop() throws LifecycleException {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
    }
}