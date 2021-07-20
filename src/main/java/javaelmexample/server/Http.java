package javaelmexample.server;

import static functionalj.list.FuncList.listOf;
import static functionalj.map.FuncMap.newMap;
import static java.util.Collections.unmodifiableMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import functionalj.promise.Promise;
import functionalj.types.Struct;

public class Http {
    
    private static final ThreadLocal<Gson> gson = ThreadLocal.withInitial(() -> new Gson());
    
    public static final int timeout = 30;
    
    public static final Map<String, String> extContentTypes 
                    = unmodifiableMap(
                        newMap(String.class, String.class)
                        .with(".icon", "image/x-icon")
                        .with(".html", "text/html; charset=utf-8")
                        .with(".htm",  "text/html; charset=utf-8")
                        .with(".js",   "application/javascript")
                        .with(".css",  "text/css; charset=utf-8")
                        .with(".json", "text/json; charset=utf-8")
                        .with(".jpg",  "image/jpeg")
                        .with(".jpeg", "image/jpeg")
                        .with(".png",  "image/png")
                        .build());
    
    @Struct
    static interface HttpErrorSpec {
        String error();
        
        public default byte[] toBytes() {
            return toJson(this).getBytes();
        }
    }
    
    @Struct
    static interface ResponseSpec {

        HttpExchange exchange();
        
        default void withError(
                        int          statusCode, 
                        Throwable    throwable) 
                            throws IOException {
            responseError(statusCode, throwable.getMessage());
        }
        
        default void responseError(
                        int          statusCode, 
                        String       errorMessage) 
                            throws IOException {
            var error = new HttpError(errorMessage);
            responseBytes(500, null, error.toBytes());
        }
        
        default void responseBytes(
                        int          statusCode, 
                        String       contentType, 
                        byte[]       contentBody) 
                            throws IOException {
            var exchange = exchange();
            try {
                addHeader("Cache-Control", "no-cache");
                addHeader("Content-Type",  contentType);
                
                exchange.sendResponseHeaders(statusCode, contentBody.length);
                var inputStream = new ByteArrayInputStream(contentBody);
                var responseBody = exchange.getResponseBody();
                inputStream.transferTo(responseBody);
            } finally {
                exchange.close();
            }
        }
        
        default <D> void withResult(D result) throws IOException {
            var json        = toJson(result);
            var contentType = extContentTypes.get(".json");
            responseBytes(200, contentType, json.getBytes());
        }
        
        default void addHeader(
                        String       headerName, 
                        String ...   contentValues) {
            var values = listOf(contentValues).filterNonNull();
            if (!values.isEmpty()) {
                var exchange = exchange();
                exchange.getResponseHeaders().put(headerName, values);
            }
        }
        
        default <D> void withPromise(String description, Promise<D> promise) throws IOException {
            var result = promise.getResult(timeout, TimeUnit.SECONDS);
            if (result.isPresent()) {
                withResult(result.get());
            } else if (result.isNull()) {
                var errorMsg = listOf("Not found", description).filterNonNull().join(": ");
                responseError(404, errorMsg);
            } else {
                // TODO - Handle this based on what the exception is.
                result.orThrowRuntimeException();
            }
        }
    }
    
    public Response responseOf(HttpExchange exchange) {
        return new Response(exchange);
    }
    
    public byte[] extractBodyBytes(HttpExchange exchange) throws IOException {
        var buffer = new ByteArrayOutputStream();
        exchange.getRequestBody().transferTo(buffer);
        return buffer.toByteArray();
    }
    
    public String extractBodyText(HttpExchange exchange) throws IOException {
        var buffer = extractBodyBytes(exchange);
        return new String(buffer);
    }
    
    public <T> T extractBody(HttpExchange exchange, Class<T> serviceData) throws IOException {
        var content = extractBodyText(exchange);
        return fromJson(content, serviceData);
    }
    
    private static <T> T fromJson(String json, Class<T> clss) {
        return gson.get().fromJson(json, clss);
    }
    
    private static <T> String toJson(T object) {
        return gson.get().toJson(object);
    }
    
}
