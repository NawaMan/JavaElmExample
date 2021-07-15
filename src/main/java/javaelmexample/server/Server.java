package javaelmexample.server;

import static functionalj.function.Func.f;
import static functionalj.lens.Access.theString;
import static functionalj.list.FuncList.listOf;
import static functionalj.map.FuncMap.mapOf;
import static functionalj.map.FuncMap.newMap;
import static java.util.Collections.unmodifiableMap;
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

import functionalj.types.Struct;
import javaelmexample.server.services.PersonServiceLoader;

public class Server {
    
    private static final Supplier<? extends ExecutorService> defaultExecutor = Executors::newCachedThreadPool;
    
    public static final Map<String, String> extContentTypes 
                    = unmodifiableMap(
                        newMap(String.class, String.class)
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
                        .build());
    
    @Struct
    static interface HttpErrorSpec {
        String error();
        
        public default byte[] toBytes() {
            return JsonUtil.toJson(this).getBytes();
        }
    }
    
    private final ExecutorService executor;
    private final int             portNumber;
    private final String          basePath;
    
    private final Runnable waitForKeyPress = () -> {
        System.out.println("Press 'ENTER' to exit ...");
        try (var scanner = new Scanner(System.in)) { scanner.nextLine(); }
    };
    
    private final AtomicBoolean stillRunning = new AtomicBoolean(true);
    
    @SuppressWarnings("rawtypes")
    private Map<String, ApiHandler> apiHandlers;
    
    public Server(int portNumber) {
        this(portNumber, null, null);
    }
    public Server(int portNumber, String basePath) {
        this(portNumber, basePath, null);
    }
    public Server(int portNumber, String basePath, ExecutorService executor) {
        this.basePath    = nullable(basePath).orElse   ("/");
        this.executor    = nullable(executor).orElseGet(defaultExecutor);
        this.portNumber  = portNumber;
        
        this.apiHandlers 
                = mapOf("persons", PersonServiceLoader.load("data/persons.json"))
                .mapValue(ApiHandler::new);
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
            if (path.isEmpty() || path.equals(basePath)) {
                path = basePath + "index.html";
            }
            
            if (path.startsWith("/api/")) {
                handleApi(path, exchange);
            } else {
                handleFile(path, exchange);
            }
        } catch (IllegalArgumentException exception) {
            var error = new HttpError(exception.getMessage());
            responseHttp(exchange, 400, null, error.toBytes());
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            var error = new HttpError(exception.getMessage());
            responseHttp(exchange, 500, null, error.toBytes());
        }
    }
    
    private void handleApi(String path, HttpExchange exchange) throws IOException {
        var pathParts 
                = listOf(path.split("/"))
                .filter(theString.thatIsNotBlank())
                .skip(/*`api`*/1)
                .toFuncList();
        
        var firstPath = pathParts.first();
        var tailPath  = pathParts.skip(1);
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        var isHandled 
                = firstPath.map(apiHandlers::get)
                .map(f((ApiHandler handler) -> handler.handle(tailPath, exchange)))
                .orElse(false);
        if (!isHandled) {
            responseHttp(exchange, 404, null, ("Not found: " + path).getBytes());
        }
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
    
    public static void responseHttp(HttpExchange exchange, int statusCode, String contentType, byte[] contentBody) throws IOException {
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
    
    public static void addHeader(HttpExchange exchange, String headerName, String ... contentValues) {
        var values = listOf(contentValues).filterNonNull();
        if (!values.isEmpty()) {
            exchange.getResponseHeaders().put(headerName, values);
        }
    }
    
    public static void main(String[] args) throws Exception {
        var server = new Server(8081);
        server.start();
    }
    
}
