package javaelmexample.server;

import static functionalj.function.Func.f;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import functionalj.list.FuncList;
import functionalj.map.FuncMap;

/**
 * Handle the api/
 **/
public class ApiHandler {
    
    @SuppressWarnings("rawtypes")
    private Map<String, ServiceHandler> apiHandlers;
    
    @SuppressWarnings("rawtypes")
    public ApiHandler(Map<String, ServiceHandler> apiHandlers) {
        this.apiHandlers = FuncMap.from(apiHandlers).toImmutableMap();
    }
    
    /**
     * Handle API. 
     **/
    public boolean handleApi(List<String> paths, HttpExchange exchange) throws IOException {
        var pathParts = FuncList.from(paths);
        var firstPath = pathParts.first();
        var tailPath  = pathParts.skip(1).toImmutableList();
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        var isHandled = firstPath.map(apiHandlers::get)
                      .map(f((ServiceHandler handler) -> handler.handle(tailPath, exchange)))
                      .orElse(false);
        return isHandled;
    }
    
}
