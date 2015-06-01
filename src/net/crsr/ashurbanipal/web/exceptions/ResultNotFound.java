package net.crsr.ashurbanipal.web.exceptions;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@SuppressWarnings("serial")
public class ResultNotFound extends WebApplicationException {

  public ResultNotFound() {
    super(NOT_FOUND);
  }

  public ResultNotFound(Throwable cause) {
    super(cause, NOT_FOUND);
  }
  
  public ResultNotFound(String message) {
    super( Response.status(NOT_FOUND).entity(message).build() );
  }
  
  public ResultNotFound(String message, Throwable e) {
    super(e, Response.status(NOT_FOUND).entity(message + ": " + e.getMessage()).build() );
  }

}
