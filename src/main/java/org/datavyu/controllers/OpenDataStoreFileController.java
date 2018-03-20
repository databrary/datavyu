/**
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.datavyu.controllers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.datavyu.Datavyu;
import org.datavyu.models.db.*;
import org.datavyu.util.ConfigurationProperties;
import org.datavyu.views.discrete.SpreadSheetPanel;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.min;

/**
 * Controller for opening a data store from disk.
 */
public final class OpenDataStoreFileController {

    /** The index of the ONSET timestamp in the CSV line */
    private static final int DATA_ONSET = 0;

    /** The index of the OFFSET timestamp in the CSV line */
    private static final int DATA_OFFSET = 1;

    /** The start of the data arguments */
    private static final int DATA_INDEX = 2;

    /** The logger instance for this class */
    private static Logger logger = LogManager.getLogger(OpenDataStoreFileController.class);

    /** The number of variables that were parsed */
    private int numVariablesParsed = 0;

    /**
     * Opens a data store
     *
     * @param dataStoreFile The source file to open.
     * @return Returns a populated data store on success, otherwise null
     */
    public DataStore open(final File dataStoreFile) {
        String inputFile = dataStoreFile.toString().toLowerCase();
        // If file ends with CSV -- treat as column separated file -- otherwise as open shapa file
        return inputFile.endsWith(".csv") ? openAsCsv(dataStoreFile) : openAsMacShapa(dataStoreFile);
    }

    /**
     * This method treats a file as a MacSHAPA database file and attempts to
     * populate the database with data.
     *
     * @param dataStoreFile The source file to use when populating the database.
     * @return populated data store on success, null otherwise.
     */
    private DataStore openAsMacShapa(final File dataStoreFile) {
        logger.error("Open as mac SHAPA DB is not implemented.");
        return null;
    }

    /**
     * This method parses a CSV file and populates the data store and spread sheet with data
     *
     * @param dataStoreFile The source file to use when populating the data store
     * @return populated data store on success, null otherwise
     */
    private DataStore openAsCsv(final File dataStoreFile) {
        try {
            logger.info("Open CSV data store from file: '" + dataStoreFile.getAbsolutePath() + "'.");
            FileInputStream fis = new FileInputStream(dataStoreFile);
            DataStore result = openAsCsv(fis);
            fis.close();
            return result;
        } catch (Exception e) {
            logger.error("Unable to open CSV file: '" + dataStoreFile.getAbsolutePath() + "'. Error: ", e);
        }
        return null;
    }

    /**
     * This method parses a CSV input stream and populates the data store and spread sheet with data. The caller is
     * responsible for managing the input stream aka opening and closing it
     *
     * @param inputStream The input stream used to deserialize the data store
     * @return Populated data store on success; otherwise null
     */
    protected DataStore openAsCsv(final InputStream inputStream) {
        try {
            logger.info("Open csv data base from input stream");

            DataStore db = DataStoreFactory.newDataStore();
            db.setTitleNotifier(Datavyu.getApplication());
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader csvFile = new BufferedReader(isr);

            // Read each line of the CSV file.
            String line = csvFile.readLine();

            // If we have a version identifier parse the file using the schema
            // that matches that identifier.
            if ("#4".equalsIgnoreCase(line)) {

                //Version 4 includes a comment for columns.
                line = csvFile.readLine();
                while (line != null) {
                    line = parseVariable(csvFile, line, db, "#4");
                }
                if (!db.getExemptionVariables().isEmpty()) {
                    logger.info("We have excemption variables");
                    SwingUtilities.invokeLater(new NameWarning(db.getExemptionVariables()));
                }
            } else if ("#3".equalsIgnoreCase(line)) {

                //Version 3 includes column visible status after the column type
                line = csvFile.readLine();
                while (line != null) {
                    line = parseVariable(csvFile, line, db, "#3");
                }
            } else if ("#2".equalsIgnoreCase(line)) {

                line = csvFile.readLine();
                while (line != null) {
                    line = parseVariable(csvFile, line, db);
                }

            } else {

                // Use the original schema to load the file - just variables,
                // and no escape characters.
                while (line != null) {
                    line = parseVariable(csvFile, line, db);
                }
            }

            csvFile.close();
            isr.close();

            return db;
        } catch (IOException e) {
            logger.error("Unable to read line from CSV file. Error: ", e);
        } catch (UserWarningException e) {
            logger.error("Unable to create new variable. Error: ", e);
        }

        // Error encountered - return null.
        return null;
    }

