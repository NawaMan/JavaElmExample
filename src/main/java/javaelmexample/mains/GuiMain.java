package javaelmexample.mains;

import static functionalj.function.Func.gracefully;
import static functionalj.map.FuncMap.mapOf;

import java.util.Random;

import javaelmexample.server.Server;
import javaelmexample.server.services.PersonService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class GuiMain extends Application {
    
    private static final String favicon = "/favicons/favicon-32x32.png";

    @Override
    public void start(final Stage stage) {
        var random     = new Random();
        var portNumber = random.nextInt(Short.MAX_VALUE - 1500) + 1500;
        var services   = mapOf("persons", PersonService.load("data/persons.json"));
        var server     = new Server(portNumber, services);
        gracefully(() -> server.start());
        
        var webView = new WebView();
        var webEngine = webView.getEngine();
        webEngine.load("http://localhost:" + portNumber);
        
        stage.getIcons().add(new Image(getClass().getResourceAsStream(favicon)));
        stage.setTitle("JavaElm with FunctionalJ.io");
        
        stage.setOnCloseRequest((__) -> {
            Platform.exit();
            server.stop();
        });
        
        var scene = new Scene(webView);
        stage.setScene(scene);
        stage.show();
    }
    
    public static void main(String[] args) throws Exception {
        // This main has be to run with the following VM arguments
        // --module-path jars/javafx-sdk-11.0.2/lib
        // --add-modules javafx.web
        launch(args);
    }
}
