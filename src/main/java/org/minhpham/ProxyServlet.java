package org.minhpham;

import jakarta.servlet.http.HttpServlet;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class ProxyServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ProxyServlet.class);
    private final CacheManager cacheManager;
    private final String originUrl;
    private final HttpClient httpClient;

    public ProxyServlet(CacheManager cacheManager, String originUrl) {
        this.cacheManager = cacheManager;
        this.originUrl = originUrl;
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String cacheKey = generateCacheKey(req);
            logger.info("Received request: {} {}", req.getMethod(), req.getRequestURI());

            Optional<CachedResponse> cachedResponse = cacheManager.get(cacheKey);

            if (cachedResponse.isPresent()) {
                logger.debug("Cache hit for key: {}", cacheKey);
                sendCachedResponse(resp, cachedResponse.get());
            } else {
                logger.debug("Cache miss for key: {}", cacheKey);
                HttpResponse originResponse = forwardRequestToOrigin(req);

                if (originResponse.getStatusLine().getStatusCode() >= 200 && originResponse.getStatusLine().getStatusCode() < 300) {
                    cacheResponse(cacheKey, originResponse);
                }

                sendResponseToClient(resp, originResponse);
            }
        } catch (Exception e) {
            logger.error("Error processing request", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An error occurred while processing your request.");
        }
    }

    private String generateCacheKey(HttpServletRequest req) {
        String method = req.getMethod();
        String path = req.getRequestURI();
        String queryString = req.getQueryString();
        return method + ":" + path + (queryString != null ? "?" + queryString : "");
    }

    private void sendCachedResponse(HttpServletResponse resp, CachedResponse cachedResponse) throws IOException {
        resp.setStatus(cachedResponse.getStatusCode());
        for (String headerName : cachedResponse.getHeaders().keySet()) {
            resp.setHeader(headerName, cachedResponse.getHeaders().get(headerName));
        }
        resp.setHeader("X-Cache", "HIT");
        resp.getOutputStream().write(cachedResponse.getBody());
    }

    private HttpResponse forwardRequestToOrigin(HttpServletRequest req) throws IOException {
        try {
            String methodName = req.getMethod();
            String requestUrl = originUrl + req.getRequestURI() +
                    (req.getQueryString() != null ? "?" + req.getQueryString() : "");

            RequestBuilder requestBuilder = RequestBuilder.create(methodName)
                    .setUri(new URI(requestUrl));

            // Copy headers
            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = req.getHeader(headerName);
                requestBuilder.addHeader(headerName, headerValue);
            }

            // Set body for POST, PUT, etc.
            if (req.getInputStream() != null) {
                requestBuilder.setEntity(new InputStreamEntity(req.getInputStream()));
            }

            HttpUriRequest proxyRequest = requestBuilder.build();
            return httpClient.execute(proxyRequest);
        } catch (URISyntaxException e) {
            logger.error("Invalid URI", e);
            throw new IOException("Invalid URI", e);
        }
    }

    private void cacheResponse(String cacheKey, HttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();

        // Read the entity content
        HttpEntity entity = response.getEntity();
        byte[] body = EntityUtils.toByteArray(entity);

        // Collect headers
        Map<String, String> headers = new HashMap<>();
        for (Header header : response.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }

        // Create and store the cached response
        CachedResponse cachedResponse = new CachedResponse(body, statusCode, headers);
        cacheManager.put(cacheKey, cachedResponse);

        // Important: Replace the consumed entity with a new ByteArrayEntity
        // This allows the response to be sent to the client after caching
        response.setEntity(new ByteArrayEntity(body));
    }

    private void sendResponseToClient(HttpServletResponse resp, HttpResponse response) throws IOException {
        resp.setStatus(response.getStatusLine().getStatusCode());

        for (org.apache.http.Header header : response.getAllHeaders()) {
            resp.setHeader(header.getName(), header.getValue());
        }
        resp.setHeader("X-Cache", "MISS");

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            entity.writeTo(resp.getOutputStream());
        }

    }
}