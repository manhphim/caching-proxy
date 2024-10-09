package org.minhpham;

import org.apache.catalina.LifecycleException;git st
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.net.URL;

@Command(name = "caching-proxy", version = "Caching Proxy 1.0", mixinStandardHelpOptions = true)
public class MainCommand implements Runnable {
    @Spec
    CommandSpec spec;

    @Option(names = { "-p", "--port" }, description = "The port to run the proxy")
    int port = 8080;

    @Option(names = { "-o", "--origin" }, description = "The origin to forward the request to")
    String origin;

    @Option(names = { "--clear-cache" }, description = "Clear all request caches")
    boolean clearCache;

    @Override
    public void run() {
        if (clearCache) {
            // Implement cache clearing logic
            System.out.println("Cache cleared");
        } else if (origin == null) {
            throw new ParameterException(spec.commandLine(), "Missing required option: --origin");
        } else {
            ProxyServer server = new ProxyServer(port, origin);
            try {
                System.out.println("Starting proxy server on port " + port + " forwarding to " + origin);
                server.start();
            } catch (LifecycleException e) {
                System.err.println("Failed to start server: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MainCommand()).execute(args);
        System.exit(exitCode);
    }
}
