package javaelmexample.server;

import static functionalj.list.FuncList.listOf;
import static functionalj.map.FuncMap.newMap;
import static nullablej.nullable.Nullable.nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;

public class Server {
    
    private static final Supplier<? extends ExecutorService> defaultExecutor = Executors::newCachedThreadPool;
    private static final byte[]                              emptyBytes      = new byte[0];
    
    private static final Map<String, String> extContentTypes = newMap(String.class, String.class)
                    .with(".icon", "image/x-icon")
                    .with(".html", "text/html; charset=utf-8")
                    .with(".htm",  "text/html; charset=utf-8")
                    .with(".js",   "application/javascript")
                    .with(".css",  "text/css; charset=utf-8")
                    .with(".json", "text/json; charset=utf-8")
                    .with(".yaml", "text/yaml; charset=utf-8")
                    .with(".yml",  "text/yaml; charset=utf-8")
                    .with(".jpg",  "image/jpeg")
                    .with(".jpeg", "image/jpeg")
                    .with(".png",  "image/png")
                    .build();
    
    private final ExecutorService executor;
    private final int             portNumber;
    
    private final AtomicBoolean stillRunning = new AtomicBoolean(true);
    
    public Server(int portNumber) {
        this(portNumber, null);
    }
    public Server(int portNumber, ExecutorService executor) {
        this.executor   = nullable(executor).orElseGet(defaultExecutor);
        this.portNumber = portNumber;
    }
    
    public void start() throws IOException {
        var httpServer = createHttpServer(portNumber);
        httpServer.createContext("/", this::handle);
        httpServer.setExecutor(executor);
        
        try {
            httpServer.start();
            
//            if ((args != null) && (args.length != 0) && args[0].equals("--infinite")) {
//                System.out.println("Touch 'control.file' to stop the server ...");
//                var controlFile = new File("control.file");
//                var lastModified = controlFile.exists() ? controlFile.lastModified() : -1;
//                while (lastModified == (controlFile.exists() ? controlFile.lastModified() : -1)) {
//                    Thread.sleep(10000);
//                }
//            } else {
                System.out.println("Press 'ENTER' to exit ...");
                try (var scanner = new Scanner(System.in)) { scanner.nextLine(); }
//            }
            
            System.out.println("Shutting down ...");
        } catch (Exception exception) {
            exception.printStackTrace();
            // TODO - Figure out how to do this without exit.
            System.exit(1);
        } finally {
            stillRunning.set(false);
            shutdown(httpServer, null);
            executor.shutdown();
        }
    }
    
    private HttpServer createHttpServer(int port) throws IOException {
        var address = new InetSocketAddress("0.0.0.0", port);
        var httpServer = HttpServer.create(address, 0);
        return httpServer;
    }
    
    private void shutdown(HttpServer httpServer, HttpsServer httpsServer) {
        new Thread(()->{
            httpServer.stop(1);
            System.out.println("HTTP Server is successfully stopped.");
        }).start();
    }
    
    private void handle(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        if (path.isEmpty() || path.equals("/")) {
            path = "index.html";
        }
        
        if (path.contains("/api/")) {
            handleApi(path, exchange);
        } else {
            handleFile(path, exchange);
        }
    }
    
    private void handleApi(String path, HttpExchange exchange) throws IOException {
        responseHttp(exchange, "{}");
    }
    
    private void handleFile(String path, HttpExchange exchange) throws IOException {
        var pathExtension = path.replaceAll("^(.*)(\\.[^.]+)$", "$2");
        var contentType   = extContentTypes.get(pathExtension);
        addHeader(exchange, "Content-Type",  contentType);
        
        var resource = Server.class.getClassLoader().getResourceAsStream("./" + path);
        if (resource != null) {
            var buffer = new ByteArrayOutputStream();
            resource.transferTo(buffer);
            responseHttp(exchange, 200, null, buffer.toByteArray());
        } else {
            responseHttp(exchange, 404, null, ("File not found: " + path).getBytes());
        }
    }
    
    private void responseHttp(HttpExchange exchange, String contentBody) throws IOException {
        var contentBytes = nullable(contentBody).map(String::getBytes).orElse(emptyBytes);
        responseHttp(exchange, 200, null, contentBytes);
    }
    
    private void responseHttp(HttpExchange exchange, int statusCode, String contentType, byte[] contentBody) 
                    throws IOException {
        try {
            addHeader(exchange, "Cache-Control", "no-cache");
            addHeader(exchange, "Content-Type",  contentType);
            
            exchange.sendResponseHeaders(statusCode, contentBody.length);
            var inputStream = new ByteArrayInputStream(contentBody);
            var responseBody = exchange.getResponseBody();
            inputStream.transferTo(responseBody);
        } finally {
            exchange.close();
        }
    }
    
    private void addHeader(HttpExchange exchange, String headerName, String ... contentValues) {
        var values = listOf(contentValues).filterNonNull();
        if (!values.isEmpty()) {
            exchange.getResponseHeaders().put(headerName, values);
        }
    }
    
}
