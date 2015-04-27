package net.crsr.ashurbanipal.web.exceptions;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@SuppressWarnings("serial")
public class InternalServerException extends WebApplicationException {

  public InternalServerException() {
    super(INTERNAL_SERVER_ERROR);
  }

  public InternalServerException(Throwable cause) {
    super(cause, INTERNAL_SERVER_ERROR);
  }
  
  public InternalServerException(String message) {
    super( Response.status(INTERNAL_SERVER_ERROR).entity(message).build() );
  }
  
  public InternalServerException(String message, Throwable e) {
    super(e, Response.status(INTERNAL_SERVER_ERROR).entity(message + ": " + e.getMessage()).build() );
  }
  
}
