package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Mark on 09/12/2016.
 */
public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static String APPLICATION_ID;
    public static String SERVLET_URL;
    public static String PIN_CODE;

    static Properties prop;

    public static void loadProperties() {
        if (prop == null) {
            prop = new Properties();
            InputStream input = null;

            try {
                //input = new FileInputStream("src/main/java/config/config.properties");
                input = Config.class.getResourceAsStream("/config.properties");

                // load a properties file
                prop.load(input);

                // get the property value and print it out
                APPLICATION_ID = prop.getProperty("application_id");
                SERVLET_URL = prop.getProperty("servlet_url");
                PIN_CODE = prop.getProperty("pin_code");

                log.info(APPLICATION_ID);
                log.info(SERVLET_URL);
                log.info(PIN_CODE);
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
