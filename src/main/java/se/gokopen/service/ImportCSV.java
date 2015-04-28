package se.gokopen.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
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

/**
 * Import comma separated files, CSV, according to the definition in
 * http://super-csv.github.io/super-csv/csv_specification.html.
 * The public method importCSVstream is called to convert an InputStream to a list of imported objects.
 * The file encoding is detected, and the use of commas or semicolon as delimiter is guessed based on the first
 * occurrence.
 * Subclasses override at least getBeanClass getAllBeanPatterns to define the class of imported objects and the columns
 * that correspond to bean properties.
 *
 * @author Anders Pikas <anders@pikas.se>
 * @param <T> Class of imported objects (same as getBeanClass returns) which follow the JavaBean conventions
 */
public abstract class ImportCSV<T> {

    /**
     * Create an array of import processors (that handle data type conversions) for an array of header names.
     *
     * @param header Header names
     * @param required Names of required headers
     * @return Import processors for all header names
     */
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
     * Get an array of bean property definitions for properties that are recognized for import of class T.
     *
     * @return
     */
    abstract BeanPropertyPattern[] getAllPropertyPatterns();

    /**
     * Get the names of properties that are required to be non-empty in all import rows.
     *
     * @return
     */
    List<String> getRequiredProperties() {
        final List<String> required = new LinkedList<>();
        for (final BeanPropertyPattern beanPattern : getAllPropertyPatterns()) {
            if (beanPattern.getRequired()) required.add(beanPattern.getPropertyName());
        }
        return required;
    }

    /**
     * Get the class object for class T.
     *
     * @return a Class object for T
     */
    abstract Class<T> getBeanClass();

