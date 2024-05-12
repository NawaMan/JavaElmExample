package javaelmexample.main;

import static functionalj.lens.Access.$S;
import static functionalj.list.FuncList.ListOf;
import static functionalj.map.FuncMap.mapOf;
import static functionalj.stream.StreamPlus.streamOf;
import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import functionalj.lens.Access;
import functionalj.list.FuncList;
import functionalj.map.ImmutableFuncMap;
import functionalj.types.Struct;
import javaelmexample.server.Server;
import javaelmexample.services.Person;
import javaelmexample.services.PersonService;
import javaelmexample.services.WithDemoMode;

/**
 * This main create a web service exposing to the port (specify using `--port=<port-number>`, default to 8081).
 */
public class Main {
    
    // List of the browser to try on Linux.
    private static final FuncList<String> browsers = ListOf("chromium", "firefox", "mozilla", "opera");
    
    public static void main(String[] args) throws Exception {
        displayHelpMessage(args);
        
        var portNumber  = determinePortNumber(args);
        var openBrowser = streamOf(args).containsNoneOf("--browser=false");
        var demoMode    = streamOf(args).containsAnyOf ("--demo=true");
        
        var services = mapOf("persons", loadPersonService("data/persons.json"));
        var server   = new Server(portNumber, services);
        var timer    = new Timer();
        
        if (demoMode) {
            System.out.println("Setup the demo mode ...");
            setupDemoMode(services, timer);
        }
        
        var isStarted = server.start();
        if (isStarted) {
            var url = format("http://localhost:%d", portNumber);
            System.out.println(format("Visit `%s`", url) );
            
            if (openBrowser) {
                var pid = ProcessHandle.current().pid();
                attemptOpenBrowser(url + "?pid=" + pid);
            }
            
            System.out.println();
            System.out.println("Press 'ENTER' to exit ...");
            try (var scanner = new Scanner(System.in)) {
                scanner.nextLine();
            }
        }
        
        System.out.println("Shutting down the server ...");
        timer.cancel();
        server.stop(() -> System.out.println("Server is successfully stopped."));
    }

    private static void setupDemoMode(ImmutableFuncMap<String, PersonService> services, Timer timer) {
        services
        .values()
        .filter (WithDemoMode.class)
        .map    (WithDemoMode.class::cast)
        .peek   (service -> service.takeSnapshot())
        .forEach(service -> timer.schedule(timerTask(service::resetToSnapshot), 0L, 5*60*1000L));
    }
    
    private static void displayHelpMessage(String[] args) {
        var askForHelp      = streamOf(args).containsAnyOf("--help");
        var unknownArgument = streamOf(args)
                        .excludeAny("--help", "--browser=false", "--browser=true", "--demo=false", "--demo=true")
                        .exclude(Access.$S.thatStartsWith("--port"))
                        .findAny();
        unknownArgument.ifPresent(argument -> {
            System.out.println("Unknown argument: " + argument);
            System.out.println();
        });
        
        if (askForHelp || unknownArgument.isPresent()) {
            System.out.println("Run a simple web server.");
            System.out.println("Paramerers: ");
            System.out.println("    --help               : print this message.");
            System.out.println("    --browser=false      : disable the attempt to open a browser.");
            System.out.println("    --demo=false         : demo mode -- data is reset every 5 mins.");
            System.out.println("    --port=<port-number> : specify the port number -- default to 8081.");
            
            var code = askForHelp ? 0 : 1;
            System.exit(code);
        }
    }
    
    private static int determinePortNumber(String[] args) {
        return streamOf(args)
                .filter   ($S.thatMatches("^--port=[0-9]+$"))
                .mapToInt ($S.replaceFirst("--port=", "").parseInteger().get())
                .findFirst()
                .orElse   (8081);
    }
    
    private static boolean attemptOpenBrowser(String url) {
        try {
            var os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                System.out.println();
                System.out.println("Open in browser...");
                exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (os.contains("mac")) {
                System.out.println();
                System.out.println("Open in browser...");
                exec("/usr/bin/open", url);
            } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
                System.out.println();
                System.out.println("Open in browser...");
                var browsers = defaultBrowserForLinux().map(Main.browsers::prepend).orElse(Main.browsers);
                var command  = browsers.map($S.concat(" \"" + url + "\"")).join(" || ");
                exec("sh", "-c", command.toString());
            } else {
                return false;
            }
            return true;
        } catch (IOException | InterruptedException e) {
            System.err.println("Fail to open the browser! You may continue by open it yourself.");
            e.printStackTrace();
        }
        return false;
    }
    
    @Struct
    void ExecResult(int code, String output) {}
    
    private static Optional<String> defaultBrowserForLinux() throws IOException, InterruptedException {
        var result = exec("xdg-settings", "get", "default-web-browser");
        if (result.code == 0) {
            var defaultBrowser = result.output.trim();
            if (defaultBrowser.endsWith(".desktop")) {
                return Optional.of(defaultBrowser.replaceAll("\\.desktop$", ""));
            }
        }
        return Optional.empty();
    }
    
    private static ExecResult exec(String ... commands) throws IOException, InterruptedException {
        var process = Runtime.getRuntime().exec(commands);
        var success = process.waitFor(10, TimeUnit.SECONDS);
        if (success) {
            var code   = process.exitValue();
            var output = extractStream(process.getInputStream());
            return new ExecResult(code, output);
        } else {
            return new ExecResult(-1, "");
        }
        
    }
    
    private static String extractStream(InputStream inStream) throws IOException {
        var buffer = new ByteArrayOutputStream();
        inStream.transferTo(buffer);
        return new String(buffer.toByteArray());
    }
    
    //== Loader Persons from file ==
    
    @SuppressWarnings("unchecked")
    static PersonService loadPersonService(String initialDataPath) {
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
    
    static TimerTask timerTask(Runnable action) {
        return new TimerTask() {
            
            @Override
            public void run() {
                action.run();
            }
        };
    }
    
}
