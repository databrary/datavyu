/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.datavyu;

import ca.beq.util.win32.registry.Win32Exception;
import ch.randelshofer.quaqua.QuaquaManager;
import com.sun.jna.NativeLibrary;
import com.usermetrix.jclient.Logger;
import com.usermetrix.jclient.UserMetrix;
import org.datavyu.controllers.project.ProjectController;
import org.datavyu.models.db.TitleNotifier;
import org.datavyu.models.db.UserWarningException;
import org.datavyu.plugins.PluginManager;
import org.datavyu.plugins.quicktime.QTDataViewer;
import org.datavyu.plugins.vlcfx.NativeLibraryManager;
import org.datavyu.undoableedits.SpreadsheetUndoManager;
import org.datavyu.util.MacHandler;
import org.datavyu.util.NativeLoader;
import org.datavyu.util.WindowsFileAssociations;
import org.datavyu.util.WindowsKeyChar;
import org.datavyu.views.*;
import org.datavyu.views.discrete.SpreadsheetPanel;
import org.jdesktop.application.*;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Stack;

/**
 * The main class of the application.
 */
public final class Datavyu extends SingleFrameApplication
        implements KeyEventDispatcher, TitleNotifier {

    private static final NativeLibraryManager nlm;
    /**
     * The desired minimum initial width.
     */
    private static final int INITMINX = 600;
    /**
     * The desired minimum initial height.
     */
    private static final int INITMINY = 700;
    public static boolean scriptRunning = false;
    private static boolean osxPressAndHoldEnabled;
    /**
     * Constant variable for the Datavyu main panel. This is so we can send
     * keyboard shortcuts to it while the QTController is in focus. It actually
     * get initialized in startup().
     */
    private static DatavyuView VIEW;
    /**
     * The scripting engine manager that we use with Datavyu.
     */
    private static ScriptEngineManager m2;
    /**
     * The scripting engine factory that we use with Datavyu
     */
    private static ScriptEngineFactory sef;
    /**
     * The logger for this class.
     */
    private static Logger LOGGER = UserMetrix.getLogger(Datavyu.class);
    /**
     * The view to use for the quick time video controller.
     */
    private static DataControllerV dataController;
    private static ProjectController projectController;

    /** Load required native libraries (JNI). */
    static {

        String tempDir = System.getProperty("java.io.tmpdir") + File.separator + "vlc" + File.separator;

        System.out.println("WORKING DIR:" + System.getProperty("user.dir"));
        System.out.println("CLASS PATH: " + System.getProperty("java.class.path"));
        nlm = new NativeLibraryManager(tempDir);

        try {

            System.out.println(System.getProperty("jna.library.path"));

            URL resource = Datavyu.class.getClassLoader().getResource("../");
            System.out.println(resource);

            nlm.unpackNativePackage();
            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), tempDir + File.separator + "vlc" + File.separator + "lib");
        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.load(tempDir + "vlc" + File.separator + "lib" + File.separator + "libvlccore.dll");

//        System.load(tempDir + "vlc" + File.separator + "lib" + File.separator + "libvlc.dll");
//        new NativeDiscovery().discover();

        switch (getPlatform()) {
            case MAC:
                try {
                    NativeLoader.LoadNativeLib("quaqua64");
                    System.out.println(System.getProperty("java.library.path"));
//                    System.loadLibrary("QTKitCanvas");

//                    System.loadLibrary("QTJNative");
//                    NativeLoader.LoadNativeLib("QTJNative");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
//                    new NativeLibraryMan
                    System.out.println(System.getProperty("java.library.path"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (getOSXPressAndHoldValue()) {
                    osxPressAndHoldEnabled = true;
                    setOSXPressAndHoldValue(false);
                }

            case WINDOWS:
                try {
                    if (System.getProperty("sun.arch.data.model").equals("32")) {
                        NativeLoader.LoadNativeLib("QTJNative");
                        NativeLoader.LoadNativeLib("QTJavaNative");
                        System.out.println(System.getProperty("java.library.path"));
                        System.loadLibrary("QTJNative");
                        System.loadLibrary("QTJavaNative");

                        QTDataViewer.librariesFound = true;
                    }
                } catch (Exception e) {
                    // for copying style
                    e.printStackTrace();
                }

//                break;
        }


//        System.setProperty("jna.library.path", "/Applications/VLC.app/Contents/MacOS/lib/vlc/lib");
//        System.setProperty("VLC_PLUGIN_PATH", "/Applications/VLC.app/Contents/MacOS/plugins");
    }

    public boolean ready = false;
    /**
     * The scripting engine that we use with Datavyu.
     */
    private ScriptEngine rubyEngine;
    /**
     * The view to use when listing all variables in the database.
     */
    private VariableListV listVarView;
    /**
     * The view to use when listing all the undoable actions.
     */
    private UndoHistoryWindow history;
    /**
     * The view to use when displaying information about Datavyu.
     */
    private AboutV aboutWindow;
    /**
     * The view to use when displaying information about Datavyu updates.
     */
    private UpdateV updateWindow;
    /**
     * Tracks if a NumPad key has been pressed.
     */
    private boolean numKeyDown = false;
    /**
     * Opened windows.
     */
    private Stack<Window> windows;
    /**
     * File path from the command line.
     */
    private String commandLineFile;
    private VideoConverterV videoConverter;

    public static boolean quicktimeLibrariesFound() {
        boolean ans = false;
        try {
            Class.forName("quicktime.QTSession");
            ans = true;
            QTDataViewer.librariesFound = true;

        } catch (ClassNotFoundException ce) {
            System.out.println("Class not found: " + ce.getMessage());
        } catch (Exception e) {
            System.out.println("Non-specific exception! " + e.getMessage());
        } finally {
            return ans;
        }

    }

    /**
     * Gets the single instance of the data controller that is currently used
     * with Datavyu.
     *
     * @return The single data controller in use with this instance of
     * Datavyu.
     */
    public static DataControllerV getDataController() {
        return dataController;
    }

    public static void setDataController(DataControllerV dc) {
        dataController = dc;
    }

    /**
     * A convenient static getter for the application instance.
     *
     * @return The instance of the Datavyu application.
     */
    public static Datavyu getApplication() {
        return Application.getInstance(Datavyu.class);
    }

    /**
     * A convenient static getter for the application session storage.
     *
     * @return The SessionStorage for the Datavyu application.
     */
    public static SessionStorage getSessionStorage() {
        return Datavyu.getApplication().getContext().getSessionStorage();
    }

    /**
     * @return The single instance of the scripting engine we use with
     * Datavyu.
     */
    public static ScriptEngine getScriptingEngine() {
        System.out.println("Returning: " + sef.getEngineName() + " " + sef.getEngineVersion());
        return sef.getScriptEngine();
    }

    /**
     * @return The platform that Datavyu is running on.
     */
    public static Platform getPlatform() {
        String os = System.getProperty("os.name");

        if (os.contains("Mac")) {
            return Platform.MAC;
        }

        if (os.contains("Win")) {
            return Platform.WINDOWS;
        }

        if (os.contains("Linux")) {
            return Platform.LINUX;
        }

        return Platform.UNKNOWN;
    }

    private static boolean getOSXPressAndHoldValue() {
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"defaults", "read", "-g ApplePressAndHoldEnabled"};
        try {
            Process process =
                    new ProcessBuilder(new String[]{"bash", "-c", "defaults read -g ApplePressAndHoldEnabled"})
                            .redirectErrorStream(true)
                            .directory(new File("./"))
                            .start();

            ArrayList<String> output = new ArrayList<String>();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null)
                output.add(line);

            if (output.size() > 0 && output.get(0).equals("0")) {
                return false;
            } else {
//                osxPressAndHoldEnabled = true;
                return true;
            }

            //There should really be a timeout here.
//            if (0 != process.waitFor())
//                return null;

//            return output;

        } catch (Exception e) {
            //Warning: doing this is no good in high quality applications.
            //Instead, present appropriate error messages to the user.
            //But it's perfectly fine for prototyping.

//            return null;
        }

        return true;
    }

    private static void setOSXPressAndHoldValue(boolean bool) {
        String strVal;
        if (bool) {
            strVal = "true";
        } else {
            strVal = "false";
        }
        try {
            Process process =
                    new ProcessBuilder(new String[]{"bash", "-c", "defaults write -g ApplePressAndHoldEnabled -bool " + strVal})
                            .redirectErrorStream(true)
                            .directory(new File("./"))
                            .start();

            ArrayList<String> output = new ArrayList<String>();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null)
                output.add(line);

        } catch (Exception e) {
            //Warning: doing this is no good in high quality applications.
            //Instead, present appropriate error messages to the user.
            //But it's perfectly fine for prototyping.

//            return null;
        }

//        return true;
    }

    /**
     * Main method launching the application.
     *
     * @param args The command line arguments passed to Datavyu.
     */
    public static void main(final String[] args) {

        // If we are running on a MAC set some additional properties:
        if (Datavyu.getPlatform() == Platform.MAC) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Datavyu");
            System.setProperty("Quaqua.jniIsPreloaded", "true");
        }

        launch(Datavyu.class, args);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
