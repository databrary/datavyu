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
import org.datavyu.Datavyu;
import org.jdesktop.application.LocalStorage;

import java.awt.*;
import java.io.*;
import java.util.function.Consumer;

/**
 * Configuration properties that are loaded from the settings.xml file in the resource folder.
 *
 * This file gets copied from the resource folder to the local temp directory where this application managers its
 * settings through the Swing Application Framework.
 */
public final class ConfigProperties implements Serializable {

    /** Unique identifier for this serial version */
    private static final long serialVersionUID = 4L;

    /** The colour to use for the border */
    public static final Color DEFAULT_BORDER_COLOUR = new Color(175, 175, 175);

    /** Default data font size */
    private static final float DEFAULT_DATA_FONT_SIZE = 14;

    /** Default label font size */
    private static final float DEFAULT_LABEL_FONT_SIZE = 12;

    /** Default font */
    private static final Font DEFAULT_SPREAD_SHEET_DATA_FONT = new Font("Arial", Font.PLAIN, 14);

    /** Spread sheet data font */
    private Font spreadSheetDataFont;

    /** Default font for labels */
    private static final Font DEFAULT_SPREAD_SHEET_LABEL_FONT = new Font("Arial", Font.PLAIN, 12);

    /** Spread sheet label font */
    private Font spreadSheetLabelFont;

    /** Default spreadsheet background color */
    private static final Color DEFAULT_SPREAD_SHEET_BACKGROUND_COLOR = new Color(249, 249, 249);

    /** Spread sheet background color */
    private Color spreadSheetBackgroundColor;

    /** Default spreadsheet foreground color */
    private static final Color DEFAULT_SPREAD_SHEET_FOREGROUND_COLOR = new Color(58, 58, 58);

    /** Spreadsheet foreground color */
    private Color spreadSheetForegroundColor;

    /** Default spreadsheet ordinal foreground colour */
    private static final Color DEFAULT_SPREAD_SHEET_ORDINAL_FOREGROUND_COLOR = new Color(175, 175, 175);

    /** Foreground color for the spreadsheet ordinal */
    private Color spreadSheetOrdinalForegroundColor;

    /** Default spreadsheet time stamp foreground color */
    private static final Color DEFAULT_SPREAD_SHEET_TIME_STAMP_FOREGROUND_COLOR = new Color(90, 90, 90);

    /** Foreground color of for spreadsheet timestamp */
    private Color spreadSheetTimeStampForegroundColor;

    /** Default spreadsheet selected color */
    private static final Color DEFAULT_SPREAD_SHEET_SELECTED_COLOR = new Color(176, 197, 227);

    /** Spreadsheet selection color */
    private Color spreadSheetSelectedColor;

    /** Default spreadsheet overlap color */
    private static final Color DEFAULT_SPREAD_SHEET_OVERLAP_COLOR = Color.RED;

    /** Spreadsheet overlap color */
    private Color spreadSheetOverlapColor;

    /** Default for the last chosen directory */
    private static final String DEFAULT_LAST_CHOSEN_DIRECTORY = System.getProperty("user.home");

    /** Last chosen directory */
    private String lastChosenDirectory;

    private static final String DEFAULT_IGNORE_VERSION = "";

    /** Version number to ignore for update reminders */
    private String ignoreVersion;

    /** Default value for warn on column names */
    private static final boolean DO_WARN_ON_COLUMN_NAMES = true;

    private static final boolean DEFAULT_FIRST_START = true;

    private static final boolean DEFAULT_SHARE_DATA = false;

    /** True if column name warnings should be displayed */
    private boolean doWarnOnIllegalColumnNames;

    /** True if is the first datavyu launch */
    private boolean firstStart;

    /** True if is the user acknowledge sharing data */
    private boolean shareData;

    /** Default on use of pre release */
    private static final boolean USE_PRE_RELEASE = false;

    /** True if pre releases are preferred */
    private boolean usePreRelease;

    /** Default for favorites folder */
    private static final String DEFAULT_FAVORITES_FOLDER = "favorites";

    /** Favorites folder */
    private String favoritesFolder;

    /** Default for support site url */
    private static final String DEFAULT_SUPPORT_SITE_URL = "http://www.datavyu.org/support";

    /** URL for the support site */
    private String supportSiteUrl;

    /** Default for the user guide url */
    private static final String DEFAULT_USER_GUIDE_URL = "http://www.datavyu.org/user-guide/index.html";

    /** URL for the user guide site */
    private String userGuideUrl;

    /** Default download url */
    private static final String DEFAULT_DOWNLOAD_URL = "http://www.datavyu.org/download.html";

