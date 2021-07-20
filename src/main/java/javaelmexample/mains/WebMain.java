package javaelmexample.mains;

import static functionalj.lens.Access.$S;
import static functionalj.map.FuncMap.mapOf;
import static functionalj.stream.StreamPlus.streamOf;
import static java.lang.String.format;

import java.util.Scanner;

import javaelmexample.server.Server;
import javaelmexample.server.services.PersonService;

public class WebMain {
    
    public static void main(String[] args) throws Exception {
        var portNumber 
                = streamOf(args)
                .filter   ($S.thatMatches("^--port=[0-9]+$"))
                .mapToInt ($S.replaceFirst("--port=", "").parseInteger().get())
                .findFirst()
                .orElse   (8081);
        
        var services = mapOf("persons", PersonService.load("data/persons.json"));
        var server   = new Server(portNumber, services);
        
        server.start();
        System.out.println(format("Visit `http://localhost:%d`", portNumber) );
        
        System.out.println("Press 'ENTER' to exit ...");
        try (var scanner = new Scanner(System.in)) {
            scanner.nextLine();
        }
        
        System.out.println("Shutting down the server ...");
        server.stop(() -> System.out.println("Server is successfully stopped."));
    }
    
}
