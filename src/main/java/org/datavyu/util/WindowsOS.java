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
package org.datavyu.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * This class provides access to Windows operating system specific properties, such as file associations, key remapping,
 * and provides interfaces to execute commands natively.
 */
public class WindowsOS {

    private static Logger logger = LogManager.getLogger();

    /**
     * Get options for a dialog in Windows with three responses.
     *
     * @param yesOption The yes option.
     * @param noOption The no option.
     * @param cancelOption The cancel option.
     *
     * @return A string array with three options: yesOption, noOption, cancelOption.
     */
    public static String[] getOptions(String yesOption, String noOption, String cancelOption) {
        return new String[] {yesOption, noOption, cancelOption};
    }

    /**
     * Get options for a dialog in Windows with two responses.
     *
     * @param defaultOption The default option.
     * @param alternativeOption The alternative option.
     *
     * @return A string array with the options: alternativeOption, defaultOption.
     */
    public static String[] getOptions(String defaultOption, String alternativeOption) {
        return new String[]{alternativeOption, defaultOption};
    }

    /**
     * Log the contents from the reader to info.
     *
     * @param reader The reader with the contents.
     * @throws IOException This exception.
     */
    private static void logReaderToInfo(final BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            logger.info(line);
        }
    }

    /**
     * Log the contents of the reader to error.
     *
     * @param reader The reader with the contents.
     * @throws IOException This exception.
     */
    private static void logReaderToError(final BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            logger.error(line);
        }
    }

    /**
     * Execute the command line argument 'cmd' and log stderr as errors in case of failure. In case of success return a
     * reader to the returned string.
     *
     * @param cmd The command line argument.
     *
     * @return BufferedReader with the output from stdout.
     */
    public static BufferedReader execAndReturn(final String cmd) {
        logger.info("Execute command: " + cmd);
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            if (process.exitValue() == 0) {
                return new BufferedReader(new InputStreamReader(process.getInputStream()));
            } else {
                logReaderToError(new BufferedReader(new InputStreamReader(process.getErrorStream())));
            }
        } catch (Exception e) {
            logger.error("Command line error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Execute the command line argument 'cmd' and log stderr as errors in case of a failure and stdout as info in case
     * of success.
     *
     * @param cmd The command line argument.
     */
    public static void exec(final String cmd) {
        logger.info("Execute command: " + cmd);
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            if (process.exitValue() == 0) {
                logReaderToInfo(new BufferedReader(new InputStreamReader(process.getInputStream())));
            } else {
                logReaderToError(new BufferedReader(new InputStreamReader(process.getErrorStream())));
            }
        } catch (Exception e) {
            logger.error("Command line error: " + e.getMessage());
        }
    }

    /**
     * Remap broken character code for windows.  Not guaranteed to work for all characters.
     *
     * @param brokenChar The broken character code.
     *
     * @return The remapped character code which is: brokenChar + 64.
     */
    public static char remapKeyChar(final char brokenChar) {
        return (char) ( ((int)brokenChar) + 64);
    }

    /**
     * Associate an extension with a program name. This only works if the access rights are granted to this program.
     *
     * @param extension The extension.
     *
     * @param programName The program name with path.
     */
    public static void associate(final String extension, final String programName) {
        // Set association and file type in one line, see https://technet.microsoft.com/en-us/library/ff687021.aspx
        String cmd = "cmd /c assoc " + extension + "=opffile & cmd /c ftype opffile=\"" + programName + "\" \"%1\"";
        exec(cmd);
    }

    /**
     * Get a listing of existing associations for a given extension.
     *
     * @param extension The extension.
     *
     * @return A listing of associates programs.
     */
    public static List<String> getAssociations(final String extension) {
        final String cmd = "cmd /c assoc " + extension;
        BufferedReader reader = execAndReturn(cmd);
        List<String> lines = new LinkedList<>();
        if (reader != null) {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException io) {
                logger.warn("Could not read output: " + io.getMessage());
            }
        }
        return lines;
    }

    /**
     * Get the current working directory for this source code location.
     *
     * @return The file to the current working directory.
     */
    public static File cwd() {
        return new File(WindowsOS.class.getProtectionDomain().getCodeSource().getLocation().getFile());
    }
}