    public void importJSONToSpreadsheet(File file, SpreadSheetPanel spreadSheet) throws UserWarningException,  JsonParseException, IOException {
        if (file.getAbsolutePath().endsWith(".json")) {
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(file);

            while (!parser.isClosed()) {
                JsonToken token = parser.nextToken();
                if (token == null) { break; }

                if (JsonToken.START_OBJECT.equals(token)) {

                    token = parser.nextToken();
                    if (JsonToken.FIELD_NAME.equals(token) && "passes".equals(parser.getCurrentName())) {

                        DataStore dataStore = spreadSheet.getDataStore();

                        token = parser.nextToken();
                        if (!JsonToken.START_ARRAY.equals(token)) { break; }

                        while (!JsonToken.END_ARRAY.equals(token)) { // for each pass

                            token = parser.nextToken();
                            if (token == null) { break; }
                            if (!JsonToken.START_OBJECT.equals(token)) { break; }

                            token = parser.nextToken();
                            if (JsonToken.FIELD_NAME.equals(token) && "name".equals(parser.getCurrentName())) {

                                token = parser.nextToken();
                                String columnName = parser.getValueAsString();

                                // Check if we already have the same column name
                                if (spreadSheet.getDataStore().getVariable(columnName) == null) {

                                    token = parser.nextToken();
                                    if (JsonToken.FIELD_NAME.equals(token) && "type".equals(parser.getCurrentName())) {
                                        token = parser.nextToken();
                                        Argument.Type variableType = getVarType(parser.getValueAsString());

                                        if (variableType != null) {
                                            token = parser.nextToken();
                                            if (JsonToken.FIELD_NAME.equals(token) && "arguments".equals(parser
                                                    .getCurrentName())) {
                                                token = parser.nextToken();
                                                if (token == null) { break; }
                                                if (!JsonToken.START_OBJECT.equals(token)) { break; }

                                                Variable newColumn = dataStore.createVariable(columnName,
                                                        variableType, true);
                                                Argument variableArg = newColumn.getRootNode();
                                                variableArg.clearChildArguments();

                                                while (!JsonToken.END_OBJECT.equals(token)) {
                                                    token = parser.nextToken();
                                                    if (token == null) { break; }
                                                    if (JsonToken.START_OBJECT.equals(token) || JsonToken
                                                            .START_OBJECT.equals(token) || JsonToken.END_OBJECT
                                                            .equals(token)) {
                                                        break;
                                                    }

                                                    token = parser.nextToken();
                                                    if (variableType == Argument.Type.TEXT) {
                                                        variableArg.name = parser.getValueAsString();
                                                    }
                                                    if (variableType == Argument.Type.NOMINAL) {
                                                        variableArg.name = parser.getValueAsString();
                                                    }
                                                    if (variableType == Argument.Type.MATRIX) {
                                                        if (getVarType(parser.getCurrentName()) != null) {
                                                            variableArg.childArguments.add(new Argument(parser
                                                                    .getValueAsString(), getVarType(parser
                                                                    .getCurrentName())));
                                                        } else {
                                                            logger.warn("Unknown argument ('" + parser.getCurrentName
                                                                    () + "' line " + parser.getCurrentLocation()
                                                                    .getLineNr() + " ): was expecting " + "argument "
                                                                    + "(NOMINAL, TEXT or MATRIX)" + " field name");
                                                            break;
                                                        }
                                                    }
                                                }
                                                newColumn.setRootNode(variableArg);

                                                token = parser.nextToken();
                                                if (JsonToken.FIELD_NAME.equals(token) && "cells".equals(parser
                                                        .getCurrentName())) {

                                                    token = parser.nextToken();
                                                    if (!JsonToken.START_ARRAY.equals(token)) { break; }


                                                    while (!JsonToken.END_ARRAY.equals(token)) { // for each cell
                                                        token = parser.nextToken();
                                                        if (!JsonToken.START_OBJECT.equals(token)) { break; }
                                                        if (JsonToken.END_ARRAY.equals(token)) { break; }
                                                        if (token == null) { break; }

                                                        Cell newCell = newColumn.createCell();

                                                        token = parser.nextToken();
                                                        if (JsonToken.FIELD_NAME.equals(token) && "id".equals(parser
                                                                .getCurrentName())) {
                                                            token = parser.nextToken(); // we skip the id value
                                                            token = parser.nextToken();
                                                            if (JsonToken.FIELD_NAME.equals(token) && "onset".equals
                                                                    (parser.getCurrentName())) {
                                                                token = parser.nextToken();
                                                                String cellONSET = parser.getValueAsString();
                                                                newCell.setOnset(cellONSET);

                                                                token = parser.nextToken();
                                                                if (JsonToken.FIELD_NAME.equals(token) && "offset"
                                                                        .equals(parser.getCurrentName())) {
                                                                    token = parser.nextToken();
                                                                    String cellOFFSET = parser.getValueAsString();
                                                                    newCell.setOffset(cellOFFSET);

                                                                    token = parser.nextToken();
                                                                    if (JsonToken.FIELD_NAME.equals(token) &&
                                                                            "values".equals(parser.getCurrentName())) {
                                                                        token = parser.nextToken();
                                                                        if (!JsonToken.START_ARRAY.equals(token)) {
                                                                            break;
                                                                        }

                                                                        token = parser.nextToken();
                                                                        int k = 0; // keep track of the matrix value
                                                                        // number
                                                                        while (!JsonToken.END_ARRAY.equals(token)) {
                                                                            if (token == null) { break; }

                                                                            if (newColumn.getRootNode().type ==
                                                                                    Argument.Type.MATRIX) {

                                                                                newCell.setMatrixValue(k, parser
                                                                                        .getValueAsString());
                                                                                k++;
                                                                            } else {
                                                                                newCell.getCellValue().set(parser
                                                                                        .getValueAsString());
                                                                            }
                                                                            token = parser.nextToken();
                                                                        }
                                                                    } else {
                                                                        dataStore.removeVariable(newColumn);
                                                                        String msg = "Unexpected character ('" +
                                                                                parser.getCurrentName() + "' line " +
                                                                                parser.getCurrentLocation().getLineNr
                                                                                        () + " ): " + "was expecting " +
                                                                                "" + "values field name";
                                                                        logger.warn(msg);
                                                                        Datavyu.getApplication().showWarningDialog(msg);
                                                                        break;
                                                                    }
                                                                } else {
                                                                    dataStore.removeVariable(newColumn);
                                                                    String msg = "Unexpected character ('" + parser
                                                                            .getCurrentName() + "' line " + parser
                                                                            .getCurrentLocation().getLineNr() + " ): " +
                                                                            "" + "was " + "expecting offset field " +
                                                                            "name";
                                                                    logger.warn(msg);
                                                                    Datavyu.getApplication().showWarningDialog(msg);
                                                                    break;
                                                                }
                                                            } else {
                                                                dataStore.removeVariable(newColumn);
                                                                String msg = "Unexpected character ('" + parser
                                                                        .getCurrentName() + "' line " + parser
                                                                        .getCurrentLocation().getLineNr() + " ): was " +
                                                                        "" + "" + "expecting onset field name";
                                                                logger.warn(msg);
                                                                Datavyu.getApplication().showWarningDialog(msg);
                                                                break;
                                                            }
                                                        } else {
                                                            dataStore.removeVariable(newColumn);
                                                            String msg = "Unexpected character ('" + parser
                                                                    .getCurrentName() + "' line " + parser
                                                                    .getCurrentLocation().getLineNr() + " ): was " +
                                                                    "expecting " + "id field name";
                                                            logger.warn(msg);
                                                            Datavyu.getApplication().showWarningDialog(msg);
                                                            break;
                                                        }
                                                        token = parser.nextToken();
                                                    }
                                                } else {
                                                    dataStore.removeVariable(newColumn);
                                                    String msg = "Unexpected character ('" + parser.getCurrentName() +
                                                            "' line " + parser.getCurrentLocation().getLineNr() + " )" +
                                                            ": was expecting cells " + "field name";
                                                    logger.warn(msg);
                                                    Datavyu.getApplication().showWarningDialog(msg);
                                                    break;
                                                    }
                                            } else {
                                                String msg = "Unexpected character ('" + parser.getCurrentName() + "' "
                                                        + "line " + parser.getCurrentLocation().getLineNr() +
                                                        " ):" + " was expecting arguments field name";
                                                logger.warn(msg);
                                                Datavyu.getApplication().showWarningDialog(msg);
                                                break;
                                            }
                                        } else {
                                            String msg = "Unknown argument ('" + parser.getCurrentName() + "' line " +
                                                    parser.getCurrentLocation().getLineNr() + " ): was expecting " +
                                                    "argument (NOMINAL, TEXT or MATRIX) field name";
                                            logger.warn(msg);
                                            Datavyu.getApplication().showWarningDialog(msg);
                                            break;
                                        }
                                    } else {
                                        String msg = "Unexpected character ('" + parser.getCurrentName() + "' line " +
                                                parser.getCurrentLocation().getLineNr() + " ): was expecting type " +
                                                "field name";
                                        logger.warn(msg);
                                        Datavyu.getApplication().showWarningDialog(msg);
                                        break;
                                    }
                                } else {
                                    String msg = "The column " + columnName + " already exists in the current" + " " +
                                            "spreadsheet";
                                    logger.warn(msg);
                                    Datavyu.getApplication().showWarningDialog(msg);
                                    break;
                                }
                            } else {
                                String msg = "Unexpected character ('" + parser.getCurrentName() + "' line " + parser
                                        .getCurrentLocation().getLineNr() + " ): was expecting name field name";
                                logger.warn(msg);
                                Datavyu.getApplication().showWarningDialog(msg);
                                break;
                            }
                            token = parser.nextToken();
                        }
                        parser.close();
                    } else {
                        String msg = "Unexpected character ('" + parser.getCurrentName() + "' line " + parser
                                .getCurrentLocation().getLineNr() + " ): was expecting passes field name";
                        logger.error(msg);
                        Datavyu.getApplication().showWarningDialog(msg);
                        break;
                    }
                } else {
                    String msg ="Unexpected character ('" + parser.getCurrentName() + "' line " + parser
                            .getCurrentLocation().getLineNr() + " ): was expecting '{'";
                    logger.error(msg);
                    Datavyu.getApplication().showWarningDialog(msg);
                    break;
                }
            }
        } else {
            String msg = "The selected file is a not a JSON format";
            logger.error(msg);
            Datavyu.getApplication().showWarningDialog(msg);
        }
    }

