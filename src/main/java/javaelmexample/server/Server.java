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

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javaelmexample.server.services.Person;
import javaelmexample.server.services.PersonService;

public class Server {
    
    private static final Supplier<? extends ExecutorService> defaultExecutor = Executors::newCachedThreadPool;
    
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
    private final String          basePath;
    
    private final Runnable waitForKeyPress = () -> {
        System.out.println("Press 'ENTER' to exit ...");
        try (var scanner = new Scanner(System.in)) { scanner.nextLine(); }
    };
    
    private final AtomicBoolean stillRunning = new AtomicBoolean(true);
    
    private PersonService personalService = null;
    
    public Server(int portNumber) {
        this(portNumber, null, null);
    }
    public Server(int portNumber, String basePath) {
        this(portNumber, basePath, null);
    }
    public Server(int portNumber, String basePath, ExecutorService executor) {
        this.basePath   = nullable(basePath).orElse   ("/");
        this.executor   = nullable(executor).orElseGet(defaultExecutor);
        this.portNumber = portNumber;
    }
    
    public void start() throws IOException {
        var address    = new InetSocketAddress("0.0.0.0", portNumber);
        var httpServer = HttpServer.create(address, 0);
        httpServer.setExecutor(executor);
        httpServer.createContext(basePath, this::handle);
        
        try {
            httpServer.start();
            
            personalService = PersonService.create("data/persons.json");
            
            waitForKeyPress.run();
            System.out.println("Shutting down ...");
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
            System.out.println("HTTP Server is successfully stopped.");
        }).start();
    }
    
    private void handle(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        if (path.isEmpty() || path.equals(basePath)) {
            path = basePath + "index.html";
        }
        
        if (path.contains("/api/")) {
            handleApi(path, exchange);
        } else {
            handleFile(path, exchange);
        }
    }
    
    private void handleApi(String path, HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("GET")) {
            if (path.matches("^/api/persons/?$")) {
                var persons     = personalService.get().toList();
                var result      = new Gson().toJson(persons);
                var contentType = extContentTypes.get(".json");
                responseHttp(exchange, 200, contentType, result.getBytes());
                return;
            }
            if (path.matches("^/api/persons/[^/]+$")) {
                var personId    = path.replaceAll("^(/api/persons/)([^/]+)$", "$2");
                var person      = personalService.get(personId);
                if (person.isEmpty()) {
                    responseHttp(exchange, 404, null, ("Not found: " + path).getBytes());
                } else {
                    var result      = new Gson().toJson(person.get());
                    var contentType = extContentTypes.get(".json");
                    responseHttp(exchange, 200, contentType, result.getBytes());
                }
                return;
            }
        }
        if (exchange.getRequestMethod().equals("POST")) {
            if (path.matches("^/api/persons/?$")) {
                var buffer = new ByteArrayOutputStream();
                exchange.getRequestBody().transferTo(buffer);
                var content     = new String(buffer.toByteArray());
                var inPerson    = new Gson().fromJson(content, Person.class);
                var outPerson   = personalService.post(inPerson);
                var outContent  = new Gson().toJson(outPerson);
                var contentType = extContentTypes.get(".json");
                responseHttp(exchange, 200, contentType, outContent.getBytes());
                return;
            }
        }
        if (exchange.getRequestMethod().equals("DELETE")) {
            if (path.matches("^/api/persons/[^/]+$")) {
                var personId    = path.replaceAll("^(/api/persons/)([^/]+)$", "$2");
                var person      = personalService.delete(personId);
                if (person.isEmpty()) {
                    responseHttp(exchange, 404, null, "{}".getBytes());
                } else {
                    var contentType = extContentTypes.get(".json");
                    responseHttp(exchange, 200, contentType, "{}".getBytes());
                }
                return;
            }
        }
        responseHttp(exchange, 404, null, ("Not found: " + path).getBytes());
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
    
    private void responseHttp(HttpExchange exchange, int statusCode, String contentType, byte[] contentBody) throws IOException {
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
    
    public static void main(String[] args) throws Exception {
        var server = new Server(8081);
        server.start();
    }
    
}
