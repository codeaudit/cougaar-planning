/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.planning.ldm;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.cougaar.core.node.DBInitializerService;
import org.cougaar.util.DBConnectionPool;
import org.cougaar.util.DBProperties;
import org.cougaar.util.Parameters;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.NullLogger;

/**
 * Database initializer implementation that strictly allows initializing
 * assets using the non-CSMART / reference database structure.
 * @see AssetInitializerServiceComponent
 * @see DBAssetInitializerServiceProvider
 **/
public class NonCSMARTDBInitializerServiceImpl implements DBInitializerService {

  /** Query file located in planning/data/common **/
  public static final String QUERY_FILE = "NonCSMARTDBAssetInitializer.q";

  private final Logger logger;
  private final DBProperties dbp;
  private final String database;
  private final String username;
  private final String password;

  /**
   * Constructor creates a DBInitializer from the DBInitializer.q
   * query control file and sets up variables for referencing the database.
   * <p>
   * @param trialId the Trial identifier.
   */
  public NonCSMARTDBInitializerServiceImpl()
    throws SQLException, IOException
  {
    Logger l = Logging.getLogger(getClass());
    logger = ((l == null) ? NullLogger.getLogger() : l);

    dbp = DBProperties.readQueryFile(QUERY_FILE, "planning");
    database = dbp.getProperty("database");
    username = dbp.getProperty("username");
    password = dbp.getProperty("password");
    if (logger.isInfoEnabled()) {
      logger.info(
          "Will initialize assets from non-CSMART DB " + database);
    }

    try {
      String dbtype = dbp.getDBType();
      ensureDriverClass(dbtype);
    } catch (ClassNotFoundException e) {
      throw new SQLException("Driver not found for " + database);
    }
  }

  public Map createSubstitutions() {
    Map m = new HashMap(7);
    return m;
  }

  public String getNonNullString(ResultSet rs, int ix, String query)
    throws SQLException
  {
    String result = rs.getString(ix);
    if (result == null)
      throw new RuntimeException("Null in DB ix=" + ix + " query=" + query);
    return result;
  }

  public String getQuery(String queryName, Map substitutions) {
    return dbp.getQuery(queryName,  substitutions);
  }

  public Connection getConnection() throws SQLException {
    return DBConnectionPool.getConnection(database, username, password);
  }

  public ResultSet executeQuery(Statement stmt, String query) throws SQLException {
    try {
      boolean shouldLog = logger.isDebugEnabled();
      long startTime = (shouldLog ? 0L : System.currentTimeMillis());
      ResultSet rs = stmt.executeQuery(query);
      if (shouldLog) {
        long endTime = System.currentTimeMillis();
        logger.debug((endTime - startTime) + " " + query);
      }
      return rs;
    } catch (SQLException sqle) {
      if (logger.isErrorEnabled()) {
        logger.error("Query failed: "+query, sqle);
      }
      throw sqle;
    }
  }

  /**
   * Translate the value of a "query" attribute type. The "key"
   * should be one or more query substitutions. Each substitution is
   * an equals separated key and value. Multiple substitutions are
   * separated by semi-colon. Backslash can quote a character. The
   * query may be in a different database. If so, then the dbp
   * should contain properties named by concatenating the query
   * name with .database, .username, .password describing the
   * database to connect to.
   * @param type is the "data type" of the attribute value and
   * names a query that should be done to obtain the actual
   * value. 
   * @return a two-element array of attribute type and value.
   **/
  public Object[] translateAttributeValue(String type, String key) throws SQLException {
    Map substitutions = createSubstitutions();
    substitutions.put(":key:", key);
    String db = dbp.getProperty(type + ".database", database);
    String un = dbp.getProperty(type + ".username", username);
    String pw = dbp.getProperty(type + ".password", password);
    try {
      ensureDriverClass(dbp.getDBType(db));
    } catch (ClassNotFoundException cnfe) {
      throw new SQLException("Driver not found for " + db);
    }
    Connection conn = DBConnectionPool.getConnection(db, un, pw);
    try {
      Statement stmt = conn.createStatement();
      String query = dbp.getQueryForDatabase(type, substitutions, type + ".database");
      ResultSet rs = executeQuery(stmt, query);
      Object[] result = new Object[2];
      if (rs.next()) {
        result[0] = rs.getString(1);
        result[1] = rs.getString(2);
      } else {
        // It would be nice to not die if the GEOLOC or whatever
        // is not found. I'm just not certain
        // how the caller (ie AssetDataDBReader) will react
        // if the result is an empty String.
        // result[0] = type;
        // result[1] = "";
        throw new SQLException(
            "No row returned for attribute value query "+
            type+"("+key+")");
      }
      rs.close();
      stmt.close();
      return result;
    } finally {
      conn.close();
    }
  }

  private void ensureDriverClass(String dbtype) throws SQLException, ClassNotFoundException {
    String driverParam = "driver." + dbtype;
    String driverClass = Parameters.findParameter(driverParam);
    if (driverClass == null) {
      // this is likely a "cougaar.rc" problem.
      // Parameters should be modified to help generate this exception:
      throw new SQLException("Unable to find driver class for \""+
                             driverParam+"\" -- check your \"cougaar.rc\"");
    }
    Class.forName(driverClass);
  }

}
