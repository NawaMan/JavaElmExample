package javaelmexample.server;

import static javaelmexample.server.Http.extContentTypes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

/**
 * Handler for file in the resources.
 **/
public class ResourceHandler {
    
    private final Http http;
    
    public ResourceHandler() {
        this.http = new Http();
    }
    
    /**
     * Given the path, handle the file associated to the path.
     **/
    public void handleFile(String path, HttpExchange exchange) throws IOException {
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
