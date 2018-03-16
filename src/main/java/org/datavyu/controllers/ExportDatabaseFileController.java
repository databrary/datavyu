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
package org.datavyu.controllers;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.models.db.*;
import org.datavyu.util.StringUtils;
import org.datavyu.views.discrete.SpreadSheetPanel;
import org.datavyu.views.discrete.SpreadsheetColumn;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Controller for saving the database to disk.
 */
// TODO: Use StringBuilder for various strings that are put together in methods here!
public final class ExportDatabaseFileController {

    /** Logger for this class */
    private static Logger logger = LogManager.getLogger(ExportDatabaseFileController.class);

    /**
     * Saves the database to the specified destination in a CSV format.
     *
     * @param outFile The path of the file to use when writing to disk.
     * @param dataStore The data store to save as a CSV file.
     * @throws UserWarningException When unable to save the database as a CSV to
     *                              disk (usually because of permissions errors).
     */
    public void exportByFrame(final String outFile, final DataStore dataStore) throws UserWarningException {
        try {
            FileOutputStream fos = new FileOutputStream(outFile);
            PrintStream ps = new PrintStream(fos);

            List<Variable> variables = dataStore.getAllVariables();

            ArrayList<List<Cell>> cellCache = new ArrayList<>();
            int[] currentIndex = new int[variables.size()];

            // Get all of the cells from the DB and store them locally
            for (Variable v : variables) {
                cellCache.add(v.getCellsTemporally());
            }

            // Get first and last time point by sweeping over the cells
            long firstTime = Long.MAX_VALUE;
            long lastTime = 0;

            for (int i = 0; i < variables.size(); i++) {
                List<Cell> cells = cellCache.get(i);
                if (cells.isEmpty()) {
                    continue;
                }
                if (cells.get(0).getOnset() < firstTime) {
                    firstTime = cells.get(0).getOnset();
                }
                if (cells.get(0).getOffset() < firstTime) {
                    firstTime = cells.get(0).getOffset();
                }

                if (cells.get(cells.size() - 1).getOnset() > lastTime) {
                    lastTime = cells.get(cells.size() - 1).getOnset();
                }
                if (cells.get(cells.size() - 1).getOffset() > lastTime) {
                    lastTime = cells.get(cells.size() - 1).getOffset();
                }
            }

            // Now that we have the first and last time, we loop over it using
            // playback model's frameRate as step size. Fallback is 30.0
            double frameRate = 30.0;
            try{
                double fromDVC = Datavyu.getVideoController().getFrameRateController().getFrameRate();
                if (fromDVC > 1.0) {
                    frameRate = Datavyu.getVideoController().getFrameRateController().getFrameRate();
                }
            } catch(Exception e) {
                frameRate = 30.0;
                logger.error("Unable to get frame rate. Assuming value: " + frameRate);
            }
            long current_time = firstTime;

            // Print header
            String header = "nFrame,time,";
            for (Variable v : variables) {

                header += v.getName() + ".ordinal";
                header += "," + v.getName() + ".onset";
                header += "," + v.getName() + ".offset";

                // Test if the variable is a matrix. If it is, then we have to print out all of its arguments.
                if (v.getRootNode().type == Argument.Type.MATRIX) {
                    for (Argument a : v.getRootNode().childArguments) {
                        header += "," + v.getName() + "." + a.name;
                    }
                } else {
                    header += "," + v.getName() + ".value";
                }
                header += ',';
            }
            header = header.trim();

            // Write header
            ps.println(header);

            int nFrame = 1;
            while (current_time <= lastTime + 1000.0 / frameRate) {
                // Update the currentIndex list
                for (int i = 0; i < variables.size(); i++) {
                    if (!cellCache.get(i).isEmpty()) {
                        Cell c = cellCache.get(i).get(currentIndex[i]);
                        if (current_time > c.getOffset()) {

                            for (int j = currentIndex[i]; j < cellCache.get(i).size(); j++) {
                                Cell nextCell = cellCache.get(i).get(j);
                                if (current_time >= nextCell.getOnset()) {
                                    currentIndex[i] = j;
                                }
                            }
                        }
                    }
                }

                // Now print each frame as we loop through it
                String row = Integer.toString(nFrame) + "," + Long.toString(current_time) + ",";
                for (int i = 0; i < variables.size(); i++) {
                    if (!cellCache.get(i).isEmpty()) {

                        Cell cell = cellCache.get(i).get(currentIndex[i]);

                        if ((cell.getOnset() <= current_time && cell.getOffset() >= current_time) ||
                                (Math.abs(cell.getOffset() - cell.getOnset()) < 1000.0 / frameRate &&
                                        cell.getOnset() > current_time - 1000.0 / frameRate + 1 &&
                                        current_time >= cell.getOnset() &&
                                        cell.getOnset() < current_time + 1000.0 / frameRate - 1)) {

                            CellValue cellValue = cell.getCellValue();

                            // Print ordinal, onset, offset
                            row += Integer.toString(currentIndex[i] + 1) + "," +
                                    Long.toString(cell.getOnset()) + "," +
                                    Long.toString(cell.getOffset());


                            if (cellValue instanceof MatrixCellValue) {
                                // Then this is a matrix cellValue, get the sub arguments
                                MatrixCellValue mv = (MatrixCellValue) cellValue;
                                for (CellValue v : mv.getArguments()) {
                                    // Loop over each cellValue and print it with a comma separator
                                    row += "," + StringUtils.escapeCSVQuotes(v.toString());
                                }
                            } else {
                                // Otherwise just print the single argument
                                row += "," + StringUtils.escapeCSVQuotes(cell.getCellValue().toString());
                            }
                            row += ",";

                        } else {
                            // Figure out what to print if we don't have a cell here
                            CellValue cellValue = cell.getCellValue();

                            // Print ordinal, onset, offset
                            row += ",,";


                            if (cellValue instanceof MatrixCellValue) {
                                // Then this is a matrix cellValue, get the sub arguments
                                MatrixCellValue mv = (MatrixCellValue) cellValue;
                                for (CellValue v : mv.getArguments()) {
                                    // Loop over each cellValue and print it with a comma separator
                                    row += ",";
                                }
                            } else {
                                // Otherwise just print the single argument
                                row += ",";
                            }
                            row += ",";
                        }
                    }

                }
                ps.println(row);
                current_time += 1000.0 / frameRate;
                ++nFrame;
            }

            fos.close();
        } catch (IOException ie) {
            logger.error("Export failed. Error: ", ie);
            ResourceMap rMap = Application.getInstance(Datavyu.class).getContext().getResourceMap(Datavyu.class);
            throw new UserWarningException(rMap.getString("UnableToSave.message", outFile), ie);
        }
    }

