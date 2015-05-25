package net.crsr.ashurbanipal.web.resources;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

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

@Path("/combination")
@Workspace(workspaceTitle="Text Metrics", collectionTitle="Text topics")
public class CombinationList {
  
  private static final String COUNT_QUERY = "select count(*) from book_metadata";
  private static final String SEARCH_QUERY = "select tc.dist_score as dist, bm.* from combination_scores(?) tc inner join book_metadata bm on (tc.etext_no = bm.etext_no) order by tc.dist_score offset ? limit ?";
  
  private static final Logger log = LoggerFactory.getLogger(CombinationList.class);

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getTextList(@QueryParam("etext_no") Integer etext_no, @QueryParam("start") Integer start, @QueryParam("limit") Integer limit) {
    if (etext_no == null) {
      final String message = "bad request: parameter q=\"search-term\" required";
      log.info(message);
      throw new WebApplicationException(Response.status(BAD_REQUEST).entity(message).build());
    }
    if (start == null) { start = 0; }
    if (limit == null) { limit = 20; }
    Connection connection = null;
    try {
      final Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
      final DataSource dataSource = (DataSource) envCtx.lookup("jdbc/AshurbanipalDB");
      connection = dataSource.getConnection();
      final JSONObject results = new JSONObject();
      results.put("count", countQuery(connection));
      results.put("rows", rowQuery(connection, etext_no, start, limit));
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
  
  private Integer countQuery(Connection connection) throws SQLException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    try {
      statement = connection.prepareStatement(COUNT_QUERY);
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

  private List<Map<String,Object>> rowQuery(Connection connection, Integer etext_no, Integer start, Integer limit) throws SQLException {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    try {
      statement = connection.prepareStatement(SEARCH_QUERY);
      statement.setInt(1, etext_no);
      statement.setInt(2, start);
      statement.setInt(3, limit);
      resultSet = statement.executeQuery();
      final ResultSetMetaData metadata = resultSet.getMetaData();
      final int columns = metadata.getColumnCount();
      final List<Map<String,Object>> results = new ArrayList<>();
      while (resultSet.next()) {
        final Map<String,Object> result = new HashMap<>();
        results.add(result);
        for (int i = 1; i <= columns; ++i) {
          switch (metadata.getColumnType(i)) {
            case Types.BIGINT:
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
              result.put(metadata.getColumnLabel(i), resultSet.getInt(i));
              break;
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
              result.put(metadata.getColumnLabel(i), resultSet.getDouble(i));
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

}
