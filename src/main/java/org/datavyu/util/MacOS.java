package org.datavyu.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.plugins.Plugin;
import org.datavyu.plugins.nativeosx.NativeOSXPlugin;
import org.datavyu.plugins.qtkitplayer.QtKitPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

/**
 * Holds any MacOS specific functions.
 */
public class MacOS {
    /** The logger for this class */
    private static Logger logger = LogManager.getLogger(DatavyuVersion.class);

    /**
     * Get options for dialog on Mac with three responses.
     *
     * @param yesOption The option for yes.
     * @param noOption The option for no.
     * @param cancelOption The option for cancel.
     *
     * @return A string array with three options in the order: yesOption, cancelOption, noOption.
     */
    public static String[] getOptions(String yesOption, String noOption, String cancelOption) {
        return new String[]{yesOption, cancelOption, noOption};
    }

    /**
     * Get options for dialog on Mac with two responses.
     *
     * @param defaultOption The default option.
     * @param alternativeOption The alternative option.
     *
     * @return A string array with two options in the order: defaultOption, alternativeOption.
     */
    public static String[] getOptions(String defaultOption, String alternativeOption) {
        return new String[]{defaultOption, alternativeOption};
    }

    /**
     * Get version for the MacOS.
     *
     * @return Version number.
     */
    public static int getOSVersion() {
        try {
            String osVersion = System.getProperty("os.version");
            return Integer.valueOf(osVersion.split("\\.")[1]); // get major version
        } catch (Exception e) {
            logger.error("Could not get major version number", e);
        }
        return -1;
    }

