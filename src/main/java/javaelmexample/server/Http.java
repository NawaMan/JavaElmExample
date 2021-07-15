package javaelmexample.server;

import static functionalj.list.FuncList.listOf;
import static functionalj.map.FuncMap.newMap;
import static java.util.Collections.unmodifiableMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import functionalj.types.Struct;

public class Http {
    
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
    
    public void responseError(
                    HttpExchange exchange, 
                    int          statusCode, 
                    Throwable    throwable) 
                        throws IOException {
        responseError(exchange, statusCode, throwable.getMessage());
    }
    
    public void responseError(
                    HttpExchange exchange, 
                    int          statusCode, 
                    String       errorMessage) 
                        throws IOException {
        var error = new HttpError(errorMessage);
        responseBytes(exchange, 500, null, error.toBytes());
    }
    
    public void responseBytes(
                    HttpExchange exchange, 
                    int          statusCode, 
                    String       contentType, 
                    byte[]       contentBody) 
                        throws IOException {
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
    
    public void addHeader(
                    HttpExchange exchange, 
                    String       headerName, 
                    String ...   contentValues) {
        var values = listOf(contentValues).filterNonNull();
        if (!values.isEmpty()) {
            exchange.getResponseHeaders().put(headerName, values);
        }
    }
    
}
