package javaelmexample.server.services;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import javaelmexample.server.Server;

public class PersonServiceLoader {
    
    @SuppressWarnings("unchecked")
    public static PersonService load(String initialDataPath) {
        var service = new PersonService();
        
        try {
            var resource = Server.class.getClassLoader().getResourceAsStream("./" + initialDataPath);
            var buffer = new ByteArrayOutputStream();
            resource.transferTo(buffer);
            var content = new String(buffer.toByteArray());
            var gson    = new Gson();
            var list    = gson.fromJson(content, JsonArray.class);
            for (var each : list) {
                var map    = gson.fromJson(each, Map.class);
                var person = Person.fromMap(map);
                service.post(person);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return service;
    }
    
}