    /**
     * Test whether the Apple press and hold functionality is enable.d
     *
     * @return True if press and hold is enabled; otherwise false.
     */
    public static boolean isOSXPressAndHoldEnabled() {
        try {
            Process process = new ProcessBuilder(new String[]{"bash", "-c", "defaults read -g ApplePressAndHoldEnabled"})
                    .redirectErrorStream(true)
                    .directory(new File("./"))
                    .start();
            ArrayList<String> output = new ArrayList<String>();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                output.add(line);
            }
            return !(output.size() > 0 && output.get(0).equals("0"));
        } catch (Exception e) {
            logger.error("Error when checking press and hold on OSX " + e.getMessage());
        }
        return false;
    }

    /**
     * Sets the press and hold value.
     *
     * @param pressAndHold The press and hold state.
     */
    public static void setOSXPressAndHoldValue(boolean pressAndHold) {
        try {
            Process process = new ProcessBuilder(new String[]{
                    "bash", "-c", "defaults write -g ApplePressAndHoldEnabled -bool " + (pressAndHold ? "true" : "false")})
                    .redirectErrorStream(true)
                    .directory(new File("./"))
                    .start();
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append(System.getProperty("line.separator"));
            }
            logger.info("Press and hold for OSX command returned: " + stringBuilder);
        } catch (Exception e) {
            logger.error("Error occurred when processing press and hold " + e);
        }
    }

    /**
     * InvocationHandler for the ApplicationAdapter to override some methods.
     */
    private final static class HandlerForApplicationAdapter implements InvocationHandler {
        /**
         * Called when a method in the proxy object is being invoked.
         *
         * @param proxy  The object we are proxying.
         * @param method The method that is being invoked.
         * @param args   The arguments being supplied to the method.
         * @return CellValue for the method being invoked.
         */
        public Object invoke(final Object proxy, final Method method, final Object[] args) {
            try {
                Class ae = Class.forName("com.apple.eawt.ApplicationEvent");
                Method setHandled;
                String methodName = method.getName();
                switch (methodName) {
                    case "handleAbout":
                        Datavyu.getApplication().showAboutWindow();
                        setHandled = ae.getMethod("setHandled", boolean.class);
                        setHandled.invoke(args[0], true);
                        break;
                    case "handleSupportOpen":
                        Datavyu.getApplication().openSupportSite();
                        setHandled = ae.getMethod("setHandled", boolean.class);
                        setHandled.invoke(args[0], true);
                        break;
                    case "handleGuideOpen":
                        Datavyu.getApplication().openGuideSite();
                        setHandled = ae.getMethod("setHandled", boolean.class);
                        setHandled.invoke(args[0], true);
                        break;
                    case "handleUpdate":
                        Datavyu.getApplication().showUpdateWindow();
                        setHandled = ae.getMethod("setHandled", boolean.class);
                        setHandled.invoke(args[0], true);
                        break;
                    case "handleQuit":
                        boolean shouldQuit = Datavyu.getApplication().safeQuit();
                        if (shouldQuit) {
                            Datavyu.getApplication().getMainFrame().setVisible(false);
                            Datavyu.getApplication().shutdown();
                        }
                        setHandled = ae.getMethod("setHandled", boolean.class);
                        setHandled.invoke(args[0], shouldQuit);
                        break;
                    case "handleOpenFile":
                        Method getFilename = ae.getMethod("getFilename", null);
                        String fileName = (String) getFilename.invoke(args[0],null);
                        if (Datavyu.getApplication().readyToOpenFile) {
                            Datavyu.getApplication().getView().openExternalFile(new File(fileName));
                        } else {
                            Datavyu.getApplication().setCommandLineFile(fileName);
                        }
                        setHandled = ae.getMethod("setHandled", boolean.class);
                        setHandled.invoke(args[0], true);
                        break;
                    default:
                        logger.info("Tried to invoke method " + methodName + " which is not registered");
                }
            } catch (NoSuchMethodException e) {
                logger.error("Unable to access method in application", e);
            } catch (IllegalAccessException e) {
                logger.error("Unable to access application excapeion", e);
            } catch (InvocationTargetException e) {
                logger.error("Unable to invoke target", e);
            } catch (ClassNotFoundException e) {
                logger.error("Unable to find apple classes", e);
            }
            return null;
        }
    }

    /**
     * Load compile handler for MacOS.
     *
     * @return True if load was successful; otherwise false.
     */
    public static boolean loadCompileHandler() {
        // This curly piece of work is a bit body of reflection to basically achieve the following snippet of code that
        // will ultimately compile on any platform.
        // public class MacOSAboutHandler extends Application {
        // public MacOSAboutHandler() { addApplicationListener(new AboutBoxHandler()); }
        // class AboutBoxHandler extends ApplicationAdapter { public void
        // handleAbout(ApplicationEvent event) {
        // Datavyu.getApplication().showAboutWindow(); event.setHandled(true); } } }
        try {
            Class appClass = Class.forName("com.apple.eawt.Application");
            Object app = appClass.newInstance();
            Class appListenerClass = Class.forName("com.apple.eawt.ApplicationListener");
            Object listener = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[]{appListenerClass}, new HandlerForApplicationAdapter());
            // Add the listener to the application.
            Method m = appClass.getMethod("addApplicationListener", appListenerClass);
            m.invoke(app, listener);
            return true;
        } catch (ClassNotFoundException e) {
            logger.error("Unable to find apple classes", e);
        } catch (InstantiationException e) {
            logger.error("Unable to instantiate apple application", e);
        } catch (IllegalAccessException e) {
            logger.error("Unable to access application excapeion", e);
        } catch (NoSuchMethodException e) {
            logger.error("Unable to access method in application", e);
        } catch (InvocationTargetException e) {
            logger.error("Unable to invoke target", e);
        }
        return false;
    }

    /**
     * Get the native OSX plugin.
     *
     * @return OSX plugin.
     */
    public static Plugin getNativeOSXPlugin() {
        try {
            String osVersion = System.getProperty("os.version");
            int major = Integer.valueOf(osVersion.split("\\.")[1]);
            if(major >= 12) {
                return new NativeOSXPlugin();
            } else {
                return new QtKitPlugin();
            }
        } catch (Exception e) {
            logger.error("Could create plugin. Error: ", e);
        }
        return new NativeOSXPlugin();
    }
}