    /**
     * Get the bean property names for all header column positions by comparing the fileHeader elements with name
     * patterns.
     *
     * @param fileHeader The first row of a CSV file
     * @param messages List that we append messages to when analyzing the fileHeader
     * @return Array of bean property names (or a null element if column is ignored) for all column positions or null if
     *         required column headers are missing
     */
    String[] getHeaderPropertyNames(final String[] fileHeader, final List<String> messages) {
        final BeanPropertyPattern[] allBeanPatterns = getAllPropertyPatterns();
        final List<String> ignored = new LinkedList<String>();
        final String[] headers = new String[fileHeader.length];
        for (int colIndex = 0; colIndex < fileHeader.length; colIndex++) {
            final String hName = fileHeader[colIndex];
            // Find a matching pattern
            String beanName = null;
            final List<String> matchedPatterns = new LinkedList<String>();
            for (final BeanPropertyPattern beanProp : allBeanPatterns) {
                final String match = beanProp.match(hName);
                if (match != null) {
                    if (beanName == null) beanName = beanProp.getPropertyName();
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

        // Check that all the required properties have columns headers
        final List<String> headerList = Arrays.asList(headers);
        final List<BeanPropertyPattern> missingRequired = new LinkedList<>();
        for (final BeanPropertyPattern beanProp : getAllPropertyPatterns()) {
            if (beanProp.getRequired() && !headerList.contains(beanProp.getPropertyName()))
                missingRequired.add(beanProp);
        }
        if (missingRequired.size() > 0) {
            messages.add("Obligatoriska kolumner saknas:");
            for (final BeanPropertyPattern beanProp : missingRequired) {
                messages.add(beanProp.getImportDescription());
            }
            return null;
        }
        return headers;
    }

    /**
     * Create one-line descriptions for each bean property that can be recognized for import
     *
     * @return List of descriptions
     */
    public List<String> getPropertyDescriptions() {
        final List<String> propertyDescriptions = new LinkedList<>();
        for (final BeanPropertyPattern beanPattern : getAllPropertyPatterns()) {
            propertyDescriptions.add(beanPattern.getImportDescription());
        }
        return propertyDescriptions;
    }

    /**
     * Use heuristics to deduce the character encoding of buf. Use ASCII if nothing more specific is found.
     *
     * @param buf Contents to analyze
     * @param messages Append diagnostic messages describing encoding used and encoding problems found
     * @return An encoding name valid for use with e.g. InputStreamReader
     */
    public String detectEncoding(final byte[] buf, final List<String> messages) {
        final org.mozilla.universalchardet.UniversalDetector detector =
            new org.mozilla.universalchardet.UniversalDetector(null);
        detector.handleData(buf, 0, buf.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        if (encoding != null) {
        } else if (buf.length / (countMacRoman(buf) + 1) < 100) {
            // Frequency of Swedish MacRoman chars is > 1%
            encoding = "MacRoman";
        } else {
            encoding = "ASCII";
            System.out.printf("No encoding detected, using default '%s'.", encoding);
            messages.add(String.format("Filen innehåller en okänd teckenkodning, använder '%s' vid import.", encoding));
        }
        messages.add(String.format("Använder teckenkodning '%s' vid import.", encoding));
        detector.reset();
        return encoding;
    }

    // Swedish MacRoman characters, e.g. produced by Microsoft Excel on Mac OS X
    private static final byte[] macRomanChars = { (byte) 0x80, (byte) 0x81, (byte) 0x83, (byte) 0x85, (byte) 0x86,
        (byte) 0x8A, (byte) 0x8C, (byte) 0x8E, (byte) 0x9A, (byte) 0x9F };

    /**
     * Count Swedish MacRoman characters.
     *
     * @param buf
     * @return The number of Swedish MacRoman characters found in buf
     */
    private int countMacRoman(final byte[] buf) {
        int count = 0;
        for (final byte element : buf) {
            if (ArrayUtils.indexOf(macRomanChars, element) >= 0) count++;
        }
        return count;
    }

    /**
     * Read stream once,
     * deduce the character encoding,
     * deduce the column delimiter (comma or semicolon),
     * analyze the first row (header) to deduce the column-to-property mapping,
     * parse the remaining rows and create an JavaBean object for each row.
     *
     * @param stream CVS file contents
     * @param messages Diagnostic messages are appended when import problems are found
     * @return A list of objects of class T that have been parsed from stream or null if import fails
     */
    public List<T> importCSVstream(final InputStream stream, final List<String> messages) {
        final Class<T> beanClass = getBeanClass();
        List<T> beanList = null;
        byte[] contents = null;
        try {
            // Read stream once, because it may not be resettable
            contents = IOUtils.toByteArray(stream);
        } catch (final IOException e) {
            e.printStackTrace();
            messages.add("Kunde inte läsa filen.");
            return beanList;
        }
        final String encoding = detectEncoding(contents, messages);

        ICsvDozerBeanReader beanReader = null;
        try {
            CsvPreference csvPref = CsvPreference.STANDARD_PREFERENCE; // Comma separated
            if (ArrayUtils.indexOf(contents, (byte) ';') >= 0
                && ArrayUtils.indexOf(contents, (byte) ';') < ArrayUtils.indexOf(contents, (byte) ',')) {
                // Semicolon found before comma, assume it is used as CSV delimiter
                csvPref = CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE; // Semicolon separated
            }
            final Reader streamReader = new InputStreamReader(new ByteArrayInputStream(contents), encoding);
            beanReader = new CsvDozerBeanReader(streamReader, csvPref);

            final String[] fileHeader = beanReader.getHeader(true); // Expect first row to be a headline
            // Match bean properties to header names - one property name or null for each column position
            final String[] propertyNames = getHeaderPropertyNames(fileHeader, messages);
            if (propertyNames == null) return null;
            // Create import processors for all columns
            final CellProcessor[] processors = getProcessors(propertyNames, getRequiredProperties());
            beanReader.configureBeanMapping(beanClass, propertyNames);

            final List<T> tmpBeanList = new LinkedList<T>();
            T bean;
            while ((bean = beanReader.read(beanClass, processors)) != null) {
                tmpBeanList.add(bean);
            }
            beanList = tmpBeanList;
        } catch (final org.supercsv.exception.SuperCsvException e) {
            messages.add("Felaktigt filformat, förklaring:");
            messages.add(e.getMessage());
            System.out.println(e.getMessage());
        } catch (final UnsupportedEncodingException e) {
            messages.add(String.format("Filens teckenkodning '%s' stöds inte.", encoding));
            System.out.println(e.getMessage());
        } catch (final IOException e) {
            e.printStackTrace();
            messages.add(String.format("Kunde inte läsa innehållet."));
        } finally {
            if (beanReader != null) try {
                beanReader.close();
            } catch (final IOException e) {
            }
        }
        return beanList;
    }

    /**
     * Definition of one property in a class that follows the JavaBean convention.
     * Use the match() method to see if the property matches a header name.
     * Multiple regexp patterns are allowed.
     * This is redundant since a union of regexps could be used, but it makes the descriptions more understandable.
     *
     * @author Anders Pikas <anders@pikas.se>
     */
    static final class BeanPropertyPattern {
        private final String propertyName; // Name of Java property that has a getter and setter
        private final String description; // Description of the property
        private final Boolean required; // True if the property is required during import
        private final String[] headerPatterns; // Regexp patterns that match import headers
        private final Pattern[] compiledPatterns; // Compiled headerPatterns

        public BeanPropertyPattern(final String propertyName, final Boolean required, final String description,
            final String[] headerPatterns) {
            this.propertyName = propertyName;
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

        public String getPropertyName() {
            return this.propertyName;
        }

        public Boolean getRequired() {
            return this.required;
        }

        public String getDescription() {
            return this.description;
        }

        /**
         * Create a one-line description of patterns that are recognized as header for this bean property.
         *
         * @return
         */
        public String getImportDescription() {
            final StringBuffer buf = new StringBuffer();
            buf.append(String.format("%s: ", this.description));
            Boolean firstElement = true;
            for (final String pattern : this.headerPatterns) {
                if (!firstElement) buf.append(", ");
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
        public String match(final String header) {
            for (int i = 0; i < this.compiledPatterns.length; i++) {
                final Pattern p = this.compiledPatterns[i];
                if (p.matcher(header).find()) return this.headerPatterns[i];
            }
            return null;
        }
    }
}
