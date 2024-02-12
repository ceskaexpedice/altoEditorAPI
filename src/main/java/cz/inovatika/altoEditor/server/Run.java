package cz.inovatika.altoEditor.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Run {

    private static final Logger LOGGER = LoggerFactory.getLogger(Run.class.getName());

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