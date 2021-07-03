package javaelmexample.server;

public class Application {
    
    public static void main(String[] args) throws Exception {
        var server = new Server(8081);
        server.start();
    }
    
}
