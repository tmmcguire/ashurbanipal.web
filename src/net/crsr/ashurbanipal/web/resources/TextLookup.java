package net.crsr.ashurbanipal.web.resources;

import static javax.ws.rs.core.Response.Status.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.crsr.ashurbanipal.web.exceptions.InternalServerException;

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
  
  private static final Logger log = LoggerFactory.getLogger(TextLookup.class);
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getQuery(@QueryParam("q") String searchTerm, @QueryParam("start") Integer start, @QueryParam("limit") Integer limit) {
    if (searchTerm == null) {
      final String message = "bad request: parameter q=\"search-term\" required";
      log.info(message);
      throw new WebApplicationException(Response.status(BAD_REQUEST).entity(message).build());
    }
    final String upperSearchTerm = /* "%" + */ searchTerm.toUpperCase() /* + "%" */;
    if (start == null) { start = 1; }
    if (limit == null) { limit = 20; }
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
  
  private List<Map<String,Object>> rowQuery(Connection connection, String upperSearchTerm, Integer start, Integer limit) throws SQLException {
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
      final List<Map<String,Object>> results = new ArrayList<>();
      while (resultSet.next()) {
        final Map<String,Object> result = new HashMap<>();
        results.add(result);
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
      }
      return results;
    } finally {
      if (resultSet != null) { try { resultSet.close(); } catch (SQLException e) { } }
      if (statement != null) { try { statement.close(); } catch (SQLException e) { } }
    }
  }

  private void setSearchTerms(PreparedStatement statement, String upperSearchTerm) throws SQLException {
    statement.setString(1, upperSearchTerm);
    statement.setString(2, upperSearchTerm);
    statement.setString(3, upperSearchTerm);
  }

}