    /** URL for the download url */
    private String downloadUrl;

    /** Default conversion url */
    private static final String DEFAULT_CONVERSION_URL = "https://handbrake.fr";

    /** URL for the conversion url */
    private String conversionUrl;

    /** This is the only instance for the configuration properties that is loaded at start-up */
    private static ConfigProperties configurationProperties = new ConfigProperties();

    /** The logger for this class */
    private static Logger logger = LogManager.getLogger(ConfigProperties.class);

    static {
        LocalStorage localStorage = Datavyu.getApplication().getContext().getLocalStorage();
        String localDirectory = localStorage.getDirectory().getAbsolutePath();

        // Copy the settings.xml file from the resources to the tmp folder where the Swing Application Framework
        // loads and stores the *.properties and *.xml files for this application with user defined properties.
        try {
            // It is important that the path into the resource with "/"
            File configurationFile = new File(localDirectory + "/" + Constants.CONFIGURATION_FILE);
            // Don't Copy the original settings.xml if already exists
            if (!configurationFile.exists()) {
                logger.info("Copying " + Constants.CONFIGURATION_FILE + " to " + localDirectory);

                configurationFile.getParentFile().mkdirs();
                InputStream inputStream = configurationProperties.getClass().getResourceAsStream(
                    "/" + Constants.CONFIGURATION_FILE);
                FileOutputStream fileOutputStream = new FileOutputStream(new File(localDirectory,
                    Constants.CONFIGURATION_FILE));
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                    fileOutputStream,
                    Constants.BUFFER_COPY_SIZE);
                int count;
                byte[] data = new byte[Constants.BUFFER_COPY_SIZE];
                while ((count = inputStream.read(data, 0, Constants.BUFFER_COPY_SIZE)) != -1) {
                    bufferedOutputStream.write(data, 0, count);
                }
                bufferedOutputStream.close();
                fileOutputStream.close();
                inputStream.close();
            }
        } catch (IOException io) {
            logger.error("Could not copy resource for settings " + io.getMessage());
        }

        // Try to load the configuration
        try {
            logger.info("Configuration loaded from directory " + localDirectory);
            configurationProperties = (ConfigProperties) localStorage.load(Constants.CONFIGURATION_FILE);
            logger.info("Loaded configuration properties: " + configurationProperties);
        } catch (IOException io) {
            logger.error("Unable to load configuration file " + io.getMessage());
            logger.info("Setting default properties.");
            configurationProperties = new ConfigProperties();
        }

        // If values are not set/loaded set their defaults
        if (!configurationProperties.hasSpreadSheetDataFont()) {
            configurationProperties.setSpreadSheetDataFont(DEFAULT_SPREAD_SHEET_DATA_FONT);
        }
        if (!configurationProperties.hasSpreadSheetLabelFont()) {
            configurationProperties.setSpreadSheetLabelFont(DEFAULT_SPREAD_SHEET_LABEL_FONT);
        }
        if (!configurationProperties.hasSpreadSheetSelectedColor()) {
            configurationProperties.setSpreadSheetSelectedColor(DEFAULT_SPREAD_SHEET_SELECTED_COLOR);
        }
        if (!configurationProperties.hasSpreadSheetOverlapColor()) {
            configurationProperties.setSpreadSheetOverlapColor(DEFAULT_SPREAD_SHEET_OVERLAP_COLOR);
        }
        if (!configurationProperties.hasIgnoreVersion()) {
            configurationProperties.setIgnoreVersion(DEFAULT_IGNORE_VERSION);
        }
        configurationProperties.setDoWarnOnIllegalColumnNames(DO_WARN_ON_COLUMN_NAMES);