//			System.err.println("ERROR: Force shutdown command caught. Initiating shutdown.");
//			Datavyu.getApplication().shutdown();
            }
        });
    }

    public static ProjectController getProjectController() {
        if (VIEW != null && VIEW.getSpreadsheetPanel() != null
                && VIEW.getSpreadsheetPanel().getProjectController() != null) {
            return VIEW.getSpreadsheetPanel().getProjectController();
        }
        return projectController;
    }

    public static void setProjectController(ProjectController p) {
        projectController = p;
    }

    public static DatavyuView getView() {
        return VIEW;
    }

    public void setCommandLineFile(String s) {
        commandLineFile = s;
    }

    /**
     * Dispatches the keystroke to the correct action.
     *
     * @param evt The event that triggered this action.
     * @return true if the KeyboardFocusManager should take no further action
     * with regard to the KeyEvent; false otherwise
     */
    @Override
    public boolean dispatchKeyEvent(final KeyEvent evt) {

        /**
         * This switch is for hot keys that are on the main section of the
         * keyboard.
         */
        int modifiers = evt.getModifiers();

        // BugzID:468 - Define accelerator keys based on OS.
        int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        // If we are typing a key that is a shortcut - we consume it straight
        // away.
        if ((evt.getID() == KeyEvent.KEY_TYPED) && (modifiers == keyMask)) {

            // VIEW also has the fun of key accelerator handling. If it is
            // focused, let it handle the fun or everything is done twice. If it
            // doesn't have focus we manually handle it in the switch below.
            if (getView().getFrame().isFocused()) {
                evt.consume();

                return true;
            }

            switch (getPlatform()) {

                // Code table used by Windows is different.
                case WINDOWS: {
                    switch (WindowsKeyChar.remap(evt.getKeyChar())) {

                        case '+':
                        case '-':
                            // Plus and minus do not respond. Uncomment
                            // the printout above to see what I mean.

                        case 'O':
                            getView().open();
                            evt.consume();

                            return true;

                        case 'S':
                            getView().save();
                            evt.consume();

                            return true;

                        case 'N':
                            getView().showNewProjectForm();
                            evt.consume();

                            return true;

                        case 'L':
                            getView().newCellLeft();
                            evt.consume();

                            return true;

                        case 'R':
                            getView().newCellRight();
                            evt.consume();

                            return true;

                        default:
                            break;
                    }
                }

                break;

                default: {

                    switch (evt.getKeyChar()) {

                        case '=': // Can't access + without shift.
                            getView().zoomIn();
                            evt.consume();

                            return true;

                        case '-':
                            getView().zoomOut();
                            evt.consume();

                            return true;

                        case 'o':
                            getView().open();
                            evt.consume();

                            return true;

                        case 's':
                            getView().save();
                            evt.consume();

                            return true;

                        case 'n':
                            getView().showNewProjectForm();
                            evt.consume();

                            return true;

                        case 'l':
                            getView().newCellLeft();
                            evt.consume();

                            return true;

                        case 'r':
                            getView().newCellRight();
                            evt.consume();

                            return true;

                        default:
                            break;
                    }
                }
            }
        }


        if ((evt.getID() == KeyEvent.KEY_PRESSED)
                && (evt.getKeyLocation() == KeyEvent.KEY_LOCATION_STANDARD)) {

            switch (evt.getKeyCode()) {

                /**
                 * This case is because VK_PLUS is not linked to a key on the
                 * English keyboard. So the GUI is bound to VK_PLUS and VK_SUBTACT.
                 * VK_SUBTRACT is on the numpad, but this is short-circuited above.
                 * The cases return true to let the KeyboardManager know that there
                 * is nothing left to be done with these keys.
                 */
                case KeyEvent.VK_EQUALS:

                    if (modifiers == keyMask) {
                        VIEW.changeFontSize(DatavyuView.ZOOM_INTERVAL);
                    }

                    return true;

                case KeyEvent.VK_MINUS:

                    if (modifiers == keyMask) {
                        VIEW.changeFontSize(-DatavyuView.ZOOM_INTERVAL);
                    }

                    return true;

                default:
                    break;
            }
        }

        // BugzID:784 - Shift key is passed to Data Controller.
        if (evt.getKeyCode() == KeyEvent.VK_SHIFT) {

            if (evt.getID() == KeyEvent.KEY_PRESSED) {
                dataController.setShiftMask(true);
            } else {
                dataController.setShiftMask(false);
            }
        }

        // BugzID:736 - Control key is passed to Data Controller.
        if (evt.getKeyCode() == KeyEvent.VK_CONTROL) {

            if (evt.getID() == KeyEvent.KEY_PRESSED) {
                dataController.setCtrlMask(true);
            } else {
                dataController.setCtrlMask(false);
            }
        }

        /**
         * The following cases handle numpad keystrokes.
         */
        if ((evt.getID() == KeyEvent.KEY_PRESSED)
                && (evt.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD)) {
            numKeyDown = true;
        } else if (numKeyDown && (evt.getID() == KeyEvent.KEY_TYPED)) {
            return true;
        }

        if ((evt.getID() == KeyEvent.KEY_RELEASED)
                && (evt.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD)) {
            numKeyDown = false;
        }

        if (!numKeyDown) {
            return false;
        }

        boolean result = true;

        switch (evt.getKeyCode()) {

            case KeyEvent.VK_DIVIDE:
                //Mac - Show/hide
                if (getPlatform().equals(Platform.MAC)) {
                    dataController.pressShowTracksSmall();
                }
                //Win - point cell
                else {
                    dataController.pressPointCell();
                }

                break;

            case KeyEvent.VK_EQUALS:
                //Mac - point cell
                if (getPlatform().equals(Platform.MAC)) {
                    dataController.pressPointCell();
                }
                //Win - nothing
                break;

            case KeyEvent.VK_ASTERISK:

                break;
            case KeyEvent.VK_MULTIPLY:
                //Win - Show/hide
                if (!getPlatform().equals(Platform.MAC)) {
                    dataController.pressShowTracksSmall();
                }
                break;

            case KeyEvent.VK_NUMPAD7:
                dataController.pressSetCellOnset();

                break;

            case KeyEvent.VK_NUMPAD8:
                dataController.pressPlay();

                break;

            case KeyEvent.VK_NUMPAD9:
                dataController.pressSetCellOffsetNine();

                break;

            case KeyEvent.VK_NUMPAD4:
                dataController.pressShuttleBack();

                break;

            case KeyEvent.VK_NUMPAD5:
                dataController.pressStop();

                break;

            case KeyEvent.VK_NUMPAD6:
                dataController.pressShuttleForward();

                break;

            case KeyEvent.VK_NUMPAD1:

                // We don't do the press Jog thing for jogging - as users often
                // just hold the button down... Which causes weird problems when
                // attempting to do multiple presses.
                dataController.jogBackAction();

                break;

            case KeyEvent.VK_NUMPAD2:
                dataController.pressPause();

                break;

            case KeyEvent.VK_NUMPAD3:

                // We don't do the press Jog thing for jogging - as users often
                // just hold the button down... Which causes weird problems when
                // attempting to do multiple presses.
                dataController.jogForwardAction();

                break;

            case KeyEvent.VK_NUMPAD0:
                dataController.pressCreateNewCellSettingOffset();

                break;

            case KeyEvent.VK_DECIMAL:
                dataController.pressSetCellOffsetPeriod();

                break;

            case KeyEvent.VK_SUBTRACT:

                if (modifiers == InputEvent.CTRL_MASK) {
                    dataController.clearRegionOfInterestAction();
                } else {
                    dataController.pressGoBack();
                }

                break;

            case KeyEvent.VK_ADD:

                if (modifiers == InputEvent.SHIFT_MASK) {
                    dataController.pressFind();
                    dataController.findOffsetAction();
                } else if (modifiers == InputEvent.CTRL_MASK) {
                    dataController.pressFind();
                    dataController.setRegionOfInterestAction();
                } else {
                    dataController.pressFind();
                }

                break;

            case KeyEvent.VK_ENTER:
                dataController.pressCreateNewCell();

                break;

            default:

                // Do nothing with the key.
                result = false;

                break;
        }

        return result;
    }

    /**
     * Action for showing the quicktime video controller.
     */
    public void showDataController() {
        Datavyu.getApplication().show(dataController);
        dataController.setShouldBeVisible(true);
    }

    /**
     * Action for showing the video converter.
     */
    public void showVideoConverter() {
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        videoConverter = new VideoConverterV();
        Datavyu.getApplication().show(videoConverter);
    }

    /**
     * Action for showing the variable list.
     */
    public void showVariableList() {
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        listVarView = new VariableListV(mainFrame, true, projectController.getDB());
        listVarView.registerListeners();

        Datavyu.getApplication().show(listVarView);
    }

    /**
     * Action for showing the Undo History.
     */
    public void showHistory() {
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        SpreadsheetUndoManager undomanager = Datavyu.getApplication().getView().getSpreadsheetUndoManager();
        history = new UndoHistoryWindow(mainFrame, false, undomanager);
        Datavyu.getApplication().show(history);
    }

    /**
     * Action for showing the about window.
     */
    public void showAboutWindow() {
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        aboutWindow = new AboutV(mainFrame, false);
        Datavyu.getApplication().show(aboutWindow);
    }

    /**
     * Action for opening the support site
     */
    public void openSupportSite() {
        String url = "http://www.datavyu.org/support";

        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (java.io.IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Action for opening the guide site
     */
    public void openGuideSite() {
        String url = "http://www.datavyu.org/user-guide/index.html";

        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (java.io.IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Action for showing the about window.
     */
    public void showUpdateWindow() {
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        updateWindow = new UpdateV(mainFrame, true);
        Datavyu.getApplication().show(updateWindow);
    }

    /**
     * Show a warning dialog to the user.
     *
     * @param s The message to present to the user.
     */
    public void showWarningDialog(final String s) {
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        ResourceMap rMap = Application.getInstance(Datavyu.class).getContext().getResourceMap(Datavyu.class);

        JOptionPane.showMessageDialog(mainFrame, s, rMap.getString("WarningDialog.title"), JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Show a warning dialog to the user.
     *
     * @param e The UserWarningException to present to the user.
     */
    public void showWarningDialog(final UserWarningException e) {
        showWarningDialog(e.getMessage());
    }

    /**
     * Show a fatal error dialog to the user.
     */
    public void showErrorDialog() {
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        ResourceMap rMap = Application.getInstance(Datavyu.class).getContext()
                .getResourceMap(Datavyu.class);

        JOptionPane.showMessageDialog(mainFrame,
                rMap.getString("ErrorDialog.message"),
                rMap.getString("ErrorDialog.title"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * User quits- check for save needed. Note that this can be used even in
     * situations when the application is not truly "quitting", but just the
     * database information is being lost (e.g. on an "open" or "new"
     * instruction). In all interpretations, "true" indicates that all unsaved
     * changes are to be discarded.
     *
     * @return True for quit, false otherwise.
     */
    public boolean safeQuit() {
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        ResourceMap rMap = Application.getInstance(Datavyu.class).getContext()
                .getResourceMap(Datavyu.class);

        if (getView().checkAllTabsForChanges()) {
            for (Component tab : getView().getTabbedPane().getComponents()) {
                if (tab instanceof SpreadsheetPanel) {
                    SpreadsheetPanel sp = (SpreadsheetPanel) tab;
                    getView().getTabbedPane().setSelectedComponent(sp);

                    // Ask to save if this spreadsheet has been changed
                    if (sp.getProjectController().isChanged()) {
                        String cancel = "Cancel";
                        String no = "Don't save";
                        String yes = "Save";
                        int noIndex;
                        int yesIndex;
                        int cancelIndex;

                        String[] options = new String[3];
                        //Mac and Windows typically order these buttons differently
                        if (getPlatform() == Platform.MAC) {
                            options[0] = yes;
                            options[1] = cancel;
                            options[2] = no;
                            noIndex = 2;
                            yesIndex = 0;
                            cancelIndex = 1;

                        } else {
                            options[0] = yes;
                            options[1] = no;
                            options[2] = cancel;
                            yesIndex = 0;
                            noIndex = 1;
                            cancelIndex = 2;

                        }

                        // Get name of spreadsheet.  Check in both project and datastore.
                        String projName = sp.getProjectController().getProjectName();
                        if(projName==null) projName = sp.getDatastore().getName();

                        int selection = JOptionPane.showOptionDialog(mainFrame,
                                rMap.getString("UnsavedDialog.message",projName),
                                rMap.getString("UnsavedDialog.title"),
                                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                                null, options, yes);

                        if (selection == yesIndex) getView().save();

                        // If the user cancels, break and return that it isnt safe to quit
                        if (selection == cancelIndex) {
                            return false;
                        }
                    }
                }


            }

            // User has been asked whether or not to save each file, we can return now
            return true;

        } else {

            // Project hasn't been changed.
            return true;
        }
    }

    /**
     * Function to check whether or not it is OK to close this tab
     */
    public boolean safeQuit(Component tab) {
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        ResourceMap rMap = Application.getInstance(Datavyu.class).getContext()
                .getResourceMap(Datavyu.class);
        SpreadsheetPanel sp = (SpreadsheetPanel) tab;
        if (sp.getProjectController().isChanged()) {
            getView().getTabbedPane().setSelectedComponent(sp);

            String cancel = "Cancel";
            String no = "Don't save";
            String yes = "Save";
            int noIndex;
            int yesIndex;
            int cancelIndex;

            String[] options = new String[3];
            //Mac and Windows typically order these buttons differently
            if (getPlatform() == Platform.MAC) {
                options[0] = yes;
                options[1] = cancel;
                options[2] = no;
                noIndex = 2;
                yesIndex = 0;
                cancelIndex = 1;

            } else {
                options[0] = yes;
                options[1] = no;
                options[2] = cancel;
                yesIndex = 0;
                noIndex = 1;
                cancelIndex = 2;

            }

            // Get project name.
            String projName = sp.getProjectController().getProjectName();
            if(projName==null) projName = sp.getDatastore().getName();
            
            int selection = JOptionPane.showOptionDialog(mainFrame,
                    rMap.getString("UnsavedDialog.tabmessage", projName),
                    rMap.getString("UnsavedDialog.title"),
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, yes);

            if (selection == yesIndex) getView().save();

            // If the user cancels, break and return that it isnt safe to quit
            if (selection == cancelIndex) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * Action to call when the application is exiting.
     *
     */
    @Override
    protected void end() {


        Datavyu.getApplication().getMainFrame().setVisible(false);
        UserMetrix.shutdown();
        shutdown();
        super.end();
    }

    /**
     * If the user is trying to save over an existing file, prompt them whether
     * they they wish to continue.
     *
     * @return True for overwrite, false otherwise.
     */
    public boolean overwriteExisting() {
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        ResourceMap rMap = Application.getInstance(Datavyu.class).getContext()
                .getResourceMap(Datavyu.class);
        String defaultOpt = "Cancel";
        String altOpt = "Overwrite";

        String[] a = new String[2];

        if (getPlatform() == Platform.MAC) {
            a[0] = defaultOpt; // This has int value 0 if selected
            a[1] = altOpt; // This has int value 1 if selected.
        } else {
            a[1] = defaultOpt; // This has int value 1 if selected
            a[0] = altOpt; // This has int value 0 if selected.
        }

        int sel =

                JOptionPane.showOptionDialog(mainFrame,
                        rMap.getString("OverwriteDialog.message"),
                        rMap.getString("OverwriteDialog.title"),
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, a, defaultOpt);

        // Button depends on platform now.
        if (getPlatform() == Platform.MAC) {
            return (sel == 1);
        } else {
            return (sel == 0);
        }
    }

    @Override
    protected void initialize(final String[] args) {

        if (getPlatform() == Platform.MAC) {

            try {
                UIManager.setLookAndFeel(QuaquaManager.getLookAndFeel());
            } catch (UnsupportedLookAndFeelException e) {
                System.err.println("Failed to set Quaqua LNF");
                e.printStackTrace();
            }

            new MacHandler();
        }

        // BugzID:1288
        if (getPlatform() == Platform.WINDOWS) {
            try {
                WindowsFileAssociations.setup();
            } catch (Win32Exception e) {
                e.printStackTrace();
            }
        }

        // This is for handling files opened from the command line.
        if (args.length > 0) {
            commandLineFile = args[0];
        }

        windows = new Stack<Window>();

        // Initalise the logger (UserMetrix).
        LocalStorage ls = Datavyu.getApplication().getContext()
                .getLocalStorage();
        ResourceMap rMap = Application.getInstance(Datavyu.class).getContext()
                .getResourceMap(Datavyu.class);

        com.usermetrix.jclient.Configuration config =
                new com.usermetrix.jclient.Configuration(2);
        config.setTmpDirectory(ls.getDirectory().toString() + File.separator);
        config.addMetaData("build",
                rMap.getString("Application.version") + ":"
                        + rMap.getString("Application.build"));
        UserMetrix.initalise(config);
        LOGGER = UserMetrix.getLogger(Datavyu.class);

        // If the user hasn't specified, we don't send error logs.
        if (Configuration.getInstance().getCanSendLogs() == null) {
            UserMetrix.setCanSendLogs(false);
        } else {
            UserMetrix.setCanSendLogs(Configuration.getInstance()
                    .getCanSendLogs());
        }

        // Init scripting engine
        m2 = new ScriptEngineManager();

        // Init ruby factory
        sef = m2.getEngineByName("jruby").getFactory();

        // Initialize plugin manager
        PluginManager.getInstance();

        // Check for updates on startup
        JFrame mainFrame = Datavyu.getApplication().getMainFrame();
        updateWindow = new UpdateV(mainFrame, true);
        if (updateWindow.Available() && !updateWindow.IgnoreVersion()) {
            Datavyu.getApplication().show(updateWindow);
        }

    }

    /**
     * At startup create and show the main frame of the application.
     */
    @Override
    protected void startup() {

        // Make view the new view so we can keep track of it for hotkeys.
        VIEW = new DatavyuView(this);
        show(VIEW);
        VIEW.getFileSplitPane().setDividerLocation(0.75);

        // Now that datavyu is up - we may need to ask the user if can send
        // gather logs.
        if (Configuration.getInstance().getCanSendLogs() == null) {
            LOGGER.event("show usermetrix dialog");
            show(new UserMetrixV(VIEW.getFrame(), true));
        }

        // BugzID:435 - Correct size if a small size is detected.
        int width = (int) getMainFrame().getSize().getWidth();
        int height = (int) getMainFrame().getSize().getHeight();

        if ((width < INITMINX) || (height < INITMINY)) {
            int x = Math.max(width, INITMINX);
            int y = Math.max(height, INITMINY);
            getMainFrame().setSize(x, y);
        }

        addExitListener(new ExitListenerImpl());

        // Create video controller.
//        projectController = new ProjectController();
        dataController = VIEW.getSpreadsheetPanel().getDataController();

        final Dimension screenSize = Toolkit.getDefaultToolkit()
                .getScreenSize();
        int x = getView().getFrame().getX();

        // don't let the data viewer fall below the bottom of the primary
        // screen, but also don't let it creep up above the screen either
        int y = getView().getFrame().getY() + getView().getFrame().getHeight();
        y = (int) Math.max(Math.min(y,
                screenSize.getHeight() - dataController.getHeight()), 0);
        dataController.setLocation(x, y);
        show(dataController);
        VIEW.checkForAutosavedFile();

        // The DB we create by default doesn't really have any unsaved changes.
        projectController.getDB().markAsUnchanged();
        ready();
    }

    @Override
    protected void ready() {

        ready = true;
        if (commandLineFile != null) {
            getView().openExternalFile(new File(commandLineFile));
            commandLineFile = null;
        }
    }

    /**
     * Clean up after ourselves.
     */
    @Override
    public void shutdown() {
        if (getPlatform() == Platform.MAC && osxPressAndHoldEnabled) {
            setOSXPressAndHoldValue(true);
        }

        NativeLoader.cleanAllTmpFiles();
        nlm.purge();
        super.shutdown();
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     *
     * @param root The parent window.
     */
    @Override
    protected void configureWindow(final java.awt.Window root) {
    }

    /**
     * Asks the main frame to update its title.
     */
    @Override
    public void updateTitle() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (VIEW != null) {
                    VIEW.updateTitle();
                }
            }
        });
    }

    @Override
    public void show(final JDialog dialog) {

        if (windows == null) {
            windows = new Stack<Window>();
        }

        windows.push(dialog);
        super.show(dialog);
    }

    @Override
    public void show(final JFrame frame) {

        if (windows == null) {
            windows = new Stack<Window>();
        }

        windows.push(frame);
        super.show(frame);
    }

    public void resetApp() {
        closeOpenedWindows();
        this.dataController.dispose();
        this.dataController = new DataControllerV(Datavyu.getApplication()
                .getMainFrame(), false);
    }

    public void closeOpenedWindows() {

        if (windows == null) {
            windows = new Stack<Window>();
        }

        while (!windows.empty()) {
            Window window = windows.pop();
            window.setVisible(false);
            window.dispose();
        }
    }

    /**
     * All the supported platforms that Datavyu runs on.
     */
    public enum Platform {

        /**
         * Generic Mac platform. I.e. Tiger, Leopard, Snow Leopard.
         */
        MAC,

        /**
         * Generic windows platform. I.e. XP, vista, etc.
         */
        WINDOWS,

        /**
         * Generic Linux platform.
         */
        LINUX,

        /**
         * Unknown platform.
         */
        UNKNOWN
    }

    /**
     * Handles exit requests.
     */
    private class ExitListenerImpl implements ExitListener {

        /**
         * Default constructor.
         */
        public ExitListenerImpl() {
        }

        /**
         * Calls safeQuit to check if we can exit.
         *
         * @param arg0 The event generating the quit call.
         * @return True if the application can quit, false otherwise.
         */
        @Override
        public boolean canExit(final EventObject arg0) {
            return safeQuit();
        }

        /**
         * Cleanup would occur here, but we choose to do nothing for now.
         *
         * @param arg0 The event generating the quit call.
         */
        @Override
        public void willExit(final EventObject arg0) {
        }
    }
}
