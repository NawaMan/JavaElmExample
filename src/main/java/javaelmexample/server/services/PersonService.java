package javaelmexample.server.services;

import static java.lang.Math.abs;
import static java.lang.String.format;
import static nullablej.nullable.Nullable.nullable;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import functionalj.list.FuncList;
import functionalj.promise.Promise;
import functionalj.stream.StreamPlus;
import functionalj.types.Nullable;
import functionalj.types.Struct;
import functionalj.types.elm.Elm;
import javaelmexample.server.Server;
import javaelmexample.server.Service;

public class PersonService implements Service<Person> {
    
    @Struct @Elm(baseModule = "", generatedDirectory = "elm/src/")
    void Person(@Nullable String id, String firstName, String lastName, @Nullable String nickName) {}
    
    //== Loader from file ==
    
    @SuppressWarnings("unchecked")
    public static PersonService load(String initialDataPath) {
        var service = new PersonService();
        
        try {
            var resource = Server.class.getClassLoader().getResourceAsStream(initialDataPath);
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
    
    //== Instance ==
    
    private static final Random              random  = new Random();
    private static final Map<String, Person> persons = new ConcurrentHashMap<>();
    
    public Class<Person> dataClass() {
        return Person.class;
    }
    
    @Override
    public Promise<Person> get(String id) {
        var person = persons.get(id);
        return Promise.ofValue(person);
    }
    
    @Override
    public Promise<FuncList<Person>> list() {
        var streamPlus = StreamPlus.from(persons.values().stream());
        var funcList   = streamPlus.toFuncList();
        return Promise.ofValue(funcList);
    }
    
    @Override
    public Promise<Person> post(Person person) {
        if (person == null) {
            return Promise.ofValue(null);
        }
        
        var newPersonId = nullable(person.id).orElseGet(()->abs(random.nextInt()) + "");
        var newPerson   = person.withId(newPersonId);
        persons.put(newPersonId, newPerson);
        return Promise.ofValue(newPerson);
    }
    
    @Override
    public Promise<Person> put(String id, Person person) {
        if (person == null) {
            return null;
        }
        if (!Objects.equals(id, person.id)) {
            var errorMessage = format("ID mismatch: id=[%s] vs item.id=[%s]", id, person.id);
            throw new IllegalArgumentException(errorMessage);
        }
        
        persons.put(person.id, person);
        return Promise.ofValue(person);
    }
    
    @Override
    public Promise<Person> delete(String id) {
        var person = persons.remove(id);
        return Promise.ofValue(person);
    }
    
}
