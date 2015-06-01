/*
 * ashurbanipal.web: Java Servlet-based interface to Ashurbanipal data
 * Copyright 2015 Tommy M. McGuire
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package net.crsr.ashurbanipal.web.resources;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.crsr.ashurbanipal.web.exceptions.InternalServerException;
import net.crsr.ashurbanipal.web.exceptions.ResultNotFound;

import org.apache.wink.common.annotations.Workspace;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/lookup")
@Workspace(workspaceTitle="Text Lookup", collectionTitle="Author, Title, Subject query")
public class TextLookup {
  
//  private static final String ROW_QUERY = "select * from book_metadata where upper(title) like ? or upper(author) like ? or upper(subject) like ? order by author, title, etext_no limit ? offset ?";
//  private static final String COUNT_QUERY = "select count(*) as count from book_metadata where upper(title) like ? or upper(author) like ? or upper(subject) like ?";

  private static final String ROW_QUERY = "select * from book_metadata where position(? in upper(title)) > 0 or position(? in upper(author)) > 0 or position(? in upper(subject)) > 0 order by author, title, etext_no limit ? offset ?";
  private static final String COUNT_QUERY = "select count(*) as count from book_metadata where position(? in upper(title)) > 0 or position(? in upper(author)) > 0 or position(? in upper(subject)) > 0";
  private static final String SINGLE_QUERY = "select * from book_metadata where etext_no = ?";
  
  private static final Logger log = LoggerFactory.getLogger(TextLookup.class);
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getQuery(
      @QueryParam("query") String searchTerm,
      @QueryParam("start") @DefaultValue("0") Integer start,
      @QueryParam("limit") @DefaultValue("20") Integer limit) {
    if (searchTerm == null) {
      final String message = "bad request: parameter query=\"search-term\" required";
      log.info(message);
      throw new WebApplicationException(Response.status(BAD_REQUEST).entity(message).build());
    }
    final String upperSearchTerm = /* "%" + */ searchTerm.toUpperCase() /* + "%" */;
    Connection connection = null;
    try {
      final Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
      final DataSource dataSource = (DataSource) envCtx.lookup("jdbc/AshurbanipalDB");
      connection = dataSource.getConnection();
      final JSONObject results = new JSONObject();
      results.put("count", countQuery(connection, upperSearchTerm));
      results.put("rows", rowQuery(connection, upperSearchTerm, start, limit));
      return results;
    } catch (SQLException e) {
      throw new InternalServerException("sql exception", e);
    } catch (JSONException e) {
      throw new InternalServerException("json exception", e);
    } catch (NamingException e) {
      throw new InternalServerException("jndi exception", e);
    } finally {
      if (connection != null) { try { connection.close(); } catch (SQLException e) { } }
    }
  }
  
  @Path("/{etext_no}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getByEtextNo(@PathParam("etext_no") Integer etext_no) {
    if (etext_no == null) {
      final String message = "bad request: etext_no required";
      log.info(message);
      throw new WebApplicationException(Response.status(BAD_REQUEST).entity(message).build());
    }
    Connection connection = null;
    try {
      final Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
      final DataSource dataSource = (DataSource) envCtx.lookup("jdbc/AshurbanipalDB");
      connection = dataSource.getConnection();
      final JSONObject results = singleBookQuery(connection, etext_no);
      return results;
    } catch (SQLException e) {
      throw new InternalServerException("sql exception", e);
    } catch (NamingException e) {
      throw new InternalServerException("jndi exception", e);
    } catch (JSONException e) {
      throw new InternalServerException("json exception", e);
    } finally {
      if (connection != null) { try { connection.close(); } catch (SQLException e) { } }
    }
  }
  
  private int countQuery(Connection connection, String upperSearchTerm) throws SQLException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    try {
      statement = connection.prepareStatement(COUNT_QUERY);
      setSearchTerms(statement, upperSearchTerm);
      resultSet = statement.executeQuery();
      if (resultSet.next()) {
        return resultSet.getInt(1);
      } else {
        throw new InternalServerException("no count in result");
      }
    } finally {
      if (resultSet != null) { try { resultSet.close(); } catch (SQLException e) { } }
      if (statement != null) { try { statement.close(); } catch (SQLException e) { } }
    }
  }
  
  private List<JSONObject> rowQuery(Connection connection, String upperSearchTerm, Integer start, Integer limit) throws SQLException, JSONException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    try {
      statement = connection.prepareStatement(ROW_QUERY);
      setSearchTerms(statement, upperSearchTerm);
      statement.setInt(4, limit);
      statement.setInt(5, start);
      resultSet = statement.executeQuery();
      final ResultSetMetaData metadata = resultSet.getMetaData();
      final int columns = metadata.getColumnCount();
      final List<JSONObject> results = new ArrayList<>();
      while (resultSet.next()) {
        results.add(rowToMap(metadata, columns, resultSet));
      }
      return results;
    } finally {
      if (resultSet != null) { try { resultSet.close(); } catch (SQLException e) { } }
      if (statement != null) { try { statement.close(); } catch (SQLException e) { } }
    }
  }
  
  private JSONObject singleBookQuery(Connection connection, Integer etext_no) throws SQLException, JSONException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    try {
      statement = connection.prepareStatement(SINGLE_QUERY);
      statement.setInt(1, etext_no);
      resultSet = statement.executeQuery();
      if (resultSet.next()) {
        final ResultSetMetaData metadata = resultSet.getMetaData();
        return rowToMap(metadata, metadata.getColumnCount(), resultSet);
      } else {
        throw new ResultNotFound();
      }
    } finally {
      if (resultSet != null) { try { resultSet.close(); } catch (SQLException e) { } }
      if (statement != null) { try { statement.close(); } catch (SQLException e) { } }
    }
  }

  private JSONObject rowToMap(final ResultSetMetaData metadata, final int columns, ResultSet resultSet) throws SQLException, JSONException {
    final JSONObject result = new JSONObject();
    for (int i = 1; i <= columns; ++i) {
      switch (metadata.getColumnType(i)) {
        case Types.INTEGER:
          result.put(metadata.getColumnLabel(i), resultSet.getInt(i));
          break;
        default:
          result.put(metadata.getColumnLabel(i), resultSet.getString(i));
          break;
      }
    }
    return result;
  }

  private void setSearchTerms(PreparedStatement statement, String upperSearchTerm) throws SQLException {
    statement.setString(1, upperSearchTerm);
    statement.setString(2, upperSearchTerm);
    statement.setString(3, upperSearchTerm);
  }

}
