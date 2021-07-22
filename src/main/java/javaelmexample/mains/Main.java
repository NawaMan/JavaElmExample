package javaelmexample.mains;

import static functionalj.lens.Access.$S;
import static functionalj.list.FuncList.ListOf;
import static functionalj.map.FuncMap.mapOf;
import static functionalj.stream.StreamPlus.streamOf;
import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import functionalj.lens.Access;
import functionalj.list.FuncList;
import functionalj.tuple.Tuple3;
import javaelmexample.server.Server;
import javaelmexample.services.PersonService;

/**
 * This main create a web service exposing to the port (specify using `--port=<port-number>`, default to 8081).
 */
public class Main {
    
    // List of the browser to try on Linux.
    private static final FuncList<String> browsers = ListOf(
                    "chromium", 
                    "firefox", 
                    "epiphany", 
                    "mozilla", 
                    "konqueror", 
                    "netscape", 
                    "opera", 
                    "lynx"
                    );
    
    public static void main(String[] args) throws Exception {
        displayHelpMessage(args);
        
        var portNumber  = determinePortNumber(args);
        var openBrowser = streamOf(args).containsNoneOf("--browser=false ");
        
        var services = mapOf("persons", PersonService.load("data/persons.json"));
        var server   = new Server(portNumber, services);
        
        server.start();
        
        if (server.isRunning()) {
            var url = format("http://localhost:%d", portNumber);
            System.out.println(format("Visit `%s`", url) );
            
            if (openBrowser) {
                var pid = ProcessHandle.current().pid();
                attemptOpenBrowser(url + "?pid=" + pid);
            }
            
            System.out.println("Press 'ENTER' to exit ...");
            try (var scanner = new Scanner(System.in)) {
                scanner.nextLine();
            }
        }
        
        System.out.println("Shutting down the server ...");
        server.stop(() -> System.out.println("Server is successfully stopped."));
    }
    
    private static void displayHelpMessage(String[] args) {
        var askForHelp      = streamOf(args).containsAnyOf("--help");
        var unknownArgument = streamOf(args)
                        .excludeAny("--help", "--browser=false", "--browser=true")
                        .exclude(Access.$S.thatStartsWith("--port"))
                        .findAny();
        unknownArgument.ifPresent(argument -> {
            System.out.println("Unknown argument: " + argument);
            System.out.println();
        });
        
        if (askForHelp || unknownArgument.isPresent()) {
            var version = determineVersion();
            System.out.println("Run a simple web server.");
            System.out.println("Version: " + version.orElse("<unknown>"));
            System.out.println("Paramerers: ");
            System.out.println("    --help               : print this message.");
            System.out.println("    --browser=false      : disable the attempt to open a browser.");
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
    
    private static Optional<String> determineVersion() {
        String pomContent = null;
        try {
            var resourceAsStream 
                    = Main.class.getClassLoader()
                    .getResourceAsStream("META-INF/maven/nawaman/JavaElmExample/pom.xml");
            var buffer = new ByteArrayOutputStream();
            resourceAsStream.transferTo(buffer);
            pomContent = new String(buffer.toByteArray());
        } catch (NullPointerException | IOException e) {
            return Optional.empty();
        }
        
        return streamOf(pomContent)
                    .map    ($S.split("\n"))
                    .flatMap(List::stream)
                    .mapTwo ()
                    .filter (pair -> pair._1().contains("<name>JavaElmExample</name>"))
                    .map    (pair -> pair._2())
                    .map    ($S.trim().replaceAll("^(.*>)(.+)(</.*)$", "$2"))
                    .findFirst();
    }
    
    private static boolean attemptOpenBrowser(String url) {
        try {
            var os = System.getProperty("os.name").toLowerCase();
            var rt = Runtime.getRuntime();
            if (os.indexOf("win") >= 0) {
                exec("rundll32 url.dll,FileProtocolHandler " + url);
                return true;
            } else if (os.indexOf("mac") >= 0) {
                exec("open " + url);
                return true;
            } else if (os.indexOf("nix") >=0 || os.indexOf("nux") >=0) {
                var browsers = defaultBrowserForLinux()
                             .map   (Main.browsers::prepend)
                             .orElse(Main.browsers);
                var cmd = browsers.map($S.concat(" \"" + url + "\"")).join(" || ");
                rt.exec(new String[] { "sh", "-c", cmd.toString() });
                return true;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Fail to open the browser! You may continue by open it yourself.");
            e.printStackTrace();
        }
        return false;
    }
    
    private static Optional<String> defaultBrowserForLinux() throws IOException, InterruptedException {
        var result = exec("xdg-settings", "get", "default-web-browser");
        if (result._1() == 0) {
            var defaultBrowser = result._2().trim();
            if (defaultBrowser.endsWith(".desktop")) {
                return Optional.of(defaultBrowser.replaceAll("\\.desktop$", ""));
            }
        }
        return Optional.empty();
    }
    
    private static Tuple3<Integer, String, String> exec(String ... commands) throws IOException, InterruptedException {
        var process = Runtime.getRuntime().exec(commands);
        var success = process.waitFor(10, TimeUnit.SECONDS);
        if (!success) {
            return Tuple3.of(-1, "", "Timeout after 10 seconds.");
        }
        
        var code   = process.exitValue();
        var output = extractStream(process.getInputStream());
        var error  = extractStream(process.getErrorStream());
        return Tuple3.of(code, output, error);
    }
    
    private static String extractStream(InputStream inStream) throws IOException {
        var buffer = new ByteArrayOutputStream();
        inStream.transferTo(buffer);
        return new String(buffer.toByteArray());
    }
    
}
