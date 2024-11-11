package cz.inovatika.altoEditor.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Run {

    private static final Logger LOGGER = LogManager.getLogger(Run.class.getName());

    public static void main(String[] args)  {
        try {
            AltoEditorInitializer initializer = new AltoEditorInitializer();
            initializer.start();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            ex.printStackTrace();
        }
    }
}