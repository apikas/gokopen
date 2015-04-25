/**
 * 
 */
package se.gokopen.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import se.gokopen.model.PatrolImpl;
import se.gokopen.model.Track;

/**
 * @author apikas
 */
public class TestImportPatrol {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws UnsupportedEncodingException, FileNotFoundException, Exception {
        File cvsFile = new File(getClass().getResource("/import/example1-macroman-semi.csv").getFile());
        System.out.println(cvsFile);
        List<PatrolImpl> oldPatrols = new LinkedList<>();
        List<Track> tracks = new LinkedList<>();
        int i = 1;
        for (String trackName : new String[] { "Spårare", "Upptäckare" }) {
            Track track = new Track();
            track.setTrackName(trackName);
            track.setTrackId(i++);
            tracks.add(track);
        }
        List<String> messages = new LinkedList<String>();
        // ISO8859_1, MacRoman, UTF8
        List<ImportPatrol.PatrolImplImport> list =
            new ImportPatrol().importCSVstream(new FileInputStream(cvsFile), messages);
        System.out.println(messages);
        for (ImportPatrol.PatrolImplImport p : list) {
            p.checkPatrolImport(tracks, oldPatrols);
            System.out.printf("%s: %s %s\n", p.getPatrolName(), p.getTrack() == null ? "?" : p.getTrack()
                .getTrackName(), p.getNote());
        }
    }

}
