/*
ImpressiveCode Depress Framework
Copyright (C) 2013  ImpressiveCode contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.impressivecode.depress.its.jira;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.impressivecode.depress.its.ITSDataType;
import org.impressivecode.depress.its.ITSPriority;
import org.impressivecode.depress.its.ITSResolution;
import org.impressivecode.depress.its.ITSStatus;
import org.impressivecode.depress.its.ITSType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Lists;

/**
 * @author Marek Majchrzak, ImpressiveCode
 * @author Maciej Borkowski, Capgemini Poland
 */
public class JiraEntriesParser {
    private static final String JIRA_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    private final HashMap<String, String[]> settings;
    private boolean priorityEnabled;
    private boolean typeEnabled;
    private boolean resolutionEnabled;

    public JiraEntriesParser(final HashMap<String, String[]> settings, final boolean priorityEnabled,
            final boolean typeEnabled, final boolean resolutionEnabled) {
        this.settings = settings;
        this.priorityEnabled = priorityEnabled;
        this.typeEnabled = typeEnabled;
        this.resolutionEnabled = resolutionEnabled;
    }

    public List<ITSDataType> parseEntries(final String path) throws ParserConfigurationException, SAXException,
            IOException, ParseException {
        Preconditions.checkArgument(!isNullOrEmpty(path), "Path has to be set.");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(path);
        NodeList nList = getItemNodes(doc);
        int size = nList.getLength();
        List<ITSDataType> entries = Lists.newLinkedList();
        for (int i = 0; i < size; i++) {
            Node item = nList.item(i);
            ITSDataType entry = parse((Element) item);
            entries.add(entry);
        }
        return entries;
    }

    private NodeList getItemNodes(final Document doc) {
        return doc.getElementsByTagName("item");
    }

    private ITSDataType parse(final Element elem) throws ParseException {
        ITSDataType data = new ITSDataType();
        data.setIssueId(getKey(elem));
        data.setComments(getComments(elem));
        data.setCreated(getCreated(elem));
        data.setDescription(getDescription(elem));
        data.setFixVersion(getFixVersion(elem));
        data.setLink(getLink(elem));
        data.setPriority(getPriority(elem));
        data.setResolved(getResolved(elem));
        data.setStatus(getStatus(elem));
        data.setSummary(getSummary(elem));
        data.setType(getType(elem));
        data.setUpdated(getUpdated(elem));
        data.setVersion(getVersion(elem));
        data.setResolution(getResolution(elem));
        data.setReporter(getReporter(elem));
        data.setAssignees(getAssinees(elem));
        data.setCommentAuthors(getCommentAuthors(elem));
        return data;
    }

