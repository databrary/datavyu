package org.datavyu.util.FileFilters;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class JSONFilter extends FileFilter {

    public static final JSONFilter INSTANCE = new JSONFilter();

    private JSONFilter(){ }

    /**
     * @return The description of the file filter.
     */
    @Override
    public String getDescription() {
        return "JavaScript Object Notation (*.json)";
    }

    /**
     * Determines if the file filter will accept the supplied file.
     *
     * @param file The file to check if this file will accept.
     * @return true if the file is to be accepted, false otherwise.
     */
    @Override
    public boolean accept(final File file) {
        return (file.getName().endsWith(".json") || file.isDirectory());
    }
}