    public void exportAsCells(final String outFile, final DataStore ds)
            throws UserWarningException {
        try {
            FileOutputStream outStream = new FileOutputStream(outFile);
            PrintStream ps = new PrintStream(outStream);

            // Get the variables, sort them, and cache the cells
            List<Variable> variables = ds.getAllVariables();
            Collections.sort(variables, new Comparator<Variable>() {
                @Override
                public int compare(Variable o1, Variable o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            ArrayList<List<Cell>> cellCache = new ArrayList<>();

            int max_length = 0;
            // Get all of the cells from the DB and store them locally
            for (Variable v : variables) {
                cellCache.add(v.getCellsTemporally());
                if (v.getCells().size() > max_length) {
                    max_length = v.getCells().size();
                }
            }

            // Print header
            String header = "";

            List<Integer> arglengths = new ArrayList<Integer>();
            for (Variable v : variables) {

                header += v.getName() + ".ordinal";
                header += "," + v.getName() + ".onset";
                header += "," + v.getName() + ".offset,";

                // Test if the variable is a matrix. If it is, then
                // we have to print out all of its arguments.
                if (v.getRootNode().type == Argument.Type.MATRIX) {
                    for (Argument a : v.getRootNode().childArguments) {
                        header += v.getName() + "." + a.name + ",";
                    }
                    arglengths.add(v.getRootNode().childArguments.size() + 3);
                } else {
                    header += v.getName() + ".value,";
                    arglengths.add(4);
                }
            }
            header = header.trim();

            // Write header
            ps.println(header);

            // Now get the column that has the most cells, we are going to use
            // that number as the number of iterations to loop over everything
            // printing blanks if that column does not have a cell there
            StringBuilder row;
            for (int i = 0; i < max_length; i++) {
                row = new StringBuilder();
                for (int j = 0; j < variables.size(); j++) {
                    Variable v = variables.get(j);
                    if (cellCache.get(j).size() > i) {
                        // Print the cell
                        Cell c = cellCache.get(j).get(i);
                        row.append(i);
                        row.append(",");
                        row.append(c.getOnset());
                        row.append(",");
                        row.append(c.getOffset());
                        row.append(",");
                        if (v.getRootNode().type == Argument.Type.MATRIX) {
                            for (int k = 0; k < v.getRootNode().childArguments.size(); k++) {
                                row.append(StringUtils.escapeCSVQuotes(c.getMatrixValue(k).toString()));
                                row.append(",");
                            }
                        }
                        else{
                            row.append(StringUtils.escapeCSVQuotes(c.getCellValue().toString()));
                            row.append(",");
                        }
                    } else {
                        // Print a placeholder: we are out of cells
                        for (int k = 0; k < arglengths.get(j); k++) {
                            row.append(",");
                        }
                    }

                }
                ps.println(row);
            }
            ps.flush();
            ps.close();
            outStream.flush();
            outStream.close();
        } catch (IOException ie) {
            logger.error("Export as cells failed. Error: ", ie);
            ResourceMap rMap = Application.getInstance(Datavyu.class).getContext().getResourceMap(Datavyu.class);
            throw new UserWarningException(rMap.getString("UnableToSave.message", outFile), ie);
        }
    }

    /**
     * Serialize the database to the specified stream in a CSV format.
     *
     * @param outStream The stream to use when serializing.
     * @param ds        The datastore to save as a CSV file.
     * @throws UserWarningException When unable to save the database as a CSV to
     *                              disk (usually because of permissions errors).
     */
    public void exportAsCSV(final OutputStream outStream, final DataStore ds)
            throws UserWarningException {
        logger.info("save database as CSV to stream");

        // Dump out an identifier for the version of file.
        PrintStream ps = new PrintStream(outStream);
        ps.println("#4");

        for (Variable variable : ds.getAllVariables()) {
            ps.printf("%s (%s,%s,%s)",
                    StringUtils.escapeCSV(variable.getName()),
                    variable.getRootNode().type,
                    !variable.isHidden(),
                    "");

            if (variable.getRootNode().type == Argument.Type.MATRIX) {
                ps.print('-');

                int numArgs = 0;
                for (Argument arg : variable.getRootNode().childArguments) {
                    ps.printf("%s|%s",
                            StringUtils.escapeCSV(arg.name),
                            arg.type);

                    if (numArgs < (variable.getRootNode().childArguments.size() - 1)) {
                        ps.print(',');
                    }
                    numArgs++;
                }
            }

            ps.println();

            for (Cell cell : variable.getCells()) {
                ps.printf("%s,%s,%s",
                        cell.getOnsetString(),
                        cell.getOffsetString(),
                        cell.getValueAsString());
                ps.println();
            }
        }
    }

    /**
     * Save a Datavyu Spreadseet in a JSON File.
     *
     * @param dbFileName Target File
     * @param dataStore DataStore to be saved as JSON
     */

    public void exportAsJSON(String dbFileName, DataStore dataStore) throws UserWarningException{
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory f = mapper.getFactory();
        File jsonFile = new File(dbFileName);
        try {
            JsonGenerator g = f.createGenerator(jsonFile, JsonEncoding.UTF8);
            g.setPrettyPrinter(new DefaultPrettyPrinter());
            //Start a Spreadsheet Object
            g.writeStartObject();
            //Spreadsheet name Field
//            g.writeStringField("name", dataStore.getName());

            //Start an Array of Passes (Column(Spreadsheet)/Variable(DataStore))
            g.writeArrayFieldStart("passes");
            for (Variable column : dataStore.getAllVariables()) {
                // Start an Object for each Pass(Column/Variable)
                g.writeStartObject();
                // Pass(Column/Variable) name
                g.writeStringField("name", column.getName());

                g.writeStringField("type",column.getRootNode().type.toString());

                g.writeObjectFieldStart("arguments");

                column.getRootNode().childArguments.forEach(argument -> {
                    try {
                        g.writeStringField(argument.type.name(), argument.name);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                g.writeEndObject();

                //Start an Array of Cells
                g.writeArrayFieldStart("cells");
                int count = 1;
                for (Cell cell : column.getCellsTemporally()) {
                    // Start an Object for each Cell
                    g.writeStartObject();

                    g.writeNumberField("id", count);
                    g.writeStringField("onset", cell.getOnsetString());
                    g.writeStringField("offset", cell.getOffsetString());
                    g.writeArrayFieldStart("values");

                    if (column.getRootNode().type == Argument.Type.MATRIX) {
                        for (int k = 0; k < column.getRootNode().childArguments.size(); k++) {
                            g.writeString(cell.getMatrixValue(k).toString());
                        }
                    } else {
                        g.writeString(cell.getCellValue().toString());
                    }

                    g.writeEndArray();
                    // End Cell Object
                    g.writeEndObject();
                    count++;
                }
                // End Cells Array
                g.writeEndArray();
                // End Pass Object
                g.writeEndObject();
            }
            // End the Passes Array
            g.writeEndArray();

            //End a Spreadsheet Object
            g.writeEndObject();

            g.close();
            logger.info("JSON File has been successfully saved");

        } catch (IOException e) {
            logger.error("Export as JSON failed. Error: ", e);
            ResourceMap rMap = Application.getInstance(Datavyu.class).getContext().getResourceMap(Datavyu.class);
            throw new UserWarningException(rMap.getString("UnableToSave.message", dbFileName), e);
        }

    }
}