    private Set<String> getCommentAuthors(final Element elem) {
        NodeList nodeList = elem.getElementsByTagName("comment");
        Builder<String> authors = ImmutableSet.builder();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            authors.add(item.getAttributes().getNamedItem("author").getTextContent());
        }
        return authors.build();
    }

    private Set<String> getAssinees(final Element elem) {
        NodeList nodeList = elem.getElementsByTagName("assignee");
        Preconditions.checkArgument(nodeList.getLength() == 1, "Reporter has to be set");
        String username = nodeList.item(0).getAttributes().getNamedItem("username").getTextContent();
        if ("-1".equals(username)) {
            return Collections.emptySet();
        } else {
            return ImmutableSet.of(username);
        }
    }

    private String getReporter(final Element elem) {
        NodeList nodeList = elem.getElementsByTagName("reporter");
        Preconditions.checkArgument(nodeList.getLength() == 1, "Reporter has to be set");
        String username = nodeList.item(0).getAttributes().getNamedItem("username").getTextContent();
        return username;
    }

    private ITSResolution getResolution(final Element elem) {
        String resolution = extractValue(elem, "resolution");
        if (resolution == null) {
            return ITSResolution.UNKNOWN;
        }
        if (resolutionEnabled) {
            for (String key : settings.keySet()) {
                for (String value : settings.get(key)) {
                    if (resolution.equals(value))
                        return ITSResolution.get(key);
                }
            }
        } else {
            for (String label : ITSResolution.labels()) {
                if (label.equals(resolution)) {
                    return ITSResolution.get(label);
                }
            }
        }

        return ITSResolution.UNKNOWN;
    }

    private List<String> getVersion(final Element elem) {
        return extractValues(elem, "version");
    }

    private Date getUpdated(final Element elem) throws ParseException {
        return extractDateValue(elem, "updated");
    }

    private ITSType getType(final Element elem) {
        String type = extractValue(elem, "type");
        if (type == null) {
            return ITSType.UNKNOWN;
        }
        if (typeEnabled) {
            for (String key : settings.keySet()) {
                for (String value : settings.get(key)) {
                    if (type.equals(value))
                        return ITSType.get(key);
                }
            }
        } else {
            for (String label : ITSType.labels()) {
                if (label.equals(type)) {
                    return ITSType.get(label);
                }
            }
        }
        return ITSType.UNKNOWN;
    }

    private String getSummary(final Element elem) {
        return extractValue(elem, "summary");
    }

    private ITSStatus getStatus(final Element elem) {
        String status = extractValue(elem, "status");
        if (status == null) {
            return ITSStatus.UNKNOWN;
        }
        switch (status) {
        case "Open":
            return ITSStatus.OPEN;
        case "Reopened":
            return ITSStatus.REOPENED;
        case "In Progress":
            return ITSStatus.IN_PROGRESS;
        case "Resolved":
            return ITSStatus.RESOLVED;
        case "Closed":
            return ITSStatus.CLOSED;
        default:
            return ITSStatus.UNKNOWN;
        }
    }

    private Date getResolved(final Element elem) throws ParseException {
        return extractDateValue(elem, "resolved");
    }

    private ITSPriority getPriority(final Element elem) {
        String priority = extractValue(elem, "priority");
        if (priority == null) {
            return ITSPriority.UNKNOWN;
        }
        if (priorityEnabled) {
            for (String key : settings.keySet()) {
                for (String value : settings.get(key)) {
                    if (priority.equals(value))
                        return ITSPriority.get(key);
                }
            }
        } else {
            for (String label : ITSPriority.labels()) {
                if (label.equals(priority)) {
                    return ITSPriority.get(label);
                }
            }
        }
        return ITSPriority.UNKNOWN;
    }

    private String getLink(final Element elem) {
        return extractValue(elem, "link");
    }

    private List<String> getFixVersion(final Element elem) {
        return extractValues(elem, "fixVersion");
    }

    private String getDescription(final Element elem) {
        return extractValue(elem, "description");
    }

    private Date getCreated(final Element elem) throws ParseException {
        return extractDateValue(elem, "created");
    }

    private List<String> getComments(final Element elem) {
        return extractValues(elem, "comment");
    }

    private List<String> extractValues(final Element elem, final String tagName) {
        NodeList nodeList = elem.getElementsByTagName(tagName);
        int size = nodeList.getLength();
        List<String> values = Lists.newLinkedList();
        for (int i = 0; i < size; i++) {
            String value = nodeList.item(i).getFirstChild().getNodeValue().trim();
            values.add(value);
        }
        return values;
    }

    private String getKey(final Element elem) {
        return extractValue(elem, "key");
    }

    private String extractValue(final Element elem, final String tagName) {
        NodeList nodeList = elem.getElementsByTagName(tagName);
        if (nodeList.getLength() == 0) {
            return null;
        }
        Node firstChild = elem.getElementsByTagName(tagName).item(0).getFirstChild();
        return firstChild == null ? null : firstChild.getNodeValue().trim();
    }

    private Date parseDate(final String nodeValue) throws ParseException {
        // Mon, 16 Feb 2004 00:29:19 +0000
        // FIXME majchmar: fix time parsing, timezone
        SimpleDateFormat sdf = new SimpleDateFormat(JIRA_DATE_FORMAT, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+000"));
        sdf.setLenient(true);
        Date date = sdf.parse(nodeValue);
        return date;
    }

    private Date extractDateValue(final Element elem, final String tagName) throws ParseException {
        String value = extractValue(elem, tagName);
        return value == null ? null : parseDate(value);
    }
}