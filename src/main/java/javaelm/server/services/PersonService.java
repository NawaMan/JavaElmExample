package javaelm.server.services;

import functionalj.types.Struct;
import functionalj.types.elm.Elm;

public class PersonService {
    
    @Elm(baseModule = "", generatedDirectory = "public/src/")
    @Struct
    void Person(String firstName, String lastName) {}
    
}
