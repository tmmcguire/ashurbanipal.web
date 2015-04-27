package net.crsr.ashurbanipal.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import net.crsr.ashurbanipal.web.resources.StyleList;
import net.crsr.ashurbanipal.web.resources.TextLookup;
import net.crsr.ashurbanipal.web.resources.TopicList;

public class AshurbanipalWeb extends Application {
  
  @Override
  public Set<Class<?>> getClasses() {
    return new HashSet<Class<?>>(
        Arrays.asList(
            StyleList.class,
            TextLookup.class,
            TopicList.class
            )
        );
    
  }

}