        configurationProperties.setUsePreRelease(USE_PRE_RELEASE);
        if (!configurationProperties.hasFavoritesFolder()) {
            configurationProperties.setFavoritesFolder(DEFAULT_FAVORITES_FOLDER);
        }
        if (!configurationProperties.hasSupportSiteUrl()) {
            configurationProperties.setSupportSiteUrl(DEFAULT_SUPPORT_SITE_URL);
        }
        if (!configurationProperties.hasUserGuideUrl()) {
            configurationProperties.setUserGuideUrl(DEFAULT_USER_GUIDE_URL);
        }
        if (!configurationProperties.hasDownloadUrl()) {
            configurationProperties.setDownloadUrl(DEFAULT_DOWNLOAD_URL);
        }
        if (!configurationProperties.hasConversionUrl()) {
            configurationProperties.setConversionUrl(DEFAULT_CONVERSION_URL);
        }
        if (!configurationProperties.hasSpreadSheetOrdinalForegroundColor()) {
            configurationProperties.setSpreadSheetOrdinalForegroundColor(DEFAULT_SPREAD_SHEET_ORDINAL_FOREGROUND_COLOR);
        }
        if (!configurationProperties.hasSpreadSheetTimeStampForegroundColor()) {
            configurationProperties.setSpreadSheetTimeStampForegroundColor(DEFAULT_SPREAD_SHEET_TIME_STAMP_FOREGROUND_COLOR);
        }
        if (!configurationProperties.hasSpreadSheetBackgroundColor()) {
            configurationProperties.setSpreadSheetBackgroundColor(DEFAULT_SPREAD_SHEET_BACKGROUND_COLOR);
        }
        if (!configurationProperties.hasSpreadSheetForegroundColor()) {
            configurationProperties.setSpreadSheetForegroundColor(DEFAULT_SPREAD_SHEET_FOREGROUND_COLOR);
        }
        if (!configurationProperties.hasLastChosenDirectory()) {
            configurationProperties.setLastChosenDirectory(DEFAULT_LAST_CHOSEN_DIRECTORY);
        }
        try {
            loadFont(Constants.DEFAULT_FONT_FILE, (font) -> {
                configurationProperties.setSpreadSheetLabelFont(font.deriveFont(DEFAULT_LABEL_FONT_SIZE));
                //configurationProperties.setSpreadSheetDataFont(font.deriveFont(DEFAULT_DATA_FONT_SIZE));
            });
            loadFont(Constants.DEFAULT_CELL_FONT_FILE, (font) -> {
                configurationProperties.setSpreadSheetDataFont(font.deriveFont(DEFAULT_DATA_FONT_SIZE));
            });
        } catch (Exception e) {
            logger.error(e.toString());
        }

