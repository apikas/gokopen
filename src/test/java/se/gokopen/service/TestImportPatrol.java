/**
 *
 */
package se.gokopen.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import se.gokopen.model.PatrolImpl;
import se.gokopen.model.Track;
import se.gokopen.service.ImportPatrol.PatrolImplImport;

public class TestImportPatrol {

    /**
     * Helper class for reading (unmarshaling) patrol lists from file
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "patrols")
    public static class PatrolImplImportList {
        @XmlElement(name = "patrol", type = ImportPatrol.PatrolImplImport.class)
        private List<ImportPatrol.PatrolImplImport> patrols = new LinkedList<ImportPatrol.PatrolImplImport>();

        public PatrolImplImportList() {
        }

        public PatrolImplImportList(final List<ImportPatrol.PatrolImplImport> patrols) {
            this.patrols = patrols;
        }

        public List<ImportPatrol.PatrolImplImport> getPatrols() {
            return this.patrols;
        }

        public void setPatrols(final List<ImportPatrol.PatrolImplImport> patrols) {
            this.patrols = patrols;
        }
    }

    static javax.xml.bind.JAXBContext jaxbCtx = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        jaxbCtx = javax.xml.bind.JAXBContext.newInstance(PatrolImplImportList.class.getPackage().getName());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    PatrolImplImportList example1Patrols = null;
    PatrolImplImportList example2Patrols = null;

    @Before
    public void setUp() throws Exception {
        final javax.xml.bind.Unmarshaller unmarshaller = jaxbCtx.createUnmarshaller();
        this.example1Patrols =
            (PatrolImplImportList) unmarshaller.unmarshal(new File(getClass().getResource("/import/example1.xml")
                .getFile()));
        this.example2Patrols =
            (PatrolImplImportList) unmarshaller.unmarshal(new File(getClass().getResource("/import/example2.xml")
                .getFile()));
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Tests reading a correct CSV file with UTF-8 encoding.
     */
    @Test
    public void testUTF8() throws UnsupportedEncodingException, FileNotFoundException, Exception {
        final File cvsFile = new File(getClass().getResource("/import/example1-utf8.csv").getFile());
        final List<String> messages = new LinkedList<String>();
        importCSV(cvsFile, this.example1Patrols, messages);
        assertThat("Använder teckenkodning 'UTF-8' vid import.", isIn(messages));
    }

    /**
     * Tests reading a correct CSV file with Latin-1 encoding.
     */
    @Test
    public void testLatin1() throws UnsupportedEncodingException, FileNotFoundException, Exception {
        final File cvsFile = new File(getClass().getResource("/import/example1-latin1.csv").getFile());
        final List<String> messages = new LinkedList<String>();
        importCSV(cvsFile, this.example1Patrols, messages);
        assertThat("Använder teckenkodning 'WINDOWS-1252' vid import.", isIn(messages));
    }

    /**
     * Tests reading a correct CSV file with MacRoman encoding, semicolon delimiter.
     */
    @Test
    public void testMacRoman() throws UnsupportedEncodingException, FileNotFoundException, Exception {
        final File cvsFile = new File(getClass().getResource("/import/example1-macroman-semi.csv").getFile());
        final List<String> messages = new LinkedList<String>();
        importCSV(cvsFile, this.example1Patrols, messages);
        assertThat("Använder teckenkodning 'MacRoman' vid import.", isIn(messages));
    }

    /**
     * Tests reading a correct CSV file with UTF-8 encoding, but with import comments.
     */
    @Test
    public void testExample2() throws UnsupportedEncodingException, FileNotFoundException, Exception {
        final File cvsFile = new File(getClass().getResource("/import/example2-utf8.csv").getFile());
        final List<String> messages = new LinkedList<String>();
        importCSV(cvsFile, this.example2Patrols, messages);
    }

    /**
     * Tests reading an incorrect CSV file with UTF-8 encoding - missing field.
     */
    @Test
    public void testExample3() throws UnsupportedEncodingException, FileNotFoundException, Exception {
        final File cvsFile = new File(getClass().getResource("/import/example3-utf8.csv").getFile());
        final List<String> messages = new LinkedList<String>();
        importCSV(cvsFile, null, messages);
        System.out.println(messages);
        assertThat("Felaktigt filformat, förklaring:", isIn(messages));
        assertThat("null value encountered", isIn(messages));
    }

    /**
     * Tests reading an incorrect CSV file with UTF-8 encoding - missing field.
     */
    @Test
    public void testExample4() throws UnsupportedEncodingException, FileNotFoundException, Exception {
        final File cvsFile = new File(getClass().getResource("/import/example4-utf8.csv").getFile());
        final List<String> messages = new LinkedList<String>();
        importCSV(cvsFile, null, messages);
        System.out.println(messages);
        assertThat("Felaktigt filformat, förklaring:", isIn(messages));
        assertThat("null value encountered", isIn(messages));
    }

