package se.gokopen.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.Trim;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.dozer.CsvDozerBeanReader;
import org.supercsv.io.dozer.ICsvDozerBeanReader;
import org.supercsv.prefs.CsvPreference;

public abstract class ImportCSV<T> {

    CellProcessor[] getProcessors(final String[] header) {
        return getProcessors(header, Collections.<String> emptyList());
    }

    CellProcessor[] getProcessors(final String[] header, final List<String> required) {
        final CellProcessor[] processors = new CellProcessor[header.length];
        for (int i = 0; i < processors.length; i++) {
            if (header[i] == null) {
                processors[i] = null;
            } else if (required.contains(header[i])) {
                processors[i] = new NotNull(new Trim());
            } else {
                processors[i] = new Optional(new Trim());
            }
        }
        return processors;
    }

    /**
     * Get an array of patterns for each bean name
     * 
     * @return
     */
    abstract BeanPattern[] getAllBeanPatterns();

    List<String> getRequiredFields() {
        List<String> required = new LinkedList<>();
        for (BeanPattern beanPattern : getAllBeanPatterns()) {
            if (beanPattern.getRequired()) required.add(beanPattern.getBeanName());
        }
        return required;
    }
    
    abstract Class<T> getBeanClass();

    /**
     * Get the bean names for each header position by comparing the fileHeader elements with name patterns
     * 
     * @param fileHeader The first row of a CSV file
     * @param allBeanPatterns Array of pattern arrays, where the first element is not an array but a bean name
     * @param messages List that we append messages to when analyzing the fileHeader
     * @return Array of bean names for each column position or null if column is ignored (no match)
     */
    String[] getHeaders(final String[] fileHeader, List<String> messages) {
        final BeanPattern[] allBeanPatterns = getAllBeanPatterns();
        List<String> ignored = new LinkedList<String>();
        String[] headers = new String[fileHeader.length];
        for (int colIndex = 0; colIndex < fileHeader.length; colIndex++) {
            String hName = fileHeader[colIndex];
            // Find a matching pattern
            String beanName = null;
            List<String> matchedPatterns = new LinkedList<String>();
            for (BeanPattern beanPatterns : allBeanPatterns) {
                String match = beanPatterns.match(hName);
                if (match != null) {
                    if (beanName == null) beanName = beanPatterns.getBeanName();
                    matchedPatterns.add(match);
                    break;
                }
            }
            headers[colIndex] = beanName;
            if (beanName == null) ignored.add(hName);
            if (matchedPatterns.size() > 1) {
                messages.add("Fler än en kolumn matchar '" + hName + "', använder den första: "
                    + matchedPatterns.toString());
            }
        }
        if (ignored.size() > 0) {
            messages.add("Ignorerar kolumner " + ignored);
        }
        return headers;
    }

    public List<String> getFieldDescriptions() {
        List<String> fieldDescriptions = new LinkedList<>();
        for (BeanPattern beanPattern : getAllBeanPatterns()) {
            fieldDescriptions.add(beanPattern.getImportDescription());
        }
        return fieldDescriptions;
    }

