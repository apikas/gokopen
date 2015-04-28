package se.gokopen.controller;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import se.gokopen.model.PatrolImpl;
import se.gokopen.model.Track;
import se.gokopen.service.ImportPatrol;
import se.gokopen.service.PatrolService;
import se.gokopen.service.TrackService;

/**
 * Read patrols from an uploaded CSV file and offer to create them in the database.
 * Header name matching against a fixed set of patterns is used, which is not the most convenient solution for a user,
 * because the user may need to edit the column headers before import (which can be extra tricky if the CSV file uses a
 * character encoding which is not the default used in the editor).
 * An enhanced solution would be to present a list of property descriptions and a list of column header names from the
 * first row in the CSV file, then let the user answer what property belongs in what column.
 *
 * @author Anders Pikas <anders@pikas.se>
 */
@RequestMapping(value = "/admin")
@Controller
public class ImportPatrolController {
    @Autowired
    private TrackService trackService;
    @Autowired
    private PatrolService patrolService;
    private final ImportPatrol patrolImporter = new ImportPatrol();

    /**
     * Component used to transfer field descriptions to a JSP page and uploaded file from a JSP page.
     *
     * @author Anders Pikas <anders@pikas.se>
     */
    @Component
    public static class Uploader {
        MultipartFile csvFile;
        List<String> propertyDescriptions;

        public MultipartFile getCsvFile() {
            return this.csvFile;
        }

        public void setCsvFile(final MultipartFile file) {
            this.csvFile = file;
        }

        public List<String> getPropertyDescriptions() {
            return this.propertyDescriptions;
        }

        public void setPropertyDescriptions(final List<String> propertyDescriptions) {
            this.propertyDescriptions = propertyDescriptions;
        }
    }

    @ModelAttribute("tracks")
    public List<Track> populateTracks() {
        return this.trackService.getAllTracks();
    }

    /**
     * Show property descriptions and let the user upload a CSV file containing patrols.
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/importpatrol", method = RequestMethod.GET)
    public ModelAndView importPatrol(final HttpServletRequest request) {
        final Uploader uploader = new Uploader();
        uploader.setPropertyDescriptions(this.patrolImporter.getPropertyDescriptions());
        return new ModelAndView("importpatrol", "importpatrolmodel", uploader);
    }

    /**
     * Component used to transfer import information about the parsed patrols to a JSP page and what patrols are
     * selected from a JSP page.
     *
     * @author Anders Pikas <anders@pikas.se>
     */
    public static class SavePatrols {
        private String errormsg = null;
        private List<String> info = new LinkedList<>();
        private List<ImportPatrol.PatrolImplImport> patrols = new ArrayList<>();

        public String getErrormsg() {
            return this.errormsg;
        }

        public void setErrormsg(final String errormsg) {
            this.errormsg = errormsg;
        }

        public List<String> getInfo() {
            return this.info;
        }

        public void setInfo(final List<String> info) {
            this.info = info;
        }

        public List<ImportPatrol.PatrolImplImport> getPatrols() {
            return this.patrols;
        }

        public void setPatrols(final List<ImportPatrol.PatrolImplImport> patrols) {
            this.patrols = patrols;
        }
    }

    /**
     * Given an uploaded file, parse the patrols and create a list of patrols that are available for import.
     *
     * @param uploader
     * @param errors
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importpatrol", method = RequestMethod.POST)
    public ModelAndView postImportPatrol(final Uploader uploader, final BindingResult errors,
        final HttpServletRequest request, final HttpServletResponse response) {
        final SavePatrols savePatrols = new SavePatrols();
        if (uploader.csvFile == null || uploader.csvFile.isEmpty()) {
            savePatrols.setErrormsg("Filen är tom, uppladdningen misslyckades!");
        }
        final String name = uploader.csvFile.getOriginalFilename();
        System.out.println(name);
        System.out.println(uploader.csvFile.getSize());
        System.out.println("Upload");

        final List<PatrolImpl> oldPatrols = this.patrolService.getAllPatrols();
        final List<Track> tracks = populateTracks();
        System.out.println(tracks);
        final List<String> messages = new LinkedList<String>();
        try {
            final List<ImportPatrol.PatrolImplImport> patrolList =
                this.patrolImporter.importCSVstream(uploader.csvFile.getInputStream(), messages);
            System.out.println(messages);
            savePatrols.info = messages;
            if (patrolList == null) {
                savePatrols.setErrormsg("Importen misslyckades.");
            } else {
                for (final ImportPatrol.PatrolImplImport p : patrolList) {
                    p.setPatrolFields(tracks, oldPatrols);
                }
                savePatrols.setPatrols(patrolList);
            }
        } catch (final Exception e) {
            e.printStackTrace();
            savePatrols.setErrormsg("Importen misslyckades.");
        }

        return new ModelAndView("importpatrolsave", "savepatrolmodel", savePatrols);
    }

    /**
     * Given a list of selected patrols, create or replace patrols in the database.
     * TODO: create a page showing the results, including a back link.
     *
     * @param savepatrols
     * @param errors
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/importpatrolsave", method = RequestMethod.POST)
    public @ResponseBody String postImportPatrolSave(final SavePatrols savepatrols, final BindingResult errors,
        final HttpServletRequest request, final HttpServletResponse response) {
        int saved = 0;
        int replaced = 0;
        int failed = 0;
        final List<Track> tracks = populateTracks();
        final List<PatrolImpl> oldPatrols = this.patrolService.getAllPatrols();
        for (final ImportPatrol.PatrolImplImport p : savepatrols.getPatrols()) {
            try {
                if (p.getShouldImport()) {
                    System.out.printf("Sparar patrull %s: %s %s %s\n", p.getPatrolName(), p.getShouldImport(),
                        p.getTrack() == null ? "?" : p.getTrack().getTrackName(), p.getNote());
                    p.fixPatrolTrack(tracks); // Find the track from the track name
                    final Integer patrolId = p.findOldPatrol(oldPatrols);
                    if (patrolId != null) {
                        replaced++;
                        System.out.printf("Replacing patrol id %d\n", patrolId);
                        this.patrolService.deletePatrolById(patrolId);
                    }
                    this.patrolService.savePatrol(p.clonePatrol());
                    saved++;
                }
            } catch (final Exception e) {
                failed++;
                e.printStackTrace();
            }
        }
        return String.format("Sparade %d patruller (varav %d ersattes), misslycades med %d.", saved, replaced, failed);
    }
}