        // In all cases save this setting for the next run
        save();
    }

    /**
     * Set the default values for all properties.
     *
     * Public to be accessible by the XMLReader for JavaBeans to create an object instance.
     */
    public ConfigProperties() {}

    /**
     * Get the static instance for the configuration.
     *
     * @return The configuration properties.
     */
    public static ConfigProperties getInstance() {
        return configurationProperties;
    }

    private static void loadFont(String fontPath, Consumer<Font> consumer) throws Exception {
        try {
            consumer.accept(Font.createFont(Font.TRUETYPE_FONT,
                    configurationProperties.getClass().getResourceAsStream(fontPath)));
        } catch (Exception e) {
            throw new Exception("Error, unable to load font " + fontPath, e);
        }
    }

    /**
     * Saves the configuration properties in local storage using the Swing Application Framework.
     */
    public static void save() {
        try {
            LocalStorage ls = Datavyu.getApplication().getContext().getLocalStorage();
            logger.info("Saving configuration properties " + configurationProperties + " to: " + ls.getDirectory());
            ls.save(configurationProperties, Constants.CONFIGURATION_FILE);
        } catch (IOException e) {
            logger.error("Unable to save configuration " + e.getMessage());
        }
    }

    /**
     * Get the spread sheet data font.
     *
     * @return The spreadsheet data font.
     */
    public Font getSpreadSheetDataFont() {
        return spreadSheetDataFont;
    }

    /**
     * Sets the spread sheet data font.
     *
     * @param font to use for spread sheet data.
     */
    public void setSpreadSheetDataFont(final Font font) {
        spreadSheetDataFont = font;
    }

    /**
     * Sets the spread sheet data font size.
     *
     * @param size to set.
     */
    public void setSpreadSheetDataFontSize(final float size) {
        setSpreadSheetDataFont(getSpreadSheetDataFont().deriveFont(size));
    }

    /**
     * Did we set the spread sheet data font?
     *
     * @return Returns true if the spread sheet data font is set; otherwise false.
     */
    public boolean hasSpreadSheetDataFont() {
        return spreadSheetDataFont != null;
    }

    /**
     * Get the spread sheet label font.
     *
     * @return The spread sheet data font.
     */
    public Font getSpreadSheetLabelFont() {
        return spreadSheetLabelFont;
    }

    /**
     * Sets the spreadsheet data font.
     *
     * @param font to use for spread sheet data.
     */
    public void setSpreadSheetLabelFont(final Font font) {
        spreadSheetLabelFont = font;
    }

    /**
     * Did we set the spread sheet label font?
     *
     * @return True if the spread sheet label font; otherwise false.
     */
    public boolean hasSpreadSheetLabelFont() {
        return spreadSheetLabelFont != null;
    }

    /**
     * Get the spread sheet background color.
     *
     * @return spread sheet background color.
     */
    public Color getSpreadSheetBackgroundColor() {
        return spreadSheetBackgroundColor;
    }

    /**
     * Sets the spread sheet background colour.
     *
     * @param color to use for the spread sheet background.
     */
    public void setSpreadSheetBackgroundColor(final Color color) {
        spreadSheetBackgroundColor = color;
    }

    /**
     * Did we set the spread sheet background color?
     *
     * @return True if the spread sheet background color was set; otherwise False.
     */
    public boolean hasSpreadSheetBackgroundColor() {
        return spreadSheetBackgroundColor != null;
    }

    /**
     * Get the spread sheet foreground color.
     *
     * @return spread sheet foreground color.
     */
    public Color getSpreadSheetForegroundColor() {
        return spreadSheetForegroundColor;
    }

    /**
     * Sets the spreadsheet foreground color.
     *
     * @param color to use for the spreadsheet foreground.
     */
    public void setSpreadSheetForegroundColor(final Color color) {
        spreadSheetForegroundColor = color;
    }

    /**
     * Did we set the spread sheet foreground color?
     *
     * @return The spread sheet foreground color.
     */
    public boolean hasSpreadSheetForegroundColor() {
        return spreadSheetForegroundColor != null;
    }

    /**
     * Get the spread sheet ordinal foreground color.
     *
     * @return The spread sheet ordinal foreground colour.
     */
    public Color getSpreadSheetOrdinalForegroundColor() {
        return spreadSheetOrdinalForegroundColor;
    }

    /**
     * Sets the spreadsheet ordinal foreground color.
     *
     * @param color to use for the spreadsheet foreground.
     */
    public void setSpreadSheetOrdinalForegroundColor(final Color color) {
        spreadSheetOrdinalForegroundColor = color;
    }

    /**
     * Did we set the spread sheet ordinal foreground color?
     *
     * @return True if we set the spread sheet ordinal foreground color; otherwise False.
     */
    public boolean hasSpreadSheetOrdinalForegroundColor() {
        return spreadSheetOrdinalForegroundColor != null;
    }

    /**
     * Sets the spread sheet time tamp foreground color.
     *
     * @param color to use for the spread sheet foreground.
     */
    public void setSpreadSheetTimeStampForegroundColor(final Color color) {
        spreadSheetTimeStampForegroundColor = color;
    }

    /**
     * Did we set the spread sheet time stamp foreground color?
     *
     * @return True if we set the spread sheet time stamp foreground color; otherwise False.
     */
    public boolean hasSpreadSheetTimeStampForegroundColor() {
        return spreadSheetTimeStampForegroundColor != null;
    }

    /**
     * Get the spread sheet time stamp foreground color.
     *
     * @return The spread sheet timestamp foreground colour.
     */
    public Color getSpreadSheetTimeStampForegroundColor() {
        return spreadSheetTimeStampForegroundColor;
    }

    /**
     * Get the spread sheet selected color.
     *
     * @return The spread sheet selections color.
     */
    public Color getSpreadSheetSelectedColor() {
        return spreadSheetSelectedColor;
    }

    /**
     * Sets the spread sheet selected color.
     *
     * @param color to use for spread sheet selections.
     */
    public void setSpreadSheetSelectedColor(final Color color) {
        spreadSheetSelectedColor = color;
    }

    /**
     * Did we set the spread sheet selected color?
     *
     * @return True if we set the spread sheet selected color; otherwise False.
     */
    public boolean hasSpreadSheetSelectedColor() {
        return spreadSheetSelectedColor != null;
    }

    /**
     * Get the spread sheet overlap color.
     *
     * @return The spread sheet overlap color.
     */
    public Color getSpreadSheetOverlapColor() {
        return spreadSheetOverlapColor;
    }

    /**
     * Sets the spread sheet overlap color.
     *
     * @param color to use for spread sheet overlap color.
     */
    public void setSpreadSheetOverlapColor(final Color color) {
        spreadSheetOverlapColor = color;
    }

    /**
     * Did we set the spread sheet overlap color?
     *
     * @return True if we set the spread sheet overlap color; otherwise False.
     */
    public boolean hasSpreadSheetOverlapColor() {
        return spreadSheetOverlapColor != null;
    }

    /**
     * Get the last chosen directory.
     *
     * @return The last chosen directory.
     */
    public String getLastChosenDirectory() {
        return lastChosenDirectory;
    }

    /**
     * Sets the last chosen directory.
     *
     * @param directory last chosen.
     */
    public void setLastChosenDirectory(final String directory) {
        lastChosenDirectory = directory;
    }

    /**
     * Did we set the last chosen directory?
     *
     * @return True if the last chosen directory is set; otherwise False.
     */
    public boolean hasLastChosenDirectory() {
        return lastChosenDirectory != null;
    }

    /**
     * Get the ignore version.
     *
     * @return The ignore version.
     */
    public String getIgnoreVersion() {
        return ignoreVersion;
    }

    /**
     * Set the ignore version.
     *
     * @param version The ignore version that is set.
     */
    public void setIgnoreVersion(final String version) {
        ignoreVersion = version;
    }

    /**
     * Did we set the ignore version?
     *
     * @return True if we set the ignore version; otherwise False.
     */
    public boolean hasIgnoreVersion() {
        return ignoreVersion != null;
    }

    /**
     * Get the flag for that we set to warn on illegal column names.
     *
     * @return whether or not to display warnings for illegal column names
     */    
    public boolean getDoWarnOnIllegalColumnNames()
    {
        return doWarnOnIllegalColumnNames;
    }
    
    /**
     * Set the warn flag for illegal column names.
     *
     * @param doWarn whether or not to display warnings for illegal column names.
     */    
    public void setDoWarnOnIllegalColumnNames(final boolean doWarn) {
        doWarnOnIllegalColumnNames = doWarn;
    }

    /**
     * Get the first launch flag.
     *
     * @return whether or not is the first launch
     */
    public boolean getFirstStart() {
        return firstStart;
    }

    /**
     * Set the first start flag for first launch.
     *
     * @param isFirstStart whether or not to display warnings for illegal column names.
     */
    public void setFirstStart(final boolean isFirstStart) {
        firstStart = isFirstStart;
    }

    /**
     */
    public boolean getShareData() {
        return shareData;
    }

    /**
     */
    public void setShareData(final boolean share) {
        shareData = share;
    }

    /**
     * Get the user pre-release.
     *
     * @return The pre-release preference.
     */
    public boolean getUsePreRelease() {
        return usePreRelease;
    }

    /**
     * Set the user pre-release flag.
     *
     * @param usePreRelease True if pre-releases are preferred; otherwise False.
     */
    public void setUsePreRelease(boolean usePreRelease) {
        this.usePreRelease = usePreRelease;
    }

    /**
     * Get the favorites folder.
     *
     * @return favorites folder is returned.
     */
    public String getFavoritesFolder(){
        return favoritesFolder;
    }

    /**
     * Set the favorites folder.
     *
     * @param favoritesFolder for the favorites folder.
     */
    public void setFavoritesFolder(String favoritesFolder){
        this.favoritesFolder = favoritesFolder;
    }

    /**
     * Did we set the favorites folder?
     *
     * @return True if the favorites folder was set; otherwise False.
     */
    public boolean hasFavoritesFolder() {
        return favoritesFolder != null;
    }

    /**
     * Get the support site URL.
     *
     * @return The support site URL.
     */
    public String getSupportSiteUrl() {
        return supportSiteUrl;
    }

    /**
     * Set the support site URL.
     *
     * @param supportSiteUrl The support site URL.
     */
    public void setSupportSiteUrl(String supportSiteUrl) {
        this.supportSiteUrl = supportSiteUrl;
    }

    /**
     * Did we set the support site URL?
     *
     * @return True if we set support site ULR; otherwise False.
     */
    public boolean hasSupportSiteUrl() {
        return supportSiteUrl != null;
    }

    /**
     * Get the user guide url.
     *
     * @return The user guide url.
     */
    public String getUserGuideUrl() {
        return userGuideUrl;
    }

    /**
     * Set the user guide url.
     *
     * @param userGuideUrl The user guide url.
     */
    public void setUserGuideUrl(String userGuideUrl) {
        this.userGuideUrl = userGuideUrl;
    }

    /**
     * Did we set the user guide url?
     *
     * @return True if we set the user guide URL; otherwise False.
     */
    public boolean hasUserGuideUrl() {
        return userGuideUrl != null;
    }

    /**
     * Get the download url.
     *
     * @return Download url.
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Set the download url.
     *
     * @param downloadUrl The download url.
     */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Did we set the download url?
     *
     * @return True if the url is set; otherwise false.
     */
    public boolean hasDownloadUrl() {
        return downloadUrl != null;
    }

    /**
     * Get the conversion url.
     *
     * @return conversion url.
     */
    public String getConversionUrl() {
        return conversionUrl;
    }

    /**
     * Sets the conversion url.
     *
     * @param conversionUrl The conversion url.
     */
    public void setConversionUrl(String conversionUrl) {
        this.conversionUrl = conversionUrl;
    }

    /**
     * Did we set the conversion URL?
     *
     * @return True if we set the url; otherwise false.
     */
    public boolean hasConversionUrl() {
        return conversionUrl != null;
    }
}
