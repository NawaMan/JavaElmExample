package javaelmexample.server;

import static nullablej.nullable.Nullable.nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class Server {
    
    private static final Supplier<? extends ExecutorService> defaultExecutor = Executors::newCachedThreadPool;
    
    private final ExecutorService executor;
    private final int             portNumber;
    private final String          basePath;
    
    private final Runnable waitForKeyPress = () -> {
        System.out.println("Press 'ENTER' to exit ...");
        try (var scanner = new Scanner(System.in)) { scanner.nextLine(); }
    };
    
    private final AtomicBoolean stillRunning = new AtomicBoolean(true);
    
    private Http        http;
    private FileHandler fileHandler;
    private ApiHandler  apiHandler;
    
    
    public Server(int portNumber) {
        this(portNumber, null, null);
    }
    public Server(int portNumber, String basePath) {
        this(portNumber, basePath, null);
    }
    public Server(int portNumber, String basePath, ExecutorService executor) {
        this.portNumber  = portNumber;
        this.basePath    = nullable(basePath).orElse   ("/");
        this.executor    = nullable(executor).orElseGet(defaultExecutor);
        this.fileHandler = new FileHandler();
        this.apiHandler  = new ApiHandler();
    }
    
    public void start() throws IOException {
        var address    = new InetSocketAddress("0.0.0.0", portNumber);
        var httpServer = HttpServer.create(address, 0);
        httpServer.setExecutor(executor);
        httpServer.createContext(basePath, this::handle);
        
        try {
            httpServer.start();
            waitForKeyPress.run();
            System.out.println("Shutting down the server ...");
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            stillRunning.set(false);
            shutdown(httpServer);
            executor.shutdown();
        }
    }
    
    private void shutdown(HttpServer httpServer) {
        new Thread(()->{
            httpServer.stop(1);
            System.out.println("Server is successfully stopped.");
        }).start();
    }
    
    private void handle(HttpExchange exchange) throws IOException {
        try {
            var path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                apiHandler.handleApi(path, exchange);
            } else {
                fileHandler.handleFile(path, exchange);
            }
        } catch (IllegalArgumentException exception) {
            http.responseError(exchange, 400, exception);
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            http.responseError(exchange, 500, exception);
        }
    }
    
    public static void main(String[] args) throws Exception {
        var server = new Server(8081);
        server.start();
    }
    
}
