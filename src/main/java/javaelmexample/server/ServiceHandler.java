package javaelmexample.server;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

import functionalj.list.FuncList;

/**
 * This class acts as an adapter between HTTP to REST by handling the HTTP request and call associated REST methods.
 **/
public class ServiceHandler<DATA extends RestData> { 
    
    private final RestService<DATA> service;
    private final Http              http;
    
    public ServiceHandler(RestService<DATA> service) {
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
        } catch (UnsupportedHttpMethodException e) {
            methodNotSupported(method, paths, response);
            return true;
        } catch (IllegalArgumentException exception) {
            response.withError(400, exception);
            return true;
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            response.withError(404, exception);
            return true;
        }
        methodNotSupported(method, paths, response);
        return false;
    }
    
    private void methodNotSupported(String method, FuncList<String> paths, Response response) throws IOException {
        var path = paths.join("/");
        response.responseError(405, "HTTP Error 405 – Method Not Allowed: " + method + ":" + path);
    }
    
}
