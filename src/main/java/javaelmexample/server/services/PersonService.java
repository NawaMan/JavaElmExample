package javaelmexample.server.services;

import static java.lang.Math.abs;
import static nullablej.nullable.Nullable.nullable;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import functionalj.stream.StreamPlus;
import functionalj.types.Nullable;
import functionalj.types.Struct;
import functionalj.types.elm.Elm;
import javaelmexample.server.Server;
import javaelmexample.server.Service;

public class PersonService implements Service<Person> {
    
    @Elm(baseModule = "", generatedDirectory = "elm/src/")
    @Struct
    void Person(@Nullable String id, String firstName, String lastName) {}
    
    private static final Random rand = new Random();
    
    @SuppressWarnings("unchecked")
    public static PersonService create(String initialDataPath) {
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
    
    private static final Map<String, Person> persons = new ConcurrentHashMap<>();
    
    @Override
    public Optional<Person> get(String id) {
        return Optional.ofNullable(persons.get(id));
    }
    
    @Override
    public StreamPlus<Person> get() {
        return StreamPlus.from(persons.values().stream());
    }
    
    @Override
    public Person post(Person person) {
        var newPersonId = nullable(person.id).orElseGet(()->abs(rand.nextInt()) + "");
        var newPerson   = person.withId(newPersonId);
        persons.put(newPersonId, newPerson);
        return newPerson;
    }

    @Override
    public Optional<Person> delete(String id) {
        return Optional.ofNullable(persons.remove(id));
    }
    
}