    /**
     * Strip escape characters from a line of text
     *
     * @param line The line of text to strip escape characters from
     * @return The line free of escape characters, i.e. '\'
     */
    private String stripEscapeCharacters(final String line) {
        String result = null;

        if (line != null) {
            result = "";

            for (int i = 0; i < line.length(); i++) {

                if (i < (line.length() - 1)) {

                    if ((line.charAt(i) == '\\')
                            && (line.charAt(i + 1) == '\\')) {
                        char[] buff = {'\\'};
                        result = result.concat(new String(buff));

                        // Move over the escape character.
                        i++;
                    } else if ((line.charAt(i) == '\\')
                            && (line.charAt(i + 1) == ',')) {
                        char[] buff = {','};
                        result = result.concat(new String(buff));

                        // Move over the escape character.
                        i++;
                    } else if ((line.charAt(i) == '\\')
                            && (line.charAt(i + 1) == '-')) {
                        char[] buff = {'-'};
                        result = result.concat(new String(buff));

                        // Move over the escape character.
                        i++;
                    } else {
                        result += line.charAt(i);
                    }
                } else {
                    result += line.charAt(i);
                }
            }
        }

        return result;
    }

    /**
     * Method to create data values for the formal arguments of a vocab element
     *
     * @param tokens The array of string tokens
     * @param startIndex The start index
     * @param destValue The destination value that we are populating
     */
    private void parseFormalArgs(final String[] tokens, final int startIndex, final Argument destPattern,
                                 final MatrixCellValue destValue) {

        // Check to see if the list of tokens we have here is correct.
        // If it is not, then mark an error state and do our best to parse.
        // Fill in missing info with a missing value.

        List<CellValue> args = destValue.getArguments();

        int endIndex = tokens.length;
        boolean parseError = false;
        if (args.size() != tokens.length - startIndex) {
            // We have a problem. Arguments are of different length.
            // Get as much from the string as we can.

            parseError = true; // TODO: Do something with this: warning, user warning exception?
            endIndex = min(tokens.length, destValue.getArguments().size() + startIndex);
        }

        for (int tokenIndex = startIndex; tokenIndex < endIndex; tokenIndex++) {
            int argIndex = tokenIndex - startIndex;
            Argument fa = destPattern.childArguments.get(argIndex);
            boolean emptyArg = false;

            // If the field doesn't contain anything or matches the FargName
            // we consider the argument to be 'empty'. 
            if ((tokens[tokenIndex].length() == 0) || tokens[tokenIndex].equals("<" + fa.name + ">")) {
                emptyArg = true;
                tokens[tokenIndex] = ""; //set <placeholder> to empty string. 
            }

            tokens[tokenIndex] = tokens[tokenIndex].trim(); //is this desirable?
            destValue.getArguments().get(argIndex).set(tokens[tokenIndex]);
        }
    }

