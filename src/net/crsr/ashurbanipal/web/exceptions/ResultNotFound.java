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