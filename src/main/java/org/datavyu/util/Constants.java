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

/**
 * Common constants.
 */
public final class Constants {

    /** The default number of ticks per second to use. */
    public static final int TICKS_PER_SECOND = 1000;

    /** Seed value for generating hash codes. */
    public static final int SEED1 = 3;

    /** Seed value for generating hash codes. */
    public static final int SEED2 = 7;

    /** The margin size to use down the sides of the columns. */
    public static final int BORDER_SIZE = 1;

    /** The name of the configuration file */
    public static final String CONFIGURATION_FILE = "settings.xml";

    /** The name for the project file history */
    public static final String PROJECT_FILE_HISTORY = "projectHistory.xml";

    /** The name for the script file history */
    public static final String SCRIPT_FILE_HISTORY = "scriptHistory.xml";

    /** Font file name */
    public static final String DEFAULT_FONT_FILE = "/fonts/DejaVuSansCondensed.ttf";

    /** Cell Unicode Font file name */
    public static final String DEFAULT_CELL_FONT_FILE = "/fonts/unifont-10.0.07.ttf";

    /** Buffer size when copying files from streams */
    public static final int BUFFER_COPY_SIZE = 16*1024; // 16 kB
}
