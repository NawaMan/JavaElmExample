package javaelmexample.server;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

import functionalj.list.FuncList;

public class ServiceHandler<T> { 
    
    private final Service<T> service;
    private final Http       http;
    
    public ServiceHandler(Service<T> service) {
        this.service = service;
        this.http    = new Http();
    }
    
    public boolean handle(
                    FuncList<String> paths, 
                    HttpExchange     exchange) 
                    throws IOException {
        var response = http.responseOf(exchange);
        var method   = exchange.getRequestMethod();
        try {
            var serviceData = service.dataClass();
            
            if (method.equals("GET")) {
                if (paths.isEmpty()) {
                    var result = service.list().getResult();
                    response.withResult(result.get());
                    return true;
                }
                if (paths.size() == 1) {
                    var itemId = paths.first().get();
                    var item   = service.get(itemId);
                    response.withPromise(itemId, item);
                    return true;
                }
            }
            if (method.equals("POST")) {
                if (paths.size() == 0) {
                    var inItem  = http.extractBody(exchange, serviceData);
                    var outItem = service.post(inItem);
                    response.withPromise(null, outItem);
                    return true;
                }
            }
            if (method.equals("PUT")) {
                if (paths.size() == 1) {
                    var itemId  = paths.first().get();
                    var inItem  = http.extractBody(exchange, serviceData);
                    var outItem = service.put(itemId, inItem);
                    response.withPromise(itemId, outItem);
                    return true;
                }
            }
            if (method.equals("DELETE")) {
                if (paths.size() == 1) {
                    var itemId = paths.first().get();
                    var item   = service.delete(itemId);
                    response.withPromise(itemId, item);
                    return true;
                }
            }
        } catch (IllegalArgumentException exception) {
            response.withError(400, exception);
            return true;
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            response.withError(404, exception);
            return true;
        }
        return false;
    }
    
}