    /**
     * Tests reading an incorrect CSV file with UTF-8 encoding - missing column.
     */
    @Test
    public void testExample5() throws UnsupportedEncodingException, FileNotFoundException, Exception {
        final File cvsFile = new File(getClass().getResource("/import/example5-utf8.csv").getFile());
        final List<String> messages = new LinkedList<String>();
        importCSV(cvsFile, null, messages);
        System.out.println(messages);
        assertThat("Obligatoriska kolumner saknas:", isIn(messages));
    }

    /**
     * Tests reading an incorrect CSV file with UTF-8 encoding - one row has fewer columns.
     */
    @Test
    public void testExample6() throws UnsupportedEncodingException, FileNotFoundException, Exception {
        final File cvsFile = new File(getClass().getResource("/import/example6-utf8.csv").getFile());
        final List<String> messages = new LinkedList<String>();
        importCSV(cvsFile, null, messages);
        System.out.println(messages);
        assertThat("Felaktigt filformat, förklaring:", isIn(messages));
    }

    private void importCSV(final File cvsFile, final PatrolImplImportList expectedPatrols, final List<String> messages)
        throws FileNotFoundException {
        final PatrolImpl aPatrol = new PatrolImpl();
        aPatrol.setTroop("Svartbäcken");
        aPatrol.setPatrolName("Dubletterna");
        aPatrol.setPatrolId(99);
        final List<PatrolImpl> oldPatrols = new LinkedList<>();
        oldPatrols.add(aPatrol);
        final List<Track> tracks = makeTracks();
        final List<ImportPatrol.PatrolImplImport> list =
            new ImportPatrol().importCSVstream(new FileInputStream(cvsFile), messages);
        if (list == null || expectedPatrols == null) {
            assertEquals(list, expectedPatrols);
            return;
        }
        int i = 0;
        for (final ImportPatrol.PatrolImplImport p : list) {
            p.setPatrolFields(tracks, oldPatrols);
            final PatrolImplImport expectedPatrol = expectedPatrols.getPatrols().get(i);
            assertEquals("Patrol name", expectedPatrol.getPatrolName(), p.getPatrolName());
            assertEquals("Troop", expectedPatrol.getTroop(), p.getTroop());
            if (expectedPatrol.getTrack() != null)
                assertEquals("Track", expectedPatrol.getTrack().getTrackName(), p.getTrack().getTrackName());
            assertEquals("Note", expectedPatrol.getNote(), p.getNote());
            assertEquals("LeaderContact", expectedPatrol.getLeaderContact(), p.getLeaderContact());
            assertEquals("Action", expectedPatrol.getAction(), p.getAction());
            assertEquals("ImportComment", expectedPatrol.getImportComment(), p.getImportComment());
            assertEquals("ShouldImport", expectedPatrol.getShouldImport(), p.getShouldImport());
            assertEquals("TrackName", expectedPatrol.getTrackName(), p.getTrackName());
            assertEquals("ContactMobile", expectedPatrol.getContactMobile(), p.getContactMobile());
            assertEquals("ContactEmail", expectedPatrol.getContactEmail(), p.getContactEmail());
            assertEquals("MemberCount", expectedPatrol.getMemberCount(), p.getMemberCount());
            i++;
        }
    }

    private static List<Track> makeTracks() {
        final List<Track> tracks = new LinkedList<>();
        int i = 1;
        for (final String trackName : new String[] { "Spårare", "Upptäckare" }) {
            final Track track = new Track();
            track.setTrackName(trackName);
            track.setTrackId(i++);
            tracks.add(track);
        }
        return tracks;
    }

    public static void storeExample(final String path, final List<PatrolImplImport> patrols) throws JAXBException,
    FileNotFoundException {
        final javax.xml.bind.Marshaller marshaller = jaxbCtx.createMarshaller();
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(new PatrolImplImportList(patrols), new FileOutputStream(path));
    }

    public static void main(final String[] args) throws Exception {
        setUpBeforeClass();
        final Class<? extends PatrolImplImportList> aClass = new PatrolImplImportList().getClass();
        final String[] exampleFiles = new String[] { "example1", "example2" };
        final PatrolImpl aPatrol = new PatrolImpl();
        aPatrol.setTroop("Svartbäcken");
        aPatrol.setPatrolName("Dubletterna");
        aPatrol.setPatrolId(99);
        final List<PatrolImpl> oldPatrols = new LinkedList<>();
        oldPatrols.add(aPatrol);
        System.out.println("After an implementation change that cause the tests to fail, ");
        System.out
            .println("review differences with files stored in src/test/resources/import and replace the files if they are correct.");
        System.out.println("Storing example XML files in " + System.getProperty("user.dir"));
        for (final String exName : exampleFiles) {
            System.out.println(exName);
            final File cvsFile = new File(aClass.getResource("/import/" + exName + "-utf8.csv").getFile());
            final List<String> messages = new LinkedList<String>();
            final List<ImportPatrol.PatrolImplImport> list =
                new ImportPatrol().importCSVstream(new FileInputStream(cvsFile), messages);
            for (final ImportPatrol.PatrolImplImport p : list) {
                p.setPatrolFields(makeTracks(), oldPatrols);
            }
            storeExample(exName + ".xml", list);
        }
    }
}