    public String detectEncoding(byte[] buf, List<String> messages) {
        org.mozilla.universalchardet.UniversalDetector detector =
            new org.mozilla.universalchardet.UniversalDetector(null);
        detector.handleData(buf, 0, buf.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        if (encoding != null) {
            System.out.println("Detected encoding = " + encoding);
            messages.add(String.format("Använder teckenkodning '%s' vid import.", encoding));
        } else if (buf.length / (countMacRoman(buf) + 1) < 100) {
            encoding = "MacRoman"; // "ASCII";
        } else {
            encoding = "ASCII";
            System.out.printf("No encoding detected, using default '%s'.", encoding);
            messages.add(String.format("Filen innehåller en okänd teckenkodning, använder '%s' vid import.", encoding));
        }
        detector.reset();
        return encoding;
    }

    private static final byte[] macRomanChars = { (byte) 0x80, (byte) 0x81, (byte) 0x83, (byte) 0x85, (byte) 0x86,
        (byte) 0x8A, (byte) 0x8C, (byte) 0x8E, (byte) 0x9A, (byte) 0x9F };

    private int countMacRoman(byte[] buf) {
        int count = 0;
        for (int i = 0; i < buf.length; i++) {
            if (ArrayUtils.indexOf(macRomanChars, buf[i]) >= 0) count++;
        }
        return count;
    }

    public List<T> importCSVstream(final InputStream stream, final List<String> messages) {
        final Class<T> beanClass = getBeanClass();
        List<T> beanList = null;
        byte[] contents = null;
        try {
            contents = IOUtils.toByteArray(stream);
        } catch (IOException e) {
            e.printStackTrace();
            messages.add("Kunde inte läsa filen.");
            return beanList;
        }
        final String encoding = detectEncoding(contents, messages);

        ICsvDozerBeanReader beanReader = null;
        try {
            CsvPreference csvPref = CsvPreference.STANDARD_PREFERENCE;
            if (ArrayUtils.indexOf(contents, (byte)';') >= 0 && ArrayUtils.indexOf(contents, (byte)';') < ArrayUtils.indexOf(contents, (byte)',')) {
                csvPref = CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE;
            }
            Reader streamReader = new InputStreamReader(new ByteArrayInputStream(contents), encoding);
            beanReader = new CsvDozerBeanReader(streamReader, csvPref );

            final String[] fileHeader = beanReader.getHeader(true); // Expect first row to be a headline
            System.out.println(Arrays.asList(fileHeader));

            final String[] header = getHeaders(fileHeader, messages);
            final CellProcessor[] processors = getProcessors(header, getRequiredFields());
            beanReader.configureBeanMapping(beanClass, header);

            List<T> tmpBeanList = new LinkedList<T>();
            T bean;
            while ((bean = beanReader.read(beanClass, processors)) != null) {
                tmpBeanList.add(bean);
            }
            beanList = tmpBeanList;
        } catch (org.supercsv.exception.SuperCsvException e) {
            messages.add("Felaktigt filformat, förklaring:");
            messages.add(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            messages.add(String.format("Filens teckenkodning '%s' stöds inte.", encoding));
        } catch (IOException e) {
            e.printStackTrace();
            messages.add(String.format("Kunde inte läsa innehållet."));
        } finally {
            if (beanReader != null) try {
                beanReader.close();
            } catch (IOException e) {
            }
        }
        return beanList;
    }

    static final class BeanPattern {
        private final String beanName;
        private final String description;
        private final Boolean required;
        private final String[] headerPatterns;
        private final Pattern[] compiledPatterns;

        public BeanPattern(String beanName, Boolean required, String description, String[] headerPatterns) {
            this.beanName = beanName;
            this.required = required;
            this.description = description;
            this.headerPatterns = headerPatterns;
            this.compiledPatterns = new Pattern[headerPatterns.length];
            for (int i = 0; i < headerPatterns.length; i++) {
                this.compiledPatterns[i] =
                    Pattern.compile(headerPatterns[i], Pattern.UNICODE_CHARACTER_CLASS | Pattern.UNICODE_CASE
                        | Pattern.CASE_INSENSITIVE);
            }
        }

        public String getBeanName() {
            return beanName;
        }

        public Boolean getRequired() {
            return required;
        }

        public String getDescription() {
            return description;
        }

        public String getImportDescription() {
            StringBuffer buf = new StringBuffer();
            buf.append(String.format("%s: ", description));
            Boolean firstElement = true;
            for (String pattern : headerPatterns) {
                if (! firstElement) buf.append(", ");
                buf.append(pattern);
                firstElement = false;
            }
            return buf.toString();
        }

        /**
         * Sees if any of the patterns match a header string
         * 
         * @param header Header string to match
         * @return Pattern string or null if not found
         */
        public String match(String header) {
            for (int i = 0; i < compiledPatterns.length; i++) {
                Pattern p = compiledPatterns[i];
                if (p.matcher(header).find()) return headerPatterns[i];
            }
            return null;
        }
    }
}
