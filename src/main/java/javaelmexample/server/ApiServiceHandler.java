package javaelmexample.server;

import static javaelmexample.server.JsonUtil.fromJson;
import static javaelmexample.server.JsonUtil.toJson;
import static javaelmexample.server.Server.extContentTypes;
import static javaelmexample.server.Server.responseHttp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

import functionalj.list.FuncList;
import functionalj.result.Result;

public class ApiServiceHandler<T> { 
    
    private final Service<T> service;
    
    public ApiServiceHandler(Service<T> service) {
        this.service = service;
    }
    
    public boolean handle(
                    FuncList<String> paths, 
                    HttpExchange     exchange) 
                    throws IOException {
        var serviceData = service.dataClass();
        
        if (exchange.getRequestMethod().equals("GET")) {
            if (paths.isEmpty()) {
                var items = service.list().toList();
                responseResult(exchange, items);
                return true;
            }
            if (paths.size() == 1) {
                var itemId = paths.first().get();
                var item   = service.get(itemId);
                responseResult(exchange, item);
                return true;
            }
        }
        if (exchange.getRequestMethod().equals("POST")) {
            if (paths.size() == 0) {
                var inItem  = extractBody(exchange, serviceData);
                var outItem = service.post(inItem);
                responseResult(exchange, outItem);
                return true;
            }
        }
        if (exchange.getRequestMethod().equals("PUT")) {
            if (paths.size() == 1) {
                var itemId  = paths.first().get();
                var inItem  = extractBody(exchange, serviceData);
                var outItem = service.put(itemId, inItem);
                responseResult(exchange, outItem);
                return true;
            }
        }
        if (exchange.getRequestMethod().equals("DELETE")) {
            if (paths.size() == 1) {
                var itemId = paths.first().get();
                var item   = service.delete(itemId);
                responseResult(exchange, item);
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
    
    private void responseResult(HttpExchange exchange, Result<T> result) throws IOException {
        if (result.isPresent()) {
            responseResult(exchange, result.get());
        } else {
            responseHttp(exchange, 404, null, "{}".getBytes());
        }
    }
    
}
