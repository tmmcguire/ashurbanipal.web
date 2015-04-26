package net.crsr.ashurbanipal.web;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class AshurbanipalWeb extends Application {
  
  @Override
  public Set<Class<?>> getClasses() {
    return new HashSet<Class<?>>(
        Arrays.asList(
            HelloWorld.class
            )
        );
    
  }

}
