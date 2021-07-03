package javaelmexample.server.services;

import functionalj.types.Struct;
import functionalj.types.elm.Elm;

public class PersonService {
    
    @Elm(baseModule = "", generatedDirectory = "elm/src/")
    @Struct
    void Person(String firstName, String lastName) {}
    
}
