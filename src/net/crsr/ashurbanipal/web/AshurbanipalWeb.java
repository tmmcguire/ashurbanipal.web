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

package net.crsr.ashurbanipal.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import net.crsr.ashurbanipal.web.resources.CombinationList;
import net.crsr.ashurbanipal.web.resources.StyleList;
import net.crsr.ashurbanipal.web.resources.TextLookup;
import net.crsr.ashurbanipal.web.resources.TopicList;

public class AshurbanipalWeb extends Application {
  
  @Override
  public Set<Class<?>> getClasses() {
    return new HashSet<Class<?>>(
        Arrays.asList(
            CombinationList.class,
            StyleList.class,
            TextLookup.class,
            TopicList.class
            )
        );
    
  }

}
