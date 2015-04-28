package se.gokopen.service;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;

import se.gokopen.model.PatrolImpl;
import se.gokopen.model.Track;

/**
 * Import patrols from a CSV file (see ImportCSV)
 *
 * @author Anders Pikas <anders@pikas.se>
 */
public class ImportPatrol extends ImportCSV<ImportPatrol.PatrolImplImport> {
    /**
     * Helper class used when importing patrols (PatrolImpl). Extra properties are defined which are used during the
     * import and then shown during import or translated to properties that are available in PatrolImpl.
     *
     * @author Anders Pikas <anders@pikas.se>
     */
    public static class PatrolImplImport extends PatrolImpl {
        String action; // Descriptive text shown during import e.g. New/Remove/Change
        String importComment; // Hints shown during import
        Boolean shouldImport; // True if the import checkbox should be ticked
        String trackName; // Name of track for the patrol, translated to Track
        String contactMobile; // Append mobile phone number to note property
        String contactEmail; // Append email address to note property
        Integer memberCount; // Member count may be used instead of a list of member names

        /**
         * The fields inherited from PatrolImpl are set based on the extra properties read from CSV.
         * shouldImport is set based on current patrols in the database and tracks.
         * 
         * @param tracks
         * @param oldPatrols
         */
        public void setPatrolFields(final List<Track> tracks, final List<PatrolImpl> oldPatrols) {
            setShouldImport(true);
            final StringBuffer wholeNote = new StringBuffer();
            final StringBuffer importComment = new StringBuffer();

            // Match track name
            fixPatrolTrack(tracks);
            if (getTrack() == null) {
                setShouldImport(false);
                importComment.append("Okänd tävlingsklass '" + (this.trackName == null ? "" : this.trackName) + "'!\n");
            }

            // Compare old patrols
            final Integer patrolId = findOldPatrol(oldPatrols);
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

        /**
         * Set track if it is unambiguous from the trackName
         *
         * @param tracks
         */
        public void fixPatrolTrack(final List<Track> tracks) {
            if (this.trackName == null) {
                if (tracks.size() == 1) setTrack(tracks.get(0));
                return;
            }
            // Match track name
            for (final Track track : tracks) {
                if (track.getTrackName().compareToIgnoreCase(this.trackName) == 0) {
                    setTrack(track);
                    break;
                }
            }
        }

        /**
         * See if there is an old patrol with the same name and troopName.
         * TODO: should be moved to class PatrolServiceImpl
         *
         * @param oldPatrols List of old patrols to compare
         * @return patrolId of old patrol or null if no patrol matches
         */
        public Integer findOldPatrol(final List<PatrolImpl> oldPatrols) {
            for (final PatrolImpl p : oldPatrols) {
                if (p.getPatrolName().compareToIgnoreCase(getPatrolName()) == 0
                    && p.getTroop().compareToIgnoreCase(getTroop()) == 0) { return p.getPatrolId(); }
            }
            return null;
        }

        /**
         * Copy the patrol import object to a PatrolImpl by copying all bean properties that have the same name.
         *
         * @return New PatrolImpl
         * @throws ReflectiveOperationException
         */
        public PatrolImpl clonePatrol() throws ReflectiveOperationException {
            final PatrolImpl p = new PatrolImpl();
            try {
                BeanUtils.copyProperties(p, this);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw (e);
            }
            return p;
        }

        public String getAction() {
            return this.action;
        }

        public void setAction(final String action) {
            this.action = action;
        }

        public String getImportComment() {
            return this.importComment;
        }

        public void setImportComment(final String importComment) {
            this.importComment = importComment;
        }

        public Boolean getShouldImport() {
            return this.shouldImport;
        }

        public void setShouldImport(final Boolean shouldImport) {
            this.shouldImport = shouldImport;
        }

        public String getContactMobile() {
            return this.contactMobile;
        }

        public void setContactMobile(final String contactMobile) {
            this.contactMobile = contactMobile;
        }

        public String getContactEmail() {
            return this.contactEmail;
        }

        public void setContactEmail(final String contactEmail) {
            this.contactEmail = contactEmail;
        }

        public String getTrackName() {
            return this.trackName;
        }

        public void setTrackName(final String trackName) {
            this.trackName = trackName;
        }

        public Integer getMemberCount() {
            return this.memberCount;
        }

        public void setMemberCount(final Integer memberCount) {
            this.memberCount = memberCount;
        }
    }

    // Property description for one property in a patrol import class
    private static BeanPropertyPattern[] allPropertyPatterns = {
        new BeanPropertyPattern("troop", true, "Scoutkårens namn", new String[] { "kår$" }),
        new BeanPropertyPattern("patrolName", true, "Scoutpatrullens namn", new String[] { "^patrull(namn)?$" }),
        new BeanPropertyPattern("trackName", false, "Deltävling eller tävlingsklass", new String[] { "tävling",
            "klass$" }),
        new BeanPropertyPattern("members", false, "Namnen på patrullens medlemmar", new String[] { "^medlemmar" }),
        new BeanPropertyPattern("memberCount", false, "Antalet medlemmar i patrullen", new String[] { "^antal" }),
        new BeanPropertyPattern("leaderContact", false, "Kontaktperson vid anmälan", new String[] { "kontaktperson",
            "ledare" }),
        new BeanPropertyPattern("contactMobile", false, "Kontaktpersonens mobiltelefonnummer", new String[] {
            "mobil.*n(umme)?r", "tel.*n(umme)?r" }),
        new BeanPropertyPattern("contactEmail", false, "Kontaktpersonens e-postsdress", new String[] { "e-?post",
            "e-?mail" }),
        new BeanPropertyPattern("action", false, "Åtgärd (ny/ändra/borttagning e.dyl.)", new String[] { "åtgärd" }),
        new BeanPropertyPattern("note", false, "Kommentar vid anmälan", new String[] { "kommentar" }), };

    @Override
    BeanPropertyPattern[] getAllPropertyPatterns() {
        return allPropertyPatterns;
    }

    @Override
    Class<PatrolImplImport> getBeanClass() {
        return PatrolImplImport.class;
    }
}
