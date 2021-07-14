package javaelmexample.server;

import static javaelmexample.server.JsonUtil.fromJson;
import static javaelmexample.server.JsonUtil.toJson;
import static javaelmexample.server.Server.extContentTypes;
import static javaelmexample.server.Server.responseHttp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

import functionalj.list.FuncList;
import functionalj.promise.Promise;

public class ApiHandler<T> { 
    
    private final Service<T> service;
    
    public ApiHandler(Service<T> service) {
        this.service = service;
    }
    
    public boolean handle(
                    FuncList<String> paths, 
                    HttpExchange     exchange) 
                    throws IOException {
        var serviceData = service.dataClass();
        
        if (exchange.getRequestMethod().equals("GET")) {
            if (paths.isEmpty()) {
                var result = service.list().getResult();
                responseResult(exchange, result.get());
                return true;
            }
            if (paths.size() == 1) {
                var itemId = paths.first().get();
                var item   = service.get(itemId);
                responsePromise(exchange, item);
                return true;
            }
        }
        if (exchange.getRequestMethod().equals("POST")) {
            if (paths.size() == 0) {
                var inItem  = extractBody(exchange, serviceData);
                var outItem = service.post(inItem);
                responsePromise(exchange, outItem);
                return true;
            }
        }
        if (exchange.getRequestMethod().equals("PUT")) {
            if (paths.size() == 1) {
                var itemId  = paths.first().get();
                var inItem  = extractBody(exchange, serviceData);
                var outItem = service.put(itemId, inItem);
                responsePromise(exchange, outItem);
                return true;
            }
        }
        if (exchange.getRequestMethod().equals("DELETE")) {
            if (paths.size() == 1) {
                var itemId = paths.first().get();
                var item   = service.delete(itemId);
                responsePromise(exchange, item);
                return true;
            }
        }
        return false;
    }
    
    private T extractBody(HttpExchange exchange, Class<T> serviceData) throws IOException {
        var buffer = new ByteArrayOutputStream();
        exchange.getRequestBody().transferTo(buffer);
        
        var content = new String(buffer.toByteArray());
        var inItem  = fromJson(content, serviceData);
        return inItem;
    }
    
    private <D> void responseResult(HttpExchange exchange, D result) throws IOException {
        var json        = toJson(result);
        var contentType = extContentTypes.get(".json");
        responseHttp(exchange, 200, contentType, json.getBytes());
    }
    
    private void responsePromise(HttpExchange exchange, Promise<T> promise) throws IOException {
//        var result = promise.getResult(timeout, timeUnit);
        var result = promise.getResult();
        if (result.isPresent()) {
            responseResult(exchange, result.get());
        } else if (result.isNull()) {
            responseHttp(exchange, 404, null, "{}".getBytes());
        } else {
            // TODO - Handle this based on what the exception is.
            result.orThrowRuntimeException();
        }
    }
    
}