    /**
     * Method to invoke when we encounter a block of text in the CSV file that is the contents of a matrix variable
     *
     * @param csvReader The csvReader we are currently parsing
     * @param variable The variable that we will be adding cells too.
     * @param argument The matrix template we are using when parsing individual matrix elements to put in the
     *                 spread sheet
     * @return The next line in the file that is not part of the block of text in the CSV file
     * @throws IOException If unable to read the file correctly
     */
    private String parseMatrixVariable(final BufferedReader csvReader, final Variable variable, final Argument argument)
            throws IOException {

        // TODO: Check if we need the argument possibly for proper error reporting
        String line = csvReader.readLine();

        while ((line != null) && isCell(line)) {

            ArrayList tokensList = new ArrayList<String>();
            String[] onsetOffsetVals = line.split(",", 3);
            tokensList.add(onsetOffsetVals[0]); //onset
            tokensList.add(onsetOffsetVals[1]); //offset

            String valuesStr = onsetOffsetVals[2];
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < valuesStr.length(); i++) {
                char cur = valuesStr.charAt(i);
                if (cur == '\\') {
                    if (i + 1 == valuesStr.length()) //newline
                    {
                        sb.append('\n');
                        valuesStr += csvReader.readLine();
                    } else //stuff following escape backslash
                    {
                        i++;
                        sb.append(valuesStr.charAt(i));
                    }
                } else if (cur == ',') //structural comma
                {
                    tokensList.add(sb.toString());
                    sb = new StringBuilder();
                } else sb.append(cur); //ordinary char
            }
            tokensList.add(sb.toString());

            String[] tokens = (String[]) tokensList.toArray(new String[tokensList.size()]);

            Cell newCell = variable.createCell();
            // Set the onset and offset from tokens in the line.
            newCell.setOnset(tokens[DATA_ONSET]);
            newCell.setOffset(tokens[DATA_OFFSET]);

            // Strip the first and last chars - presumably parens
            tokens[DATA_INDEX] = tokens[DATA_INDEX].substring(1, tokens[DATA_INDEX].length());
            int end = tokens.length - 1;
            tokens[end] = tokens[end].substring(0, tokens[end].length() - 1);

            parseFormalArgs(tokens, DATA_INDEX, variable.getRootNode(), (MatrixCellValue) newCell.getCellValue());
            // Get the next line in the file for reading.
            line = csvReader.readLine();
        }

