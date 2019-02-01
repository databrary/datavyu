package org.datavyu.controllers;

import org.apache.commons.io.IOUtils;
import org.datavyu.models.db.*;
import org.datavyu.models.project.Project;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;

import static org.testng.Assert.assertTrue;

/**
 * Tests for saving Datavyu project and CSV files.
 */
public class SaveControllerTest {
    // The location of the test files.
    private static final String TEST_FOLDER = System.getProperty("testPath");


    /** BLOCK_SIZE for files */
    private static final int BLOCK_SIZE = 65536;

    /**
     * Checks two files for byte equality
     *
     * @param file1 First file
     * @param file2 Second file
     * @return True if they are equal; otherwise false
     * @throws IOException exception
     */
    private static Boolean areFilesSameByteComp(final File file1, final File file2) throws IOException {

        // Check file sizes first
        if (file1.length() != file2.length()) {
            return false;
        }

        // Compare bytes
        InputStream i1 = new FileInputStream(file1);
        InputStream i2 = new FileInputStream(file2);
        byte[] stream1Block = new byte[BLOCK_SIZE];
        byte[] stream2Block = new byte[BLOCK_SIZE];
        int b1, b2;

        do {
            b1 = i1.read(stream1Block);
            b2 = i2.read(stream2Block);
        } while ((b1 == b2) && (b1 != -1));

        i1.close();
        i2.close();

        // Check if we've reached the end of the file. If we have, they're identical
        return b1 == -1;
    }

    /**
     * Checks if two text files are equal.
     *
     * @param file1 First file
     * @param file2 Second file
     * @return true if equal, else false
     * @throws IOException on file read error
     */
    private static Boolean areFilesSameLineComp(final File file1, final File file2) throws IOException {
        FileReader fr1 = new FileReader(file1);
        FileReader fr2 = new FileReader(file2);

        BufferedReader r1 = new BufferedReader(fr1);
        BufferedReader r2 = new BufferedReader(fr2);

        String line1 = r1.readLine();
        String line2 = r2.readLine();

        if ((line1 != null) && !line1.equals(line2)) {
            return false;
        }

        while ((line1 != null) || (line2 != null)) {

            if ((line1 != null) && !line1.equals(line2)) {
                return false;
            }

            line1 = r1.readLine();
            line2 = r2.readLine();
        }

        IOUtils.closeQuietly(r1);
        IOUtils.closeQuietly(r2);

        IOUtils.closeQuietly(fr1);
        IOUtils.closeQuietly(fr2);

        return true;
    }

    @BeforeClass
    public void spinUp() {
    }

    @AfterClass
    public void spinDown() {
    }

    @Test
    public void testSaveCSV() throws UserWarningException, IOException {
        File outFile = new File("target/test1.csv");
        // Clean up the out file only if it exists.
        if (outFile.exists()) {
            boolean deleted = outFile.delete();
        }
        File demoFile = new File(TEST_FOLDER + "IO/simple1.csv");

        DataStore ds = DataStoreFactory.newDataStore();
        Variable var = ds.createVariable("TestColumn", Argument.Type.TEXT);
        Cell c = var.createCell();
        c.setOnset("00:01:00:000");
        c.setOffset("00:02:00:000");
        c.getCellValue().set("This is a test cell.");

        SaveController savec = new SaveController();
        savec.saveDataStore(outFile, ds);

        assertTrue(areFilesSameLineComp(outFile, demoFile));
    }

    @Test
    public void testLoadOPF() throws UserWarningException, IOException {
        File outFile = new File("target/test2.opf");
        if (outFile.exists()) {
            boolean deleted = outFile.delete();
        }
        File demoFile = new File(TEST_FOLDER + "IO/simple2.opf");

        Project p = new Project();
        p.setProjectName("simple2");
        p.setDatabaseFileName("simple1.csv");
        p.setOriginalProjectDirectory("Z:\\datavyu\\src\\test\\resources\\IO");
        DataStore ds = DataStoreFactory.newDataStore();
        Variable var = ds.createVariable("TestColumn", Argument.Type.TEXT);
        Cell c = var.createCell();
        c.setOnset("00:01:00:000");
        c.setOffset("00:02:00:000");
        c.getCellValue().set("This is a test cell.");

        SaveController savec = new SaveController();
        savec.saveProject(outFile, p, ds);
        assertTrue(areFilesSameByteComp(outFile, demoFile));
    }

    @Test
    public void testLoadOPF2() throws UserWarningException, IOException {
        File outFile = new File("target/test3.opf");
        if (outFile.exists()) {
            boolean deleted = outFile.delete();
        }
        File demoFile = new File(TEST_FOLDER + "IO/simple3.opf");

        Project p = new Project();
        p.setProjectName("simple3");
        p.setOriginalProjectDirectory("Z:\\datavyu\\src\\test\\resources\\IO");

        DataStore ds = DataStoreFactory.newDataStore();
        Variable var = ds.createVariable("testColumn", Argument.Type.TEXT);
        Cell c = var.createCell();
        c.getCellValue().set("cellA");

        var = ds.createVariable("testColumn2", Argument.Type.NOMINAL);
        c = var.createCell();
        c.getCellValue().set("cellB");

        var = ds.createVariable("testColumn3", Argument.Type.MATRIX);
        c = var.createCell();
        c.getCellValue().set("(cellC)");

        var = ds.createVariable("hiddenColumn", Argument.Type.TEXT);
        var.setHidden(true);

        SaveController savec = new SaveController();
        savec.saveProject(outFile, p, ds);
        assertTrue(areFilesSameByteComp(outFile, demoFile));
    }
}
