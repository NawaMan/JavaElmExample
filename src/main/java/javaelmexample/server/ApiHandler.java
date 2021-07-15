package javaelmexample.server;

import static functionalj.function.Func.f;
import static functionalj.lens.Access.theString;
import static functionalj.list.FuncList.listOf;
import static functionalj.map.FuncMap.mapOf;

import java.io.IOException;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import functionalj.map.FuncMap;
import javaelmexample.server.services.PersonServiceLoader;

public class ApiHandler {
    
    @SuppressWarnings("rawtypes")
    private Map<String, ServiceHandler> apiHandlers;
    
    private Http http;
    
    public ApiHandler() {
        this(mapOf("persons", PersonServiceLoader.load("data/persons.json"))
                .mapValue(ServiceHandler::new));
    }
    
    @SuppressWarnings("rawtypes")
    public ApiHandler(Map<String, ServiceHandler> apiHandlers) {
        this.apiHandlers = FuncMap.from(apiHandlers).toImmutableMap();
        this.http        = new Http();
    }
    
    public void handleApi(String path, HttpExchange exchange) throws IOException {
        var pathParts 
                = listOf(path.split("/"))
                .filter(theString.thatIsNotBlank())
                .skip(/*`api`*/1)
                .toFuncList();
        
        var firstPath = pathParts.first();
        var tailPath  = pathParts.skip(1).toImmutableList();
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        var isHandled 
                = firstPath.map(apiHandlers::get)
                .map(f((ServiceHandler handler) -> handler.handle(tailPath, exchange)))
                .orElse(false);
        
        if (!isHandled) {
            http.responseError(exchange, 404, "Not found: " + path);
        }
    }
    
}
