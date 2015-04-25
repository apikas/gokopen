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

@RequestMapping(value = "/admin")
@Controller
public class ImportPatrolController {
    @Autowired
    private TrackService trackService;
    @Autowired
    private PatrolService patrolService;
    private ImportPatrol patrolImporter = new ImportPatrol();

    @Component
    public static class Uploader {
        MultipartFile csvFile;
        List<String> fieldDescriptions;

        public MultipartFile getCsvFile() {
            return csvFile;
        }

        public void setCsvFile(MultipartFile file) {
            this.csvFile = file;
        }

        public List<String> getFieldDescriptions() {
            return fieldDescriptions;
        }

        public void setFieldDescriptions(List<String> fieldDescriptions) {
            this.fieldDescriptions = fieldDescriptions;
        }
    }

    @ModelAttribute("tracks")
    public List<Track> populateTracks() {
        return trackService.getAllTracks();
    }

    @RequestMapping(value = "/importpatrol", method = RequestMethod.GET)
    public ModelAndView importPatrol(HttpServletRequest request) {
        //        Config config = configService.getCurrentConfig();
        //         ModelMap map = new ModelMap();
        //         map.put("import", config);
        Uploader uploader = new Uploader();
        uploader.setFieldDescriptions(patrolImporter.getFieldDescriptions());
        return new ModelAndView("importpatrol", "importpatrolmodel", uploader);
    }

    public static class SavePatrols {
        private String errormsg = null;
        private List<String> info = new LinkedList<>();
        private List<ImportPatrol.PatrolImplImport> patrols = new ArrayList<>();

        public String getErrormsg() {
            return errormsg;
        }

        public void setErrormsg(String errormsg) {
            this.errormsg = errormsg;
        }

        public List<String> getInfo() {
            return info;
        }

        public void setInfo(List<String> info) {
            this.info = info;
        }

        public List<ImportPatrol.PatrolImplImport> getPatrols() {
            return patrols;
        }

        public void setPatrols(List<ImportPatrol.PatrolImplImport> patrols) {
            this.patrols = patrols;
        }
    }

    @RequestMapping(value = "/importpatrol", method = RequestMethod.POST)
    public ModelAndView postImportPatrol(Uploader uploader, BindingResult errors, HttpServletRequest request,
        HttpServletResponse response) {
        SavePatrols savePatrols = new SavePatrols();
        if (uploader.csvFile == null || uploader.csvFile.isEmpty()) {
            savePatrols.setErrormsg("Filen är tom, uppladdningen misslyckades!");
        }
        String name = uploader.csvFile.getOriginalFilename();
        System.out.println(name);
        System.out.println(uploader.csvFile.getSize());
        System.out.println("Upload");

        List<PatrolImpl> oldPatrols = patrolService.getAllPatrols();
        List<Track> tracks = populateTracks();
        System.out.println(tracks);
        List<String> messages = new LinkedList<String>();
        try {
            List<ImportPatrol.PatrolImplImport> patrolList =
                patrolImporter.importCSVstream(uploader.csvFile.getInputStream(), messages);
            System.out.println(messages);
            savePatrols.info = messages;
            if (patrolList == null) {
                savePatrols.setErrormsg("Importen misslyckades.");
            } else {
                for (ImportPatrol.PatrolImplImport p : patrolList) {
                    p.checkPatrolImport(tracks, oldPatrols);
                    System.out.printf("Patrull %s: %s %s %s\n", p.getPatrolName(), p.getShouldImport(),
                        p.getTrack() == null ? "?" : p.getTrack().getTrackName(), p.getNote());
                }
                savePatrols.setPatrols(patrolList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            savePatrols.setErrormsg("Importen misslyckades.");
        }

        return new ModelAndView("importpatrolsave", "savepatrolmodel", savePatrols);
    }

    @RequestMapping(value = "/importpatrolsave", method = RequestMethod.POST)
    public @ResponseBody String postImportPatrolSave(SavePatrols savepatrols, BindingResult errors,
        HttpServletRequest request, HttpServletResponse response) {
        int saved = 0;
        int replaced = 0;
        int failed = 0;
        List<Track> tracks = populateTracks();
        List<PatrolImpl> oldPatrols = patrolService.getAllPatrols();
        for (ImportPatrol.PatrolImplImport p : savepatrols.getPatrols()) {
            try {
                if (p.getShouldImport()) {
                    System.out.printf("Sparar patrull %s: %s %s %s\n", p.getPatrolName(), p.getShouldImport(),
                        p.getTrack() == null ? "?" : p.getTrack().getTrackName(), p.getNote());
                    p.fixPatrolTrack(tracks); // Find the track from the track name
                    Integer patrolId = p.findOldPatrol(oldPatrols);
                    if (patrolId != null) {
                        replaced++;
                        System.out.printf("Replacing patrol id %d\n", patrolId);
                        patrolService.deletePatrolById(patrolId);
                    }
                    patrolService.savePatrol(p.clonePatrol());
                    saved++;
                }
            } catch (Exception e) {
                failed++;
                e.printStackTrace();
            }
        }
        return String.format("Sparade %d patruller (varav %d ersattes), misslycades med %d.", saved, replaced, failed);
    }
}
