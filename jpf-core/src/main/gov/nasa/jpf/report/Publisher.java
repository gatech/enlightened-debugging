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


package gov.nasa.jpf.report;

import gov.nasa.jpf.Config;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public abstract class Publisher {


  public static final int START = 1;
  public static final int TRANSITION = 2;
  public static final int PROBE = 3;
  public static final int CONSTRAINT = 4;
  public static final int PROPERTY_VIOLATION = 5;
  public static final int FINISHED = 6;

  protected Config conf;
  protected Reporter reporter; 

  protected String[] startItems = {};
  protected String[] transitionItems = {};
  protected String[] propertyViolationItems = {};
  protected String[] constraintItems = {};
  protected String[] finishedItems = {};
  protected String[] probeItems = {};

  DateFormat dtgFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT,
      DateFormat.SHORT);

  ArrayList<PublisherExtension> extensions;

  
  protected PrintWriter out;

  public PrintWriter getOut () {
    return out;
  }

  protected Publisher (Config conf, Reporter reporter){
    this.conf = conf;
    this.reporter = reporter;

    setTopicItems();
  }

  public void setItems (int category, String[] newTopics){
    switch (category){
    case START:
      startItems = newTopics; break;
    case PROBE:
      probeItems = newTopics; break;
    case TRANSITION:
      transitionItems = newTopics; break;
    case CONSTRAINT:
      constraintItems = newTopics; break;
    case PROPERTY_VIOLATION:
      propertyViolationItems = newTopics; break;
    case FINISHED:
      finishedItems = newTopics; break;
    default:
      Reporter.log.warning("unknown publisher topic: " + category);
    }
  }

  public abstract String getName();

  protected void setTopicItems () {
    setTopicItems(getName());
  }
  
  protected void setTopicItems (String name) {
    String prefix = "report." + name;
    startItems = conf.getStringArray(prefix + ".start", startItems);
    transitionItems = conf.getStringArray(prefix + ".transition", transitionItems);
    probeItems = conf.getStringArray(prefix + ".probe", transitionItems);
    propertyViolationItems = conf.getStringArray(prefix + ".property_violation", propertyViolationItems);
    constraintItems = conf.getStringArray(prefix + ".constraint", constraintItems);
    finishedItems = conf.getStringArray(prefix + ".finished", finishedItems);    
  }
  
  public void addExtension (PublisherExtension ext) {
    if (extensions == null) {
      extensions = new ArrayList<PublisherExtension>();
    }
    extensions.add(ext);
  }


  private static final List<PublisherExtension> EMPTY_LIST = new ArrayList<PublisherExtension>(0);
  
  public List<PublisherExtension> getExtensions(){
    if (extensions != null){
      return extensions;
    } else {
      return EMPTY_LIST; 
    }
  }
  
  public String getLastErrorId() {
    return reporter.getCurrentErrorId();
  }

  public boolean hasTopic (String topic) {
    for (String s : startItems) {
      if (s.equalsIgnoreCase(topic)){
        return true;
      }
    }
    for (String s : transitionItems) {
      if (s.equalsIgnoreCase(topic)){
        return true;
      }
    }
    for (String s : constraintItems) {
      if (s.equalsIgnoreCase(topic)){
        return true;
      }
    }
    for (String s : propertyViolationItems) {
      if (s.equalsIgnoreCase(topic)){
        return true;
      }
    }
    for (String s : finishedItems) {
      if (s.equalsIgnoreCase(topic)){
        return true;
      }
    }

    return false;
  }

  public String formatDTG (Date date) {
    return dtgFormatter.format(date);
  }

  

  static char[] tBuf = { '0', '0', ':', '0', '0', ':', '0', '0' };
  
  static synchronized public String formatHMS (long t) {
    int h = (int) (t / 3600000);
    int m = (int) ((t / 60000) % 60);
    int s = (int) ((t / 1000) % 60);
    
    tBuf[0] = (char) ('0' + (h / 10));
    tBuf[1] = (char) ('0' + (h % 10));
    
    tBuf[3] = (char) ('0' + (m / 10));
    tBuf[4] = (char) ('0' + (m % 10));
    
    tBuf[6] = (char) ('0' + (s / 10));
    tBuf[7] = (char) ('0' + (s % 10));
    
    return new String(tBuf);
  }
  
  public String getReportFileName (String key) {
    String fname = conf.getString(key);
    if (fname == null){
      fname = conf.getString("report.file");
      if (fname == null) {
        fname = "report";
      }
    }

    return fname;
  }

  public void publishTopicStart (String topic) {

  }

  public void publishTopicEnd (String topic) {

  }

  public boolean hasToReportStatistics() {
    for (String s : finishedItems) {
      if ("statistics".equalsIgnoreCase(s)){
        return true;
      }
    }
    return false;
  }


  protected void openChannel(){}
  protected void closeChannel(){}


  public void publishStart() {
    for (String item : startItems) {
      if ("jpf".equalsIgnoreCase(item)){
        publishJPF();
      } else if ("platform".equalsIgnoreCase(item)){
        publishPlatform();
      } else if ("user".equalsIgnoreCase(item)) {
      } else if ("dtg".equalsIgnoreCase(item)) {
        publishDTG();
      } else if ("config".equalsIgnoreCase(item)){
        publishJPFConfig();
      } else if ("sut".equalsIgnoreCase(item)){
        publishSuT();
      }
    }

    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishStart(this);
      }
    }
  }

  public void publishTransition() {
    for (String topic : transitionItems) {
      if ("statistics".equalsIgnoreCase(topic)){
        publishStatistics();
      }
    }
    
    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishTransition(this);
      }
    }
  }

  public void publishConstraintHit() {
    for (String item : constraintItems) {
      if ("constraint".equalsIgnoreCase(item)) {
        publishConstraint();
      } else if ("trace".equalsIgnoreCase(item)){
        publishTrace();
      } else if ("snapshot".equalsIgnoreCase(item)){
        publishSnapshot();
      } else if ("output".equalsIgnoreCase(item)){
        publishOutput();
      } else if ("statistics".equalsIgnoreCase(item)){
        publishStatistics(); 
      }
    }

    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishConstraintHit(this);
      }
    }
  }
  
  public void publishProbe(){
    for (String topic : probeItems) {
      if ("statistics".equalsIgnoreCase(topic)){
        publishStatistics();
      }
    }    
    
    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishProbe(this);
      }
    }
  }

  public void publishPropertyViolation() {

    for (String topic : propertyViolationItems) {
      if ("error".equalsIgnoreCase(topic)) {
        publishError();
      } else if ("trace".equalsIgnoreCase(topic)){
        publishTrace();
      } else if ("snapshot".equalsIgnoreCase(topic)){
        publishSnapshot();
      } else if ("output".equalsIgnoreCase(topic)){
        publishOutput();
      } else if ("statistics".equalsIgnoreCase(topic)){
        publishStatistics(); 
      }
    }

    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishPropertyViolation(this);
      }
    }

  }

  public void publishFinished() {
    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishFinished(this);
      }
    }

    for (String topic : finishedItems) {
      if ("result".equalsIgnoreCase(topic)){
        publishResult();
      } else if ("statistics".equalsIgnoreCase(topic)){
        publishStatistics();
      }
    }
  }

  protected void publishProlog() {} 
  protected void publishEpilog() {} 


  protected void publishJPF() {}
  protected void publishJPFConfig() {}
  protected void publishPlatform() {}
  protected void publishUser() {}
  protected void publishDTG() {}
  protected void publishJava() {}
  protected void publishSuT() {}
  protected void publishResult() {}
  protected void publishError() {}
  protected void publishConstraint() {}
  protected void publishTrace() {}
  protected void publishOutput() {}
  protected void publishSnapshot() {}
  protected void publishStatistics() {}


}
