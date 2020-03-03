/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The Java Pathfinder core (jpf-core) platform is licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */


package gov.nasa.jpf.util;

import gov.nasa.jpf.Config;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


public class LogManager {
    
  static HashMap<String,JPFLogger> loggers = new HashMap<String, JPFLogger>(); 
  
  static Level defaultLevel = Level.WARNING;
  static Handler handler;  
  

  static String[] activeSevere;
  static String[] activeWarning;
  static String[] activeInfo;
  static String[] activeConfig;
  static String[] activeFine;
  static String[] activeFiner;
  static String[] activeFinest;
  
  
  public static void init (Config conf) {
    try {
      defaultLevel = Level.parse( conf.getString("log.level", "INFO").toUpperCase());
    } catch (Throwable x) {
      defaultLevel = Level.WARNING;
    }
    
    activeSevere = conf.getStringArray("log.severe");
    activeWarning = conf.getStringArray("log.warning");
    activeInfo = conf.getStringArray("log.info");
    activeConfig = conf.getStringArray("log.config");
    activeFine = conf.getStringArray("log.fine");
    activeFiner = conf.getStringArray("log.finer");
    activeFinest = conf.getStringArray("log.finest");
    
    handler = conf.getInstance("log.handler.class", Handler.class);
  }
  
  static boolean checkInclusion (String[] actives, String name) {
    if (actives == null) {
      return false;
    }
    
    for (int i=0; i<actives.length; i++) {
      if (name.matches(actives[i])) {
        return true;
      }
    }
    
    return false;
  }
  
  static Level getLevel (String name) {
    if (checkInclusion(activeSevere, name)) return Level.SEVERE;
    if (checkInclusion(activeWarning, name)) return Level.WARNING;
    if (checkInclusion(activeInfo, name)) return Level.INFO;
    if (checkInclusion(activeConfig, name)) return Level.CONFIG;
    if (checkInclusion(activeFine, name)) return Level.FINE;
    if (checkInclusion(activeFiner, name)) return Level.FINER;
    if (checkInclusion(activeFinest, name)) return Level.FINEST;
    
    return defaultLevel;
  }
  
  public static JPFLogger getLogger (String name) {
    if (handler == null){

      handler = new LogHandler.DefaultConsoleHandler();
    }


    JPFLogger logger = loggers.get(name);
    
    if (logger == null) {

      Logger baseLogger = Logger.getLogger(name);
      baseLogger.setLevel( getLevel(name));
      baseLogger.addHandler(handler);
      baseLogger.setUseParentHandlers(false); 
      

      logger = new JPFLogger(baseLogger);
      loggers.put(name, logger);
    }
    
    return logger;
  }
  
  public static void setOutput(OutputStream out) {

    if (handler instanceof LogHandler){
      ((LogHandler)handler).setOutput(out);
    }
  }
  
  public static void printStatus (Logger log) {
    if (handler instanceof LogHandler){
      ((LogHandler)handler).printStatus(log);
    }
  }
}
