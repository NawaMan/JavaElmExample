package javaelmexample.server;

import static functionalj.function.Func.f;
import static functionalj.lens.Access.theString;
import static functionalj.list.FuncList.listOf;
import static javaelmexample.server.Http.extContentTypes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import functionalj.list.FuncList;
import functionalj.map.FuncMap;

public class Server {
    
    private final AtomicBoolean stillRunning = new AtomicBoolean(true);
    
    private final int             portNumber;
    private final ExecutorService executor;
    private final Http            http;
    private final CountDownLatch  latch = new CountDownLatch(1);
    
    private final AtomicReference<Runnable> onStop = new AtomicReference<>(() -> {});
    
    @SuppressWarnings("rawtypes")
    private final Map<String, ServiceHandler> apiHandlers;
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Server(int portNumber, Map<String, ? extends Service<?>> services) {
        this.portNumber  = portNumber;
        this.executor    = Executors.newCachedThreadPool();
        this.http        = new Http();
        this.apiHandlers = FuncMap.from(services).mapValue(service -> new ServiceHandler(service)).toImmutableMap();
    }
    
    public void start() throws IOException {
        if (!stillRunning.get())
            return;
        
        new Thread(() -> {
            try {
                var address    = new InetSocketAddress("0.0.0.0", portNumber);
                var httpServer = HttpServer.create(address, 0);
                httpServer.setExecutor(executor);
                httpServer.createContext("/", this::handle);
                
                try {
                    httpServer.start();
                    latch.await();
                } catch (Exception exception) {
                    exception.printStackTrace();
                } finally {
                    stillRunning.set(false);
                    shutdown(httpServer);
                    executor.shutdown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        })
        .start();
    }
    
    public void stop() {
        stop(null);
    }
    
    public void stop(Runnable onStop) {
        this.onStop(onStop);
        latch.countDown();
    }
    
    public Server onStop(Runnable onStop) {
        if (onStop != null) {
            this.onStop.updateAndGet(prev -> {
                return () -> {
                    prev.run();
                    onStop.run();
                };
            });
        }
        if (!stillRunning.get()) {
            this.onStop.get().run();
        }
        return this;
    }
    
    private void handle(HttpExchange exchange) throws IOException {
        var response = http.responseOf(exchange);
        try {
            var path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                var pathParts = listOf(path.split("/")).filter(theString.thatIsNotBlank()).skip(/*`api`*/1);
                var isHandled = handleApi(pathParts, exchange);
                if (!isHandled) {
                    response.responseError(404, "Not found: " + path);
                }
            } else {
                handleFile(path, exchange);
            }
        } catch (IllegalArgumentException exception) {
            response.withError(400, exception);
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            response.withError(500, exception);
        }
    }
    
    private void shutdown(HttpServer httpServer) {
        new Thread(()->{
            httpServer.stop(1);
            onStop.get().run();
        }).start();
    }
    
    private boolean handleApi(List<String> paths, HttpExchange exchange) throws IOException {
        var pathParts = FuncList.from(paths);
        var firstPath = pathParts.first();
        var tailPath  = pathParts.skip(1).toImmutableList();
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        var isHandled = firstPath.map(apiHandlers::get)
                      .map(f((ServiceHandler handler) -> handler.handle(tailPath, exchange)))
                      .orElse(false);
        return isHandled;
    }
    
    private void handleFile(String path, HttpExchange exchange) throws IOException {
        if (path.isEmpty()) {
            path = "/";
        }
        if (path.endsWith("/")) {
            path = path + "index.html";
        }
        
        var pathExtension = path.replaceAll("^(.*)(\\.[^.]+)$", "$2");
        var contentType   = extContentTypes.get(pathExtension);
        var response      = http.responseOf(exchange);
        if (contentType == null) {
            response.responseError(401, "Not allowed: " + path);
        } else {
            var resource = Server.class.getClassLoader().getResourceAsStream("./" + path);
            if (resource != null) {
                var buffer = new ByteArrayOutputStream();
                resource.transferTo(buffer);
                response.responseBytes(200, contentType, buffer.toByteArray());
            } else {
                response.responseError(404, "File not found: " + path);
            }
        }
    }
    
}