        return line;
    }

    /**
     * Does the string represent a cell?
     *
     * @param string The string.
     *
     * @return True if it is a cell; otherwise false.
     */
    private boolean isCell(String string) {
        return Character.isDigit(string.charAt(0))
                && Character.isDigit(string.charAt(1))
                && string.charAt(2) == ':';
    }

    /**
     * Method to invoke when we encounter a block of text in the CSV file that is the contents of a variable
     *
     * @param csvReader The csvReader we are currently parsing
     * @param variable The variable that we will be adding cells too
     * @param populateEntry Populates entries to use when converting the contents of the cell into a data value that can
     *                      be inserted into the spread sheet
     * @return The next line in the file that is not part of the block of text in the CSV file
     * @throws IOException Exception if unable to read the file correctly
     */
    private String parseEntries(final BufferedReader csvReader, final Variable variable,
                                final PopulateEntry populateEntry) throws IOException {

        // Keep parsing lines and putting them in the newly formed nominal variable until we get to a line indicating
        // the end of file or a new variable section
        String line = csvReader.readLine();

        boolean hasParseError = false;
        int nError = 0;

        while ((line != null) && Character.isDigit(line.charAt(0))) {

            // Remove backslashes if there are more than would be used for newline escapes
            if (line.contains("\\")) {
                if (line.endsWith("\\") || line.endsWith("\\\\")) {
                    line = line.replace("\\", "") + "\\";
                } else {
                    line = line.replace("\\", "");
                }
            }

            try {
                // Split the line into tokens using a comma delimiter
                String[] tokens = line.split(",");

                // BugzID: 1075 - If the line ends with an escaped new line - add
                // the next line to the current text field.
                while ((line != null) && line.endsWith("\\") && !line.endsWith("\\\\")) {
                    line = csvReader.readLine();
                    String content = tokens[tokens.length - 1];
                    content = content.substring(0, content.length() - 1);
                    tokens[tokens.length - 1] = content + '\n' + line;
                }

                Cell newCell = variable.createCell();

                // Set the onset and offset from tokens in the line
                newCell.setOnset(tokens[DATA_ONSET]);
                newCell.setOffset(tokens[DATA_OFFSET]);
                populateEntry.populate(tokens, newCell.getCellValue());

                // Get the next line in the file for reading
                line = csvReader.readLine();

                // Test to see if the new line is an error line
                if ((line != null) && !Character.isDigit(line.charAt(0))) {
                    if (testForCorruptLine(line)) {
                        hasParseError = true;
                        nError += 1;
                        line = fixCorruptLine(line);
                        logger.error("Error in line " + line);
                    }
                }
            } catch (Exception e) {
                // TODO: Add in fix here for matrix cells that are corrupted in the data values
                hasParseError = true;
                nError += 1;
                logger.error("Error in line: " + line + ". Error: ", e);
            }
        }

        if (hasParseError) {
            JOptionPane.showMessageDialog(null,
                    "Error reading file. " + String.valueOf(nError) + " cells could not be read.\n" +
                            "Recovered files have time 99:00:00:000.\n" +
                            "Please send this file to Datavyu Support for further analysis!",
                    "Error reading file: Corrupted cells",
                    JOptionPane.ERROR_MESSAGE);
        }

        return line;
    }

    private boolean testForCorruptLine(String line) {
        return line.split("\\(").length != 2;
    }

    private String fixCorruptLine(String line) {
        return "99:00:00:000,99:00:00:000," + line;
    }

    /**
     * Method to build a formal argument.
     *
     * @param content The string holding the formal argument content to be parsed.
     * @return The formal argument.
     */
    private Argument parseFormalArgument(final String content) {
        Argument argument;
        String[] formalArgument = content.split("\\|");
        formalArgument[0] = this.stripEscapeCharacters(formalArgument[0]);

        // Add text formal argument.
        if (formalArgument[1].equalsIgnoreCase("quote_string")) {
            argument = null;

        } else if (formalArgument[1].equalsIgnoreCase("integer")) {
            // Add integer formal argument.
            argument = new Argument(formalArgument[0], Argument.Type.NOMINAL);

        } else if (formalArgument[1].equalsIgnoreCase("float")) {
            // Add float formal argument.
            argument = new Argument(formalArgument[0], Argument.Type.NOMINAL);

        } else {
            // Add nominal formal argument.
            argument = new Argument(formalArgument[0], Argument.Type.NOMINAL);
        }

        return argument;
    }

    /**
     * Method to invoke when we encounter a block of text that is a variable.
     *
     * @param csvFile The CSV file we are currently reading.
     * @param line    The line of the CSV file we are currently reading.
     * @param db      The data store we are populating with data from the CSV file.
     * @return The next String that is not part of the currently variable that
     * we are parsing.
     * @throws IOException          When we are unable to read from the csvFile.
     * @throws UserWarningException When we are unable to create a new variable.
     */
    private String parseVariable(final BufferedReader csvFile,
                                 final String line,
                                 final DataStore db)
            throws IOException, UserWarningException {
        return parseVariable(csvFile, line, db, "#2");
    }

    /**
     * Method to invoke when we encounter a block of text that is a variable.
     *
     * @param csvFile The CSV file we are currently reading
     * @param line The line of the CSV file we are currently reading
     * @param dataStore The data store we are populating with data from the CSV file
     * @return The next String that is not part of the currently variable that we are parsing
     * @throws IOException When we are unable to read from the csvFile
     * @throws UserWarningException When we are unable to create variables
     */
    private String parseVariable(final BufferedReader csvFile, final String line, final DataStore dataStore,
                                 final String version)
            throws IOException, UserWarningException {
        // Determine the variable name and type.
        String[] tokens = line.split("\\(");
        String varName = this.stripEscapeCharacters(tokens[0].trim());
        String varType;
        boolean varVisible = true;

        logger.info("Parsing variable from line: " + line);
        logger.info("Found " + tokens.length + " tokens");
        if (version.equals("#4")) {
            String[] varArgs = tokens[1].split(",");
            varType = varArgs[0];
            varVisible = Boolean.parseBoolean(varArgs[1]);
            //varComment = varArgs[2].substring(0, varArgs[2].indexOf(")"));
        } else if (version.equals("#3")) {
            varType = tokens[1].substring(0, tokens[1].indexOf(","));
            varVisible = Boolean.parseBoolean(tokens[1].substring(
                    tokens[1].indexOf(",") + 1, tokens[1].indexOf(")")));
        } else {
            varType = tokens[1].substring(0, tokens[1].indexOf(")"));
        }

        // BugzID:1703 - Ignore old macshapa query variables, we don't have a
        // reliable mechanisim for loading their predicates. Given problems
        // between the untyped nature of macshapa and the typed nature of
        // Datavyu.
        if (varName.equals("###QueryVar###")) {
            String lineEater = csvFile.readLine();

            while ((lineEater != null)
                    && Character.isDigit(lineEater.charAt(0))) {
                lineEater = csvFile.readLine();
            }

            return lineEater;
        }

        // Create variable to put cells within.
        Argument.Type variableType = getVarType(varType);
        Variable newVar = dataStore.createVariable(varName, variableType, true);
        
        newVar.setHidden(!varVisible);

        newVar.setOrderIndex(numVariablesParsed);
        numVariablesParsed++;
        // Read text variable.
        if (variableType == Argument.Type.TEXT) {
            return parseEntries(csvFile, newVar, new PopulateText());

        } else if (variableType == Argument.Type.NOMINAL) {
            // Read nominal variable.
            return parseEntries(csvFile, newVar, new PopulateNominal());

        } else if (variableType == Argument.Type.MATRIX) {
            if(tokens.length > 1) {
                // Read matrix variable - Build vocab for matrix.
                String[] vocabString = tokens[1].split("(?<!\\\\)-");

                // Get the vocab element for the matrix and clean it up to be
                // populated with arguments from the CSV file.
                Argument newArg = newVar.getRootNode();
                newArg.clearChildArguments();

                if (vocabString.length > 1) {
                    // For each of the formal arguments in the file - parse it and
                    // create a formal argument in the matrix vocab element.
                    for (String arg : vocabString[1].split(",")) {
                        newArg.childArguments.add(parseFormalArgument(arg));
                    }
                } else {
                    logger.error("Can not parse codes from: " + tokens[1]);
                }
                newVar.setRootNode(newArg);

                return parseMatrixVariable(csvFile, newVar, newArg);
            }
        }
        throw new IllegalStateException("Unknown variable type.");
    }

    /**
     * @param type The string containing the variable type.
     * @return The type of the variable.
     */
    private Argument.Type getVarType(final String type) {

        if (type.equalsIgnoreCase("text")) {
            return Argument.Type.TEXT;

        } else if (type.equalsIgnoreCase("nominal")) {
            return Argument.Type.NOMINAL;

        } else if (type.equalsIgnoreCase("predicate")) {
            // TODO - support predicate types.
            return null;

        } else if (type.equalsIgnoreCase("matrix")) {
            return Argument.Type.MATRIX;

        } else if (type.equalsIgnoreCase("integer")) {
            // TODO - support integer types.
            return null;

        } else if (type.equalsIgnoreCase("float")) {
            // TODO - support float types.
            return null;
        }

        // Error - Unknown type.
        return null;
    }

    /**
     * Populate entries in spreadsheet cells.
     */
    private abstract class PopulateEntry {

        /**
         * Populates a DataValue from the supplied array of tokens.
         *
         * @param tokens    The tokens to use when building a DataValue.
         * @param destCellValue That this populator is filling with content.
         */
        abstract void populate(final String[] tokens, final CellValue destCellValue);
    }

    /**
     * PopulateEntry for creating nominal data values.
     */
    private class PopulateNominal extends PopulateEntry {

        /**
         * Populates a DataValue from the supplied array of tokens.
         *
         * @param tokens The tokens to use when building a DataValue.
         * @param destCellValue That this populator is filling with content.
         */
        @Override
        void populate(final String[] tokens, final CellValue destCellValue) {
            // BugzID:722 - Only populate the value if we have one from the file
            if (tokens.length > DATA_INDEX) {
                destCellValue.set(stripEscapeCharacters(tokens[DATA_INDEX]));
            }
        }
    }

    /**
     * PopulateEntry for creating text data values.
     */
    private class PopulateText extends PopulateEntry {

        /**
         * Populates a DataValue from the supplied array of tokens.
         *
         * @param tokens    The tokens to use when building a DataValue.
         * @param destCellValue That this populator is filling with content.
         */
        @Override
        void populate(final String[] tokens, final CellValue destCellValue) {
            // BugzID:722 - Only populate the value if we have one from the file
            if (tokens.length > DATA_INDEX) {
                String text = "";

                for (int i = DATA_INDEX; i < tokens.length; i++) {
                    text = text.concat(tokens[i]);

                    if (i < (tokens.length - 1)) {
                        text = text.concat(",");
                    }
                }

                destCellValue.set(stripEscapeCharacters(text));
            }
        }
    }
    
    private class NameWarning implements Runnable {
        private String names;
        
        public NameWarning(String names)
        {
            this.names = names;
        }
        
        public void run() {
            ConfigurationProperties config = ConfigurationProperties.getInstance();
            if (config.getDoWarnOnIllegalColumnNames()) {
                String message = "The following: \n" + names + " is/are no longer a valid column name(s)." +
                        "\nColumn names should begin with letter. Underscores are the only permitted special characters." +
                        "\nIt is recommended that you manually rename this column immediately or use the nifty script in favorites." +
                        "\nContinue showing this warning in the future?";
                if (JOptionPane.showConfirmDialog(null, message,"Warning!",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION){
                    config.setDoWarnOnIllegalColumnNames(false);
                }
            }
        }
    }
}
