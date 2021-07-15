package javaelmexample.server;

import static javaelmexample.server.Http.extContentTypes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

public class FileHandler {
    
    private final Http http;
    
    public FileHandler() {
        this.http = new Http();
    }
    
    public void handleFile(String path, HttpExchange exchange) throws IOException {
        if (path.isEmpty()) {
            path = "/index.html";
        } else if (path.endsWith("/")) {
            path = path + "index.html";
        }
        
        var pathExtension = path.replaceAll("^(.*)(\\.[^.]+)$", "$2");
        var contentType   = extContentTypes.get(pathExtension);
        if (contentType == null) {
            http.responseError(exchange, 401, "Not allowed: " + path);
        } else {
            http.addHeader(exchange, "Content-Type",  contentType);
            var resource = Server.class.getClassLoader().getResourceAsStream("./" + path);
            if (resource != null) {
                var buffer = new ByteArrayOutputStream();
                resource.transferTo(buffer);
                http.responseBytes(exchange, 200, null, buffer.toByteArray());
            } else {
                http.responseError(exchange, 404, "File not found: " + path);
            }
        }
    }
    
}
