/*
 * Copyright (c) 2007-2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 */

package org.broad.igv.dev.db;

import org.apache.log4j.Logger;
import org.broad.igv.session.SubtlyImportant;
import org.broad.igv.util.ParsingUtils;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.util.Utilities;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Object representation of the location of a database,
 * specifying address, tables, etc.
 * User: jacob
 * Date: 2013-Jan-14
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso(DBProfile.DBTable.class)
public class DBProfile {

    @XmlAttribute private String name;
    @XmlAttribute private String description;
    @XmlAttribute private String version;
    @XmlAttribute private String subprotocol;
    @XmlAttribute private String host;
    @XmlAttribute private String path;
    @XmlAttribute private String port;
    @XmlAttribute private String username;
    @XmlAttribute private String password;

    @XmlElement(name = "table")
    private List<DBTable> tableList;

    public List<DBTable> getTableList() {
        return tableList;
    }

    @XmlTransient
    private ResourceLocator dbLocator;

    public ResourceLocator getDBLocator() {
        if(dbLocator == null){
            dbLocator = new ResourceLocator(DBManager.createConnectionURL(subprotocol, host, path, port));
            dbLocator.setUsername(username);
            dbLocator.setPassword(password);
        }
        return dbLocator;
    }

    private static JAXBContext jc = null;
    public static JAXBContext getJAXBContext() throws JAXBException {
        if(jc == null){
            jc = JAXBContext.newInstance(DBProfile.class);
        }
        return jc;
    }

