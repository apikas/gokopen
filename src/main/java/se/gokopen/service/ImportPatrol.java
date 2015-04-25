package se.gokopen.service;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;

import se.gokopen.model.PatrolImpl;
import se.gokopen.model.Track;

public class ImportPatrol extends ImportCSV<ImportPatrol.PatrolImplImport> {
    //    public static List<PatrolImpl> parseFile() {       
    //    }

    public static class PatrolImplImport extends PatrolImpl {
        String action;
        String importComment;
        Boolean shouldImport;
        String trackName;
        String contactMobile;
        String contactEmail;
        Integer memberCount;

        public void checkPatrolImport(final List<Track> tracks, final List<PatrolImpl> oldPatrols) {
            setShouldImport(true);
            StringBuffer wholeNote = new StringBuffer();
            StringBuffer importComment = new StringBuffer();

            // Match track name
            fixPatrolTrack(tracks);
            if (getTrack() == null) {
                setShouldImport(false);
                importComment.append("Okänd tävlingsklass '" + (trackName == null ? "" : trackName) + "'!\n");
            }

            // Compare old patrols
            Integer patrolId = findOldPatrol(oldPatrols);
            if (patrolId != null) {
                setShouldImport(false);
                importComment.append("Patrullen finns redan.\n");
            }

            // Extra fields
            if (getMembers() == null && getMemberCount() != null) setMembers(getMemberCount().toString());
            if (getNote() != null) wholeNote.append(getNote());
            if (getContactMobile() != null) wholeNote.append("\nKontakt mobil: " + getContactMobile());
            if (getContactEmail() != null) wholeNote.append("\nKontakt email: " + getContactEmail());
            setNote(wholeNote.toString());
            setImportComment(importComment.toString());
        }

        public void fixPatrolTrack(final List<Track> tracks) {
            if (trackName == null) {
                if (tracks.size() == 1) setTrack(tracks.get(0));
                return;
            }
            // Match track name
            for (Track track : tracks) {
                if (track.getTrackName().compareToIgnoreCase(trackName) == 0) {
                    setTrack(track);
                    break;
                }
            }
        }

        public Integer findOldPatrol(final List<PatrolImpl> oldPatrols) {
            for (PatrolImpl p : oldPatrols) {
                if (p.getPatrolName().compareToIgnoreCase(getPatrolName()) == 0
                    && p.getTroop().compareToIgnoreCase(getTroop()) == 0) { return p.getPatrolId(); }
            }
            return null;
        }

        public PatrolImpl clonePatrol() throws ReflectiveOperationException {
            PatrolImpl p = new PatrolImpl();
            try {
                BeanUtils.copyProperties(p, this);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw (e);
            }
            return p;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getImportComment() {
            return importComment;
        }

        public void setImportComment(String importComment) {
            this.importComment = importComment;
        }

        public Boolean getShouldImport() {
            return shouldImport;
        }

        public void setShouldImport(Boolean shouldImport) {
            this.shouldImport = shouldImport;
        }

        public String getContactMobile() {
            return contactMobile;
        }

        public void setContactMobile(String contactMobile) {
            this.contactMobile = contactMobile;
        }

        public String getContactEmail() {
            return contactEmail;
        }

        public void setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
        }

        public String getTrackName() {
            return trackName;
        }

        public void setTrackName(String trackName) {
            this.trackName = trackName;
        }

        public Integer getMemberCount() {
            return memberCount;
        }

        public void setMemberCount(Integer memberCount) {
            this.memberCount = memberCount;
        }
    }

    private static BeanPattern[] allBeanPatterns =
        { new BeanPattern("troop", true, "Scoutkårens namn", new String[] { "kår$" }),
            new BeanPattern("patrolName", true, "Scoutpatrullens namn", new String[] { "^patrull(namn)?$" }),
            new BeanPattern("trackName", false, "Deltävling eller tävlingsklass", new String[] { "tävling", "klass$" }),
            new BeanPattern("members", false, "Namnen på patrullens medlemmar", new String[] { "^medlemmar" }),
            new BeanPattern("memberCount", false, "Antalet medlemmar i patrullen", new String[] { "^antal" }),
            new BeanPattern("leaderContact", false, "Kontaktperson vid anmälan", new String[] { "kontaktperson", "ledare" }),
            new BeanPattern("contactMobile", false, "Kontaktpersonens mobiltelefonnummer", new String[] { "mobil.*n(umme)?r", "tel.*n(umme)?r" }),
            new BeanPattern("contactEmail", false, "Kontaktpersonens e-postsdress", new String[] { "e-?post", "e-?mail" }),
            new BeanPattern("action", false, "Åtgärd (ny/ändra/borttagning e.dyl.)", new String[] { "åtgärd" }),
            new BeanPattern("note", false, "Kommentar vid anmälan", new String[] { "kommentar" }), };

    @Override
    BeanPattern[] getAllBeanPatterns() {
        return allBeanPatterns;
    }

    @Override
    Class<PatrolImplImport> getBeanClass() {
        return PatrolImplImport.class;
    }
}
