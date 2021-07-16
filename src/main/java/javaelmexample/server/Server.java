package javaelmexample.server;

import static functionalj.lens.Access.theString;
import static functionalj.list.FuncList.listOf;
import static functionalj.map.FuncMap.mapOf;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import functionalj.map.FuncMap;
import javaelmexample.server.services.PersonServiceLoader;

public class Server {
    
    private final AtomicBoolean stillRunning = new AtomicBoolean(true);
    
    private final int             portNumber;
    private final ExecutorService executor;
    private final Http            http;
    private final ResourceHandler fileHandler;
    private final ApiHandler      apiHandler;
    
    
    public Server(int portNumber, Map<String, ? extends Service<?>> services) {
        this.portNumber  = portNumber;
        this.executor    = Executors.newCachedThreadPool();
        this.http        = new Http();
        this.fileHandler = new ResourceHandler();
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        var apiHandlers  = FuncMap.from(services).mapValue(service -> new ServiceHandler(service));
        this.apiHandler  = new ApiHandler(apiHandlers);
    }
    
    public void start() throws IOException {
        var address    = new InetSocketAddress("0.0.0.0", portNumber);
        var httpServer = HttpServer.create(address, 0);
        httpServer.setExecutor(executor);
        httpServer.createContext("/", this::handle);
        
        try {
            httpServer.start();
            waitForKeyPress();
            System.out.println("Shutting down the server ...");
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            stillRunning.set(false);
            shutdown(httpServer);
            executor.shutdown();
        }
    }
    
    private void handle(HttpExchange exchange) throws IOException {
        var response = http.responseOf(exchange);
        try {
            var path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                var pathParts = listOf(path.split("/")).filter(theString.thatIsNotBlank()).skip(/*`api`*/1);
                var isHandled = apiHandler.handleApi(pathParts, exchange);
                if (!isHandled) {
                    response.responseError(404, "Not found: " + path);
                }
            } else {
                fileHandler.handleFile(path, exchange);
            }
        } catch (IllegalArgumentException exception) {
            response.withError(400, exception);
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            response.withError(500, exception);
        }
    }
    
    private void waitForKeyPress() {
        System.out.println("Press 'ENTER' to exit ...");
        try (var scanner = new Scanner(System.in)) {
            scanner.nextLine();
        }
    };
    
    private void shutdown(HttpServer httpServer) {
        new Thread(()->{
            httpServer.stop(1);
            System.out.println("Server is successfully stopped.");
        }).start();
    }
    
    public static void main(String[] args) throws Exception {
        var services = mapOf("persons", PersonServiceLoader.load("data/persons.json"));
        var server   = new Server(8081, services);
        server.start();
    }
    
}
