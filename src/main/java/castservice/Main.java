package castservice;

import java.io.FileInputStream;
import java.util.Properties;

public class Main {
    static CastService service_;
    static String PROPERTIES_FILE = "CastService.properties";

    public static void main(String[] args)
    {
        int exit_code = 0;
        service_ = new CastService();
        try {
            // install shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutdownHook called!");
                try {
                    service_.shutdown();
                } catch(InterruptedException ignore) {}
            }));
            // load local service properties into System properties
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(PROPERTIES_FILE));
                for (String name : props.stringPropertyNames()) {
                    String value = props.getProperty(name);
                    System.setProperty(name, value);
                }
            } catch (Exception ignore) { }
            // start service
            service_.register();
            System.out.println("CastService v" + CastService.VERSION);
            System.out.println("Ready");
            // keep main thread running just in case
            while(!service_.isShutdown())
            {
                Thread.sleep(200);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            exit_code = 2;
        }
        System.out.println("Exiting...");
        System.exit(exit_code);
    }
}