    public static DBProfile parseProfile(String profilePath){

        InputStream profileStream = null;
        try {
            profileStream = ParsingUtils.openInputStream(profilePath);
        } catch (IOException e) {
            try {
                if (profileStream != null) profileStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            throw new RuntimeException("Unable to open DB profile", e);
        }

        Document doc = null;
        try{
            doc = Utilities.createDOMDocumentFromXmlStream(profileStream);
        }catch (Exception e){
            throw new RuntimeException("Error parsing DB Profile", e);
        }

        try{
        JAXBContext jc = getJAXBContext();
        Unmarshaller u = jc.createUnmarshaller();

//        u.setListener(new Unmarshaller.Listener() {
//            @Override
//            public void afterUnmarshal(Object target, Object parent) {
//                if(target instanceof DBTable && parent instanceof DBProfile){
//                    ((DBTable) target).setDbLocator(((DBProfile) parent).getDBLocator());
//                }
//            }
//        });

        return u.unmarshal(doc, DBProfile.class).getValue();
        }catch(JAXBException e){
            throw new RuntimeException("Error unmarshalling DB Profile, it may be misformed", e);
        }
    }

    @SubtlyImportant
    public void afterUnmarshal(Unmarshaller u, Object parent) {
        dbLocator = getDBLocator();
        for (DBTable table : getTableList()) {
            table.setDbLocator(dbLocator);
        }
    }

    /**
     * Object representation of a single {@code table} element of
     * a database profile. Contains static method for parsing dbXML files
     * <p/>
     * User: jacob
     * Date: 2012-Oct-31
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class DBTable {

        private static Logger log = Logger.getLogger(DBTable.class);

        @XmlAttribute private String name;
        @XmlAttribute private String format;
        @XmlAttribute private String binColName;

        @XmlAttribute private String chromoColName;
        @XmlAttribute private String posStartColName;
        @XmlAttribute private String posEndColName;
        @XmlAttribute private int startColIndex;
        @XmlAttribute private int endColIndex;
        @XmlAttribute private String baseQuery;

        @XmlElement(name = "column")
        private List<Column> columnList;

        @XmlElement(name = "header")
        private List<String> headerLines;

        @XmlTransient
        private HashMap<Integer, String> columnLabelMap;

        @XmlTransient
        private ResourceLocator dbLocator;

        /**
         * Generally just intended for testing, where all we try
         * to do is get all data from a db and don't need anything fancy
         *
         * @param dbLocator
         * @param tableName
         */
        public static DBTable build(ResourceLocator dbLocator, String tableName) {
            return new DBTable(dbLocator, tableName, null, null, null, null, null, 1, Integer.MAX_VALUE - 1, null, null, null);
        }

        private DBTable(){}

        public DBTable(ResourceLocator dbLocator, String name, String format, String binColName,
                       String chromoColName, String posStartColName, String posEndColName, int startColIndex, int endColIndex,
                       HashMap<Integer, String> columnLabelMap, String baseQuery, List<String> headerLines) {
            this.dbLocator = dbLocator;
            this.name = name;
            this.format = format;
            this.binColName = binColName;
            this.chromoColName = chromoColName;
            this.posStartColName = posStartColName;
            this.posEndColName = posEndColName;
            this.startColIndex = startColIndex;
            this.endColIndex = endColIndex;
            this.columnLabelMap = columnLabelMap;
            this.baseQuery = baseQuery;
            this.headerLines = headerLines;
        }

        @SubtlyImportant
        public void afterUnmarshal(Unmarshaller u, Object parent) {
            if(columnList != null){
                columnLabelMap = ColumnMapAdapter.unmarshal(columnList);
            }
        }

        public ResourceLocator getDbLocator() {
            return dbLocator;
        }

        public String getBinColName() {
            return binColName;
        }

        public String getChromoColName() {
            return chromoColName;
        }

        public int getEndColIndex() {
            return endColIndex;
        }

        public String getFormat() {
            return format;
        }

        public String getPosEndColName() {
            return posEndColName;
        }

        public String getPosStartColName() {
            return posStartColName;
        }

        public int getStartColIndex() {
            return startColIndex;
        }

        public String getName() {
            return name;
        }

        public String getBaseQuery() {
            return baseQuery;
        }

        public Map<Integer, String> getColumnLabelMap() {
            return columnLabelMap;
        }

        public List<String> getHeaderLines() {
            return headerLines;
        }

        void setDbLocator(ResourceLocator dbLocator) {
            this.dbLocator = dbLocator;
        }

        /**
         * Return an array of column labels in specified ordinal positions
         *
         * @return
         */
        public static String[] columnMapToArray(Map<Integer, String> columnLabelMap) {
            List<Integer> arrayIndexes = new ArrayList<Integer>(columnLabelMap.keySet());
            Collections.sort(arrayIndexes);
            int minArrayIndex = arrayIndexes.get(0);
            int maxArrayIndex = arrayIndexes.get(arrayIndexes.size() - 1);
            int colCount = maxArrayIndex + 1;
            String[] tokens = new String[colCount];

            for (int cc = minArrayIndex; cc < maxArrayIndex; cc++) {
                tokens[cc] = columnLabelMap.get(cc);
            }
            return tokens;
        }

        private static class ColumnMapAdapter extends XmlAdapter<XmlMap, HashMap<Integer, String>> {
            @Override
            public HashMap<Integer, String> unmarshal(XmlMap v){
                return unmarshal(v.column);
            }

            public static HashMap<Integer, String> unmarshal(List<Column> columnList){
                HashMap<Integer, String> result = new HashMap<Integer, String>(columnList.size());
                for(Column entry: columnList){
                    result.put(entry.fileIndex, entry.colLabel);
                }
                return result;
            }

            @Override
            public XmlMap marshal(HashMap<Integer, String> v){
                XmlMap result = new XmlMap();
                for(Map.Entry<Integer, String> entry: v.entrySet()){
                    Column mapEntry = new Column();
                    mapEntry.fileIndex = entry.getKey();
                    mapEntry.colLabel = entry.getValue();
                    result.column.add(mapEntry);
                }
                return result;
            }
        }

        private static class XmlMap {
            public List<Column> column =
                    new ArrayList<Column>();
        }

        private static class Column {
            @XmlAttribute private Integer fileIndex;
            @XmlAttribute private String colLabel;
        }
    }
}
