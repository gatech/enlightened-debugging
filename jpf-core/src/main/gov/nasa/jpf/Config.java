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


package gov.nasa.jpf;


import gov.nasa.jpf.util.FileUtils;
import gov.nasa.jpf.util.JPFSiteUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;





@SuppressWarnings("serial")
public class Config extends Properties {

  static final char   KEY_PREFIX = '@';
  public static final String REQUIRES_KEY = "@requires";
  public static final String INCLUDE_KEY = "@include";
  public static final String INCLUDE_UNLESS_KEY = "@include_unless";
  public static final String INCLUDE_IF_KEY = "@include_if";
  public static final String USING_KEY = "@using";

  static final String[] EMPTY_STRING_ARRAY = new String[0];

  public static final String LIST_SEPARATOR = ",";
  public static final String PATH_SEPARATOR = ","; 

  public static final Class<?>[] CONFIG_ARGTYPES = { Config.class };  
  public static final Class<?>[] NO_ARGTYPES = new Class<?>[0];
  public static final Object[] NO_ARGS = new Object[0];

  public static final String TRUE = "true";
  public static final String FALSE = "false";
  
  static final String MAX = "MAX";

  static final String IGNORE_VALUE = "-";


  public static int MAX_NUM_PRC = 16;


  public static boolean log = false;


  static class MissingRequiredKeyException extends RuntimeException {
    MissingRequiredKeyException(String details){
      super(details);
    }
  }




  ClassLoader loader = Config.class.getClassLoader();
    

  ArrayList<Object> sources = new ArrayList<Object>();
  
  ArrayList<ConfigChangeListener> changeListeners;
  

  LinkedList<String> entrySequence = new LinkedList<String>();


  HashMap<String,Object> singletons;
  
  public final Object[] CONFIG_ARGS = { this };


  String[] args;
  

  String[] freeArgs;

  
  public Config (String[] cmdLineArgs)  {
    args = cmdLineArgs;
    String[] a = cmdLineArgs.clone(); 


    String appProperties = getAppPropertiesLocation(a);


    String siteProperties = getSitePropertiesLocation( a, appProperties);
    if (siteProperties != null){
      loadProperties( siteProperties);
    }


    loadProjectProperties();


    if (appProperties != null){
      loadProperties( appProperties);
    }


    loadArgs(a);





  }

  private Config() {

  }
  
  
  public Config (String fileName){
    loadProperties(fileName);
  }

  public Config (Reader in){
    try {
      load(in);
    } catch (IOException iox){
      exception("error reading data: " + iox);
    }
  }
  
  public static void enableLogging (boolean enableLogging){
    log = enableLogging;
  }

  public void log (String msg){
    if (log){ 
      System.out.println(msg);
    }
  }


  String getAppPropertiesLocation(String[] args){
    String path = null;

    path = getPathArg(args, "app");
    if (path == null){

      path = getAppArg(args);
    }
    
    put("jpf.app", path);

    return path;
  }

  String getSitePropertiesLocation(String[] args, String appPropPath){
    String path = getPathArg(args, "site");

    if (path == null){



      if (appPropPath != null){
        path = JPFSiteUtils.getMatchFromFile(appPropPath,"site");
      }

      if (path == null) {
        File siteProps = JPFSiteUtils.getStandardSiteProperties();
        if (siteProps != null){
          path = siteProps.getAbsolutePath();
        }
      }
    }
    
    put("jpf.site", path);

    return path;
  }



  public Config reload() {
    log("reloading config");


    Config newConfig = new Config();
    for (Object src : sources){
      if (src instanceof File) {
        newConfig.loadProperties(((File)src).getPath());
      } else if (src instanceof URL) {
        newConfig.loadProperties((URL)src);
      } else {
        log("don't know how to reload: " + src);
      }
    }


    newConfig.loadArgs(args);
    newConfig.args = args;
    
    return newConfig;
  }

  public String[] getArgs() {
    return args;
  }

  
  protected String getPathArg (String[] args, String key){
    int keyLen = key.length();

    for (int i=0; i<args.length; i++){
      String a = args[i];
      if (a != null){
        int len = a.length();
        if (len > keyLen + 2){
          if (a.charAt(0) == '+' && a.charAt(keyLen+1) == '='){
            if (a.substring(1, keyLen+1).equals(key)){
              String val = expandString(key, a.substring(keyLen+2));
              args[i] = null; 
              return val;
            }
          }
        }
      }
    }

    return null;
  }

  
  protected String getAppArg (String[] args){

    for (int i=0; i<args.length; i++){
      String a = args[i];
      if (a != null && a.length() > 0){
        switch (a.charAt(0)) {
          case '+': continue;
          case '-': continue;
          default:
            if (a.endsWith(".jpf")){
              String val = expandString("jpf.app", a);
              args[i] = null; 
              return val;
            }
        }
      }
    }

    return null;
  }


  protected void loadProperties (URL url){
    log("loading defaults from: " + url);

    InputStream is = null;
    try {
      is = url.openStream();
      load(is);
      sources.add(url);
    } catch (IOException iox){
      log("error in input stream for: " + url + " : " + iox.getMessage());
    } finally {
      if (is != null){
        try {
          is.close();
        } catch (IOException iox1){
          log("error closing input stream for: " + url + " : " + iox1.getMessage());
        }
      }
    }
  }

  protected void setConfigPathProperties (String fileName){
    put("config", fileName);
    int i = fileName.lastIndexOf(File.separatorChar);
    if (i>=0){
      put("config_path", fileName.substring(0,i));
    } else {
      put("config_path", ".");
    }
  }


  protected boolean loadProperties (String fileName) {
    if (fileName != null && fileName.length() > 0) {
      FileInputStream is = null;
      try {
        File f = new File(fileName);
        if (f.isFile()) {
          log("loading property file: " + fileName);

          setConfigPathProperties(f.getAbsolutePath());
          sources.add(f);
          is = new FileInputStream(f);
          load(is);
          return true;
        } else {
          throw exception("property file does not exist: " + f.getAbsolutePath());
        }
      } catch (MissingRequiredKeyException rkx){

        log("missing required key: " + rkx.getMessage() + ", skipping: " + fileName);
      } catch (IOException iex) {
        throw exception("error reading properties: " + fileName);
      } finally {
        if (is != null){
          try {
            is.close();
          } catch (IOException iox1){
            log("error closing input stream for file: " + fileName);
          }
        }
      }
    }

    return false;
  }


  
  protected void loadProjectProperties () {


    LinkedList<File> jpfDirs = new LinkedList<File>();



    addJPFdirsFromClasspath(jpfDirs);


    addJPFdirsFromSiteExtensions(jpfDirs);



    addCurrentJPFdir(jpfDirs);



    for (File dir : jpfDirs){
      loadProperties(new File(dir,"jpf.properties").getAbsolutePath());
    }
  }

  protected void appendPath (String pathKey, String key, String configPath){
    String[] paths = getStringArray(key);
    if (paths != null){
      for (String e : paths) {
        if (!e.startsWith("${") || !e.startsWith(File.separator)) {
          e = configPath + File.separatorChar + e;
        }
        append(pathKey, e, PATH_SEPARATOR);
      }
    }
  }

  protected void addJPFdirs (List<File> jpfDirs, File dir){
    while (dir != null) {
      File jpfProp = new File(dir, "jpf.properties");
      if (jpfProp.isFile()) {
        registerJPFdir(jpfDirs, dir);
        return;       
      }
      dir = getParentFile(dir);
    }
  }

  
  protected void addCurrentJPFdir(List<File> jpfDirs){
    File dir = new File(System.getProperty("user.dir"));
    while (dir != null) {
      File jpfProp = new File(dir, "jpf.properties");
      if (jpfProp.isFile()) {
        registerJPFdir(jpfDirs, dir);
        return;
      }
      dir = getParentFile(dir);
    }
  }

  protected void addJPFdirsFromClasspath(List<File> jpfDirs) {
    String cp = System.getProperty("java.class.path");
    String[] cpEntries = cp.split(File.pathSeparator);

    for (String p : cpEntries) {
      File f = new File(p);
      File dir = f.isFile() ? getParentFile(f) : f;

      addJPFdirs(jpfDirs, dir);
    }
  }

  protected void addJPFdirsFromSiteExtensions (List<File> jpfDirs){
    String[] extensions = getCompactStringArray("extensions");
    if (extensions != null){
      for (String pn : extensions){
        addJPFdirs( jpfDirs, new File(pn));
      }
    }
  }

  
  protected boolean registerJPFdir(List<File> list, File dir){
    try {
      dir = dir.getCanonicalFile();

      for (File e : list) {
        if (e.equals(dir)) {
          list.remove(e);
          list.add(e);
          return false;
        }
      }
    } catch (IOException iox) {
      throw new JPFConfigException("illegal path spec: " + dir);
    }
    
    list.add(dir);
    return true;
  }

  static File root = new File(File.separator);

  protected File getParentFile(File f){
    if (f == root){
      return null;
    } else {
      File parent = f.getParentFile();
      if (parent == null){
        parent = new File(f.getAbsolutePath());

        if (parent.getName().equals(root.getName())) {
          return root;
        } else {
          return parent;
        }
      } else {
        return parent;
      }
    }
  }


  

  protected void loadArgs (String[] cmdLineArgs) {

    for (int i=0; i<cmdLineArgs.length; i++){
      String a = cmdLineArgs[i];

      if (a != null && a.length() > 0){
        switch (a.charAt(0)){
          case '+': 
            processArg(a.substring(1));
            break;

          case '-': 
            continue;

          default:  

            int n = cmdLineArgs.length - i;
            freeArgs = new String[n];
            System.arraycopy(cmdLineArgs, i, freeArgs, 0, n);

            return;
        }
      }
    }
  }


  
  protected void processArg (String a) {

    int idx = a.indexOf("=");

    if (idx == 0){
      throw new JPFConfigException("illegal option: " + a);
    }

    if (idx > 0) {
      String key = a.substring(0, idx).trim();
      String val = a.substring(idx + 1).trim();

      if (val.length() == 0){
        val = null;
      }

      setProperty(key, val);

    } else {
      setProperty(a.trim(), "true");
    }

  }


  
  protected String normalize (String v) {
    if (v == null){
      return null; 
    }


    v = v.trim();
    

    if ("true".equalsIgnoreCase(v)
        || "yes".equalsIgnoreCase(v)
        || "on".equalsIgnoreCase(v)) {
      v = TRUE;
    } else if ("false".equalsIgnoreCase(v)
        || "no".equalsIgnoreCase(v)
        || "off".equalsIgnoreCase(v)) {
      v = FALSE;
    }


    if ("nil".equalsIgnoreCase(v) || "null".equalsIgnoreCase(v)){
      v = null;
    }
    
    return v;
  }

  


  protected String expandString (String key, String s) {
    int i, j = 0;
    if (s == null || s.length() == 0) {
      return s;
    }

    while ((i = s.indexOf("${", j)) >= 0) {
      if ((j = s.indexOf('}', i)) > 0) {
        String k = s.substring(i + 2, j);
        String v;
        
        if ((key != null) && key.equals(k)) {

          v = getProperty(key);
        } else {


          v = getProperty(k);
        }
        
        if (v == null) { 
          v = System.getProperty(k);
        }
        
        if (v != null) {
          s = s.substring(0, i) + v + s.substring(j + 1, s.length());
          j = i + v.length();
        } else {
          s = s.substring(0, i) + s.substring(j + 1, s.length());
          j = i;
        }
      }
    }

    return s;    
  }


  boolean loadPropertiesRecursive (String fileName){

    String curConfig = (String)get("config");
    String curConfigPath = (String)get("config_path");

    File propFile = new File(fileName);
    if (!propFile.isAbsolute()){
      propFile = new File(curConfigPath, fileName);
    }
    String absPath = propFile.getAbsolutePath();

    if (!propFile.isFile()){
      throw exception("property file does not exist: " + absPath);
    }

    boolean ret = loadProperties(absPath);


    super.put("config", curConfig);
    super.put("config_path", curConfigPath);

    return ret;
  }

  void includePropertyFile(String key, String value){
    value = expandString(key, value);
    if (value != null && value.length() > 0){
      loadPropertiesRecursive(value);
    } else {
      throw exception("@include pathname argument missing");
    }
  }

  void includeCondPropertyFile(String key, String value, boolean keyPresent){
    value = expandString(key, value);
    if (value != null && value.length() > 0){

      if (value.charAt(0) == '?'){
        int idx = value.indexOf('?', 1);
        if (idx > 1){
          String k = value.substring(1, idx);
          if (containsKey(k) == keyPresent){
            String v = value.substring(idx+1);
            if (v.length() > 0){
              loadPropertiesRecursive(v);
            } else {
              throw exception("@include_unless pathname argument missing (?<key>?<pathName>)");
            }
          }

        } else {
          throw exception("malformed @include_unless argument (?<key>?<pathName>), found: " + value);
        }
      } else {
        throw exception("malformed @include_unless argument (?<key>?<pathName>), found: " + value);
      }
    } else {
      throw exception("@include_unless missing ?<key>?<pathName> argument");
    }
  }


  void includeProjectPropertyFile (String projectId){
    String projectPath = getString(projectId);
    if (projectPath != null){
      File projectProps = new File(projectPath, "jpf.properties");
      if (projectProps.isFile()){
        loadPropertiesRecursive(projectProps.getAbsolutePath());

      } else {
        throw exception("project properties not found: " + projectProps.getAbsolutePath());
      }

    } else {
      throw exception("unknown project id (check site.properties): " + projectId);
    }
  }



  @Override
  public Object put (Object keyObject, Object valueObject){

    if (keyObject == null){
      throw exception("no null keys allowed");
    } else if (!(keyObject instanceof String)){
      throw exception("only String keys allowed, got: " + keyObject);
    }
    if (valueObject != null && !(valueObject instanceof String)){
      throw exception("only String or null values allowed, got: " + valueObject);
    }

    String key = (String)keyObject;
    String value = (String)valueObject;

    if (key.length() == 0){
      throw exception("no empty keys allowed");
    }

    if (key.charAt(0) == KEY_PREFIX){
      processPseudoProperty( key, value);
      return null; 

    } else {

      String k = expandString(null, key);

      if (!(value == null)) { 
        String v = value;

        if (k.charAt(k.length() - 1) == '+') { 
          k = k.substring(0, k.length() - 1);
          return append(k, v, null);

        } else if (k.charAt(0) == '+') { 
          k = k.substring(1);
          return prepend(k, v, null);

        } else { 
          v = normalize(expandString(k, v));
          if (v != null){
            return setKey(k, v);
          } else {
            return removeKey(k);
          }
        }

      } else { 
        return removeKey(k);
      }
    }
  }
  
  protected void processPseudoProperty( String key, String value){
    if (REQUIRES_KEY.equals(key)) {


      for (String reqKey : split(value)) {
        if (!containsKey(reqKey)) {
          throw new MissingRequiredKeyException(reqKey);
        }
      }

    } else if (INCLUDE_KEY.equals(key)) {
      includePropertyFile(key, value);
      
    } else if (INCLUDE_UNLESS_KEY.equals(key)) {
      includeCondPropertyFile(key, value, false);
      
    } else if (INCLUDE_IF_KEY.equals(key)) {
      includeCondPropertyFile(key, value, true);
      
    } else if (USING_KEY.equals(key)) {

      if (!haveSeenProjectProperty(value)){
        includeProjectPropertyFile(value);
      }
      
    } else {
      throw exception("unknown keyword: " + key);
    }
  }

  protected boolean haveSeenProjectProperty (String key){
    String pn = getString(key);
    if (pn == null){
      return false;
    } else {
      return sources.contains( new File( pn, "jpf.properties"));
    }
  }
  
  private Object setKey (String k, String v){
    Object oldValue = put0(k, v);
    notifyPropertyChangeListeners(k, (String) oldValue, v);
    return oldValue;
  }

  private Object removeKey (String k){
    Object oldValue = super.get(k);
    remove0(k);
    notifyPropertyChangeListeners(k, (String) oldValue, null);
    return oldValue;
  }

  private Object put0 (String k, Object v){
    entrySequence.add(k);
    return super.put(k, v);
  }

  private Object remove0 (String k){
    entrySequence.add(k);
    return super.remove(k);
  }

  public String prepend (String key, String value, String separator) {
    String oldValue = getProperty(key);
    value = normalize( expandString(key, value));

    append0(key, oldValue, value, oldValue, separator);

    return oldValue;
  }

  public String append (String key, String value, String separator) {
    String oldValue = getProperty(key);
    value = normalize( expandString(key, value));

    append0(key, oldValue, oldValue, value, separator);

    return oldValue;
  }


  private void append0 (String key, String oldValue, String a, String b, String separator){
    String newValue;

    if (a != null){
      if (b != null) {
        StringBuilder sb = new StringBuilder(a);
        if (separator != null) {
          sb.append(separator);
        }
        sb.append(b);
        newValue = sb.toString();

      } else { 
        if (oldValue == a){ 
          return; 
        } else {
          newValue = a;
        }
      }

    } else { 
      if (oldValue == b || b == null){  
        return; 
      } else {
        newValue = b;
      }
    }


    put0(key, newValue);
    notifyPropertyChangeListeners(key, oldValue, newValue);
  }

  protected String append (String key, String value) {
    return append(key, value, LIST_SEPARATOR); 
  }

  
  public String getIndexableKey (String key, int index){
    String k = key + '.' + index;
    if (containsKey(k)){
      return k;
    } else {
      if (containsKey(key)){
        return key;
      }
    }
    
    return null; 
  }

  public void setClassLoader (ClassLoader newLoader){
    loader = newLoader;
  }

  public ClassLoader getClassLoader (){
    return loader;
  }

  public boolean hasSetClassLoader (){
    return Config.class.getClassLoader() != loader;
  }

  public JPFClassLoader initClassLoader (ClassLoader parent) {
    ArrayList<String> list = new ArrayList<String>();







    collectGlobalPaths();
    if (log){
      log("collected native_classpath=" + get("native_classpath"));
      log("collected native_libraries=" + get("native_libraries"));
    }


    String[] cp = getCompactStringArray("native_classpath");
    cp = FileUtils.expandWildcards(cp);
    for (String e : cp) {
      list.add(e);
    }
    URL[] urls = FileUtils.getURLs(list);

    String[] nativeLibs = getCompactStringArray("native_libraries");

    JPFClassLoader cl;
    if (parent instanceof JPFClassLoader){ 
      cl = (JPFClassLoader)parent;
      for (URL url : urls){
        cl.addURL(url);
      }
      cl.setNativeLibs(nativeLibs);
      
    } else {    
      cl = new JPFClassLoader( urls, nativeLibs, parent);
    }
    
    loader = cl;
    return cl;
  }

  
  public void updateClassLoader (){
    if (loader != null && loader instanceof JPFClassLoader){
      JPFClassLoader jpfCl = (JPFClassLoader)loader;
            
      ArrayList<String> list = new ArrayList<String>();
      String[] cp = getCompactStringArray("native_classpath");
      cp = FileUtils.expandWildcards(cp);
      for (String e : cp) {
        URL url = FileUtils.getURL(e);
        jpfCl.addURL(url); 
      }

      String[] nativeLibs = getCompactStringArray("native_libraries");
      jpfCl.setNativeLibs(nativeLibs);
    }
  }
  




  public String[] getEntrySequence () {


    return entrySequence.toArray(new String[entrySequence.size()]);
  }

  public void addChangeListener (ConfigChangeListener l) {
    if (changeListeners == null) {
      changeListeners = new ArrayList<ConfigChangeListener>();
      changeListeners.add(l);
    } else {
      if (!changeListeners.contains(l)) {
        changeListeners.add(l);
      }
    }
  }
  
  public void removeChangeListener (ConfigChangeListener l) {
    if (changeListeners != null) {
      changeListeners.remove(l);
      
      if (changeListeners.size() == 0) {
        changeListeners = null;
      }
    }
  }
  

  public void jpfRunTerminated() {
    if (changeListeners != null) {



      ArrayList<ConfigChangeListener> list = (ArrayList<ConfigChangeListener>)changeListeners.clone();
      for (ConfigChangeListener l : list) {
        l.jpfRunTerminated(this);
      }
    }
  }
  
  public JPFException exception (String msg) {
    String context = getString("config");
    if (context != null){
      msg = "error in " + context + " : " + msg;
    }

    return new JPFConfigException(msg);
  }

  public void throwException(String msg) {
    throw new JPFConfigException(msg);
  }

  
  public String[] getFreeArgs(){
    return freeArgs;
  } 


  
  
  
  public void setTarget (String clsName) {
    put("target", clsName);
  }
  public String getTarget(){
    return getString("target");
  }
  
  public void setTargetArgs (String[] args) {
    StringBuilder sb = new StringBuilder();
    int i=0;
    for (String a : args){
      if (i++ > 0){
        sb.append(',');
      }
      sb.append(a);
    }
    put("target.args", sb.toString());
  }
  public String[] getTargetArgs(){
    String[] a = getStringArray("target.args");
    if (a == null){
      return new String[0];
    } else {
      return a;
    }
  }
  
  public void setTargetEntry (String mthName) {
    put("target.entry", mthName);
  }
  public String getTargetEntry(){
    return getString("target.entry");
  }
  
  


  public boolean getBoolean(String key) {
    String v = getProperty(key);
    return (v == TRUE);
  }

  public boolean getBoolean(String key, boolean def) {
    String v = getProperty(key);
    if (v != null) {
      return (v == TRUE);
    } else {
      return def;
    }
  }

  
  public String[] getStringEnumeration (String baseKey, int maxSize) {
    String[] arr = new String[maxSize];
    int max=-1;

    StringBuilder sb = new StringBuilder(baseKey);
    sb.append('.');
    int len = baseKey.length()+1;

    for (int i=0; i<maxSize; i++) {
      sb.setLength(len);
      sb.append(i);

      String v = getString(sb.toString());
      if (v != null) {
        arr[i] = v;
        max = i;
      }
    }

    if (max >= 0) {
      max++;
      if (max < maxSize) {
        String[] a = new String[max];
        System.arraycopy(arr,0,a,0,max);
        return a;
      } else {
        return arr;
      }
    } else {
      return null;
    }
  }

  public String[] getKeysStartingWith (String prefix){
    ArrayList<String> list = new ArrayList<String>();

    for (Enumeration e = keys(); e.hasMoreElements(); ){
      String k = e.nextElement().toString();
      if (k.startsWith(prefix)){
        list.add(k);
      }
    }

    return list.toArray(new String[list.size()]);
  }

  public String[] getKeyComponents (String key){
    return key.split("\\.");
  }

  public int[] getIntArray (String key) throws JPFConfigException {
    String v = getProperty(key);

    if (v != null) {
      String[] sa = split(v);
      int[] a = new int[sa.length];
      int i = 0;
      try {
        for (; i<sa.length; i++) {
          String s = sa[i];
          int val;
          if (s.startsWith("0x")){
            val = Integer.parseInt(s.substring(2),16); 
          } else {
            val = Integer.parseInt(s);
          }
          a[i] = val;
        }
        return a;
      } catch (NumberFormatException nfx) {
        throw new JPFConfigException("illegal int[] element in '" + key + "' = \"" + sa[i] + '"');
      }
    } else {
      return null;
    }
  }
  public int[] getIntArray (String key, int... defaultValues){
    int[] val = getIntArray(key);
    if (val == null){
      return defaultValues;
    } else {
      return val;
    }
  }

  public long getDuration (String key, long defValue) {
    String v = getProperty(key);
    if (v != null) {
      long d = 0;

      if (v.indexOf(':') > 0){
        String[] a = v.split(":");
        if (a.length > 3){

          return defValue;
        }
        int m = 1000;
        for (int i=a.length-1; i>=0; i--, m*=60){
          try {
            int n = Integer.parseInt(a[i]);
            d += m*n;
          } catch (NumberFormatException nfx) {
            throw new JPFConfigException("illegal duration element in '" + key + "' = \"" + v + '"');
          }
        }

      } else {
        try {
          d = Long.parseLong(v);
        } catch (NumberFormatException nfx) {
          throw new JPFConfigException("illegal duration element in '" + key + "' = \"" + v + '"');
        }
      }

      return d;
    }

    return defValue;
  }

  public int getInt(String key) {
    return getInt(key, 0);
  }

  public int getInt(String key, int defValue) {
    String v = getProperty(key);
    if (v != null) {
      if (MAX.equals(v)){
        return Integer.MAX_VALUE;
      } else {
        try {
          return Integer.parseInt(v);
        } catch (NumberFormatException nfx) {
          throw new JPFConfigException("illegal int element in '" + key + "' = \"" + v + '"');
        }
      }
    }

    return defValue;
  }

  public long getLong(String key) {
    return getLong(key, 0L);
  }

  public long getLong(String key, long defValue) {
    String v = getProperty(key);
    if (v != null) {
      if (MAX.equals(v)){
        return Long.MAX_VALUE;
      } else {
        try {
          return Long.parseLong(v);
        } catch (NumberFormatException nfx) {
          throw new JPFConfigException("illegal long element in '" + key + "' = \"" + v + '"');
        }
      }
    }

    return defValue;
  }

  public long[] getLongArray (String key) throws JPFConfigException {
    String v = getProperty(key);

    if (v != null) {
      String[] sa = split(v);
      long[] a = new long[sa.length];
      int i = 0;
      try {
        for (; i<sa.length; i++) {
          a[i] = Long.parseLong(sa[i]);
        }
        return a;
      } catch (NumberFormatException nfx) {
        throw new JPFConfigException("illegal long[] element in " + key + " = " + sa[i]);
      }
    } else {
      return null;
    }
  }

  public long[] getLongArray (String key, long... defaultValues){
    long[] val = getLongArray(key);
    if (val != null){
      return val;
    } else {
      return defaultValues;
    } 
  }

  public float getFloat (String key) {
    return getFloat(key, 0.0f);
  }

  public float getFloat (String key, float defValue) {
    String v = getProperty(key);
    if (v != null) {
      try {
        return Float.parseFloat(v);
      } catch (NumberFormatException nfx) {
        throw new JPFConfigException("illegal float element in '" + key + "' = \"" + v + '"');
      }
    }

    return defValue;
  }
  
  public float[] getFloatArray (String key) throws JPFConfigException {
    String v = getProperty(key);

    if (v != null) {
      String[] sa = split(v);
      float[] a = new float[sa.length];
      int i = 0;
      try {
        for (; i<sa.length; i++) {
          a[i] = Float.parseFloat(sa[i]);
        }
        return a;
      } catch (NumberFormatException nfx) {
        throw new JPFConfigException("illegal float[] element in " + key + " = " + sa[i]);
      }
    } else {
      return null;
    }
  }
  public float[] getFloatArray (String key, float... defaultValues){
    float[] v = getFloatArray( key);
    if (v != null){
      return v;
    } else {
      return defaultValues;
    }
  }
  
  
  public double getDouble (String key) {
    return getDouble(key, 0.0);
  }

  public double getDouble (String key, double defValue) {
    String v = getProperty(key);
    if (v != null) {
      try {
        return Double.parseDouble(v);
      } catch (NumberFormatException nfx) {
        throw new JPFConfigException("illegal double element in '" + key + "' = \"" + v + '"');
      }
    }

    return defValue;
  }

  public double[] getDoubleArray (String key) throws JPFConfigException {
    String v = getProperty(key);

    if (v != null) {
      String[] sa = split(v);
      double[] a = new double[sa.length];
      int i = 0;
      try {
        for (; i<sa.length; i++) {
          a[i] = Double.parseDouble(sa[i]);
        }
        return a;
      } catch (NumberFormatException nfx) {
        throw new JPFConfigException("illegal double[] element in " + key + " = " + sa[i]);
      }
    } else {
      return null;
    }
  }
  public double[] getDoubleArray (String key, double... defaultValues){
    double[] v = getDoubleArray( key);
    if (v != null){
      return v;
    } else {
      return defaultValues;
    }
  }

  public <T extends Enum<T>> T getEnum( String key, T[] values, T defValue){
    String v = getProperty(key);

    if (v != null){
      for (T t : values){
        if (v.equalsIgnoreCase(t.name())){
          return t;
        }
      }
      
      throw new JPFConfigException("unknown enum value for " + key + " = " + v);
      
    } else {
      return defValue;
    }
  }

  public String getString(String key) {
    return getProperty(key);
  }

  public String getString(String key, String defValue) {
    String s = getProperty(key);
    if (s != null) {
      return s;
    } else {
      return defValue;
    }
  }

  
  public long getMemorySize(String key, long defValue) {
    String v = getProperty(key);
    long sz = defValue;

    if (v != null) {
      int n = v.length() - 1;
      try {
        char c = v.charAt(n);

        if ((c == 'M') || (c == 'm')) {
          sz = Long.parseLong(v.substring(0, n)) << 20;
        } else if ((c == 'K') || (c == 'k')) {
          sz = Long.parseLong(v.substring(0, n)) << 10;
        } else {
          sz = Long.parseLong(v);
        }

      } catch (NumberFormatException nfx) {
        throw new JPFConfigException("illegal memory size element in '" + key + "' = \"" + v + '"');
      }
    }

    return sz;
  }

  public HashSet<String> getStringSet(String key){
    String v = getProperty(key);
    if (v != null && (v.length() > 0)) {
      HashSet<String> hs = new HashSet<String>();
      for (String s : split(v)) {
        hs.add(s);
      }
      return hs;
    }

    return null;
    
  }
  
  public HashSet<String> getNonEmptyStringSet(String key){
    HashSet<String> hs = getStringSet(key);
    if (hs != null && hs.isEmpty()) {
      return null;
    } else {
      return hs;
    }
  }
    
  public String[] getStringArray(String key) {
    String v = getProperty(key);
    if (v != null && (v.length() > 0)) {
      return split(v);
    }

    return null;
  }

  public String[] getStringArray(String key, char[] delims) {
    String v = getProperty(key);
    if (v != null && (v.length() > 0)) {
      return split(v,delims);
    }

    return null;
  }

  public String[] getCompactTrimmedStringArray (String key){
    String[] a = getStringArray(key);

    if (a != null) {
      for (int i = 0; i < a.length; i++) {
        String s = a[i];
        if (s != null && s.length() > 0) {
          a[i] = s.trim();
        }
      }

      return removeEmptyStrings(a);

    } else {
      return EMPTY_STRING_ARRAY;
    }
  }

  public String[] getCompactStringArray(String key){
    return removeEmptyStrings(getStringArray(key));
  }

  
  public String[] getStringArray(String key, String[] def){
    String v = getProperty(key);
    if (v != null && (v.length() > 0)) {
      return split(v);
    } else {
      return def;
    }
  }

  public static String[] removeEmptyStrings (String[] a){
    if (a != null) {
      int n = 0;
      for (int i=0; i<a.length; i++){
        if (a[i].length() > 0){
          n++;
        }
      }

      if (n < a.length){ 
        String[] r = new String[n];
        for (int i=0, j=0; i<a.length; i++){
          if (a[i].length() > 0){
            r[j++] = a[i];
            if (j == n){
              break;
            }
          }
        }
        return r;

      } else {
        return a;
      }
    }

    return null;
  }


  
  String getIdPart (String key) {
    String v = getProperty(key);
    if ((v != null) && (v.length() > 0)) {
      int i = v.indexOf('@');
      if (i >= 0){
        return v.substring(i+1);
      }
    }

    return null;
  }

  public Class<?> asClass (String v) throws JPFConfigException {
    if ((v != null) && (v.length() > 0)) {
      v = stripId(v);
      v = expandClassName(v);
      try {
        return loader.loadClass(v);
      } catch (ClassNotFoundException cfx) {
        throw new JPFConfigException("class not found " + v + " by classloader: " + loader);
      } catch (ExceptionInInitializerError ix) {
        throw new JPFConfigException("class initialization of " + v + " failed: " + ix,
            ix);
      }
    }

    return null;    
  }
      
  public <T> Class<? extends T> getClass(String key, Class<T> type) throws JPFConfigException {
    Class<?> cls = asClass( getProperty(key));
    if (cls != null) {
      if (type.isAssignableFrom(cls)) {
        return cls.asSubclass(type);
      } else {
        throw new JPFConfigException("classname entry for: \"" + key + "\" not of type: " + type.getName());
      }
    }
    return null;
  }
  
    
  public Class<?> getClass(String key) throws JPFConfigException {
    return asClass( getProperty(key));
  }
  
  public Class<?> getEssentialClass(String key) throws JPFConfigException {
    Class<?> cls = getClass(key);
    if (cls == null) {
      throw new JPFConfigException("no classname entry for: \"" + key + "\"");
    }

    return cls;
  }
  
  String stripId (String v) {
    int i = v.indexOf('@');
    if (i >= 0) {
      return v.substring(0,i);
    } else {
      return v;
    }
  }

  String getId (String v){
    int i = v.indexOf('@');
    if (i >= 0) {
      return v.substring(i+1);
    } else {
      return null;
    }
  }

  String expandClassName (String clsName) {
    if (clsName != null && clsName.length() > 0 && clsName.charAt(0) == '.') {
      return "gov.nasa.jpf" + clsName;
    } else {
      return clsName;
    }
  }

  
  public Class<?>[] getClasses(String key) throws JPFConfigException {
    String[] v = getStringArray(key);
    if (v != null) {
      int n = v.length;
      Class<?>[] a = new Class[n];
      for (int i = 0; i < n; i++) {
        String clsName = expandClassName(v[i]);
        if (clsName != null && clsName.length() > 0){
          try {
            clsName = stripId(clsName);
            a[i] = loader.loadClass(clsName);
          } catch (ClassNotFoundException cnfx) {
            throw new JPFConfigException("class not found " + v[i]);
          } catch (ExceptionInInitializerError ix) {
            throw new JPFConfigException("class initialization of " + v[i] + " failed: " + ix, ix);
          }
        }
      }

      return a;
    }

    return null;
  }
  
  
  public <T> T[] getGroupInstances (String keyPrefix, String keyPostfix, Class<T> type, 
          String... defaultClsNames) throws JPFConfigException {
    
    String[] ids = getCompactTrimmedStringArray(keyPrefix);
    
    if (ids.length > 0){
      keyPrefix = keyPrefix + '.';
      T[] arr = (T[]) Array.newInstance(type, ids.length);
      
      for(int i = 0; i < ids.length; i++){
        String key = keyPrefix + ids[i];
        if (keyPostfix != null){
          key = key + keyPostfix;
        }
        arr[i] = getEssentialInstance(key, type);
      }
      
      return arr;
      
    } else {
      T[] arr = (T[]) Array.newInstance(type, defaultClsNames.length);
              
      for (int i=0; i<arr.length; i++){
        arr[i] = getInstance((String)null, defaultClsNames[i], type);
        if (arr[i] == null){
          exception("cannot instantiate default type " + defaultClsNames[i]);
        }
      }
      
      return arr;
    }
  }
  

  String[] getIds (String key) {
    String v = getProperty(key);

    if (v != null) {
      int i = v.indexOf('@');
      if (i >= 0) { 
        String[] a = split(v);
        String[] ids = new String[a.length];
        for (i = 0; i<a.length; i++) {
          ids[i] = getId(a[i]);
        }
        return ids;
      }
    }

    return null;
  }

  public <T> ArrayList<T> getInstances(String key, Class<T> type) throws JPFConfigException {

    Class<?>[] argTypes = { Config.class };
    Object[] args = { this };

    return getInstances(key,type,argTypes,args);
  }
  
  public <T> ArrayList<T> getInstances(String key, Class<T> type, Class<?>[]argTypes, Object[] args)
                                                      throws JPFConfigException {
    Class<?>[] c = getClasses(key);

    if (c != null) {
      String[] ids = getIds(key);

      ArrayList<T> a = new ArrayList<T>(c.length);

      for (int i = 0; i < c.length; i++) {
        String id = (ids != null) ? ids[i] : null;
        T listener = getInstance(key, c[i], type, argTypes, args, id);
        if (listener != null) {
          a.add( listener);
        } else {

        }
      }

      return a;
      
    } else {

    }

    return null;
  }
  
  public <T> T getInstance(String key, Class<T> type, String defClsName) throws JPFConfigException {
    Class<?>[] argTypes = CONFIG_ARGTYPES;
    Object[] args = CONFIG_ARGS;

    Class<?> cls = getClass(key);
    String id = getIdPart(key);

    if (cls == null) {
      try {
        cls = loader.loadClass(defClsName);
      } catch (ClassNotFoundException cfx) {
        throw new JPFConfigException("class not found " + defClsName);
      } catch (ExceptionInInitializerError ix) {
        throw new JPFConfigException("class initialization of " + defClsName + " failed: " + ix, ix);
      }
    }
    
    return getInstance(key, cls, type, argTypes, args, id);
  }

  public <T> T getInstance(String key, Class<T> type) throws JPFConfigException {
    Class<?>[] argTypes = CONFIG_ARGTYPES;
    Object[] args = CONFIG_ARGS;

    return getInstance(key, type, argTypes, args);
  }
    
  public <T> T getInstance(String key, Class<T> type, Class<?>[] argTypes,
                            Object[] args) throws JPFConfigException {
    Class<?> cls = getClass(key);
    String id = getIdPart(key);

    if (cls != null) {
      return getInstance(key, cls, type, argTypes, args, id);
    } else {
      return null;
    }
  }
  
  public <T> T getInstance(String key, Class<T> type, Object arg1, Object arg2)  throws JPFConfigException {
    Class<?>[] argTypes = new Class<?>[2];
    argTypes[0] = arg1.getClass();
    argTypes[1] = arg2.getClass();

    Object[] args = new Object[2];
    args[0] = arg1;
    args[1] = arg2;

    return getInstance(key, type, argTypes, args);
  }


  public <T> T getEssentialInstance(String key, Class<T> type) throws JPFConfigException {
    Class<?>[] argTypes = { Config.class };
    Object[] args = { this };
    return getEssentialInstance(key, type, argTypes, args);
  }

  
  public <T> T getEssentialInstance(String key, Class<T> type, Object arg1, Object arg2)  throws JPFConfigException {
    Class<?>[] argTypes = new Class<?>[2];
    argTypes[0] = arg1.getClass();
    argTypes[1] = arg2.getClass();

    Object[] args = new Object[2];
    args[0] = arg1;
    args[1] = arg2;

    return getEssentialInstance(key, type, argTypes, args);
  }

  public <T> T getEssentialInstance(String key, Class<T> type, Class<?>[] argTypes, Object[] args) throws JPFConfigException {
    Class<?> cls = getEssentialClass(key);
    String id = getIdPart(key);

    return getInstance(key, cls, type, argTypes, args, id);
  }

  public <T> T getInstance (String id, String clsName, Class<T> type, Class<?>[] argTypes, Object[] args) throws JPFConfigException {
    Class<?> cls = asClass(clsName);
    
    if (cls != null) {
      return getInstance(id, cls, type, argTypes, args, id);
    } else {
      return null;
    }
  }
  
  public <T> T getInstance (String id, String clsName, Class<T> type) throws JPFConfigException {
    Class<?>[] argTypes = CONFIG_ARGTYPES;
    Object[] args = CONFIG_ARGS;

    Class<?> cls = asClass(clsName);
    
    if (cls != null) {
      return getInstance(id, cls, type, argTypes, args, id);
    } else {
      return null;
    }
  }
  
  
  <T> T getInstance(String key, Class<?> cls, Class<T> type, Class<?>[] argTypes,
                     Object[] args, String id) throws JPFConfigException {
    Object o = null;
    Constructor<?> ctor = null;

    if (cls == null) {
      return null;
    }

    if (id != null) { 
      if (singletons == null) {
        singletons = new HashMap<String,Object>();
      } else {
        o = type.cast(singletons.get(id));
      }
    }

    while (o == null) {
      try {
        ctor = cls.getConstructor(argTypes);
        o = ctor.newInstance(args);
      } catch (NoSuchMethodException nmx) {
         
        if ((argTypes.length > 1) || ((argTypes.length == 1) && (argTypes[0] != Config.class))) {

          argTypes = CONFIG_ARGTYPES;
          args = CONFIG_ARGS;

        } else if (argTypes.length > 0) {

          argTypes = NO_ARGTYPES;
          args = NO_ARGS;

        } else {

          throw new JPFConfigException(key, cls, "no suitable ctor found");
        }
      } catch (IllegalAccessException iacc) {
        throw new JPFConfigException(key, cls, "\n> ctor not accessible: "
            + getMethodSignature(ctor));
      } catch (IllegalArgumentException iarg) {
        throw new JPFConfigException(key, cls, "\n> illegal constructor arguments: "
            + getMethodSignature(ctor));
      } catch (InvocationTargetException ix) {
        Throwable tx = ix.getTargetException();
        if (tx instanceof JPFConfigException) {
          throw new JPFConfigException(tx.getMessage() + "\n> used within \"" + key
              + "\" instantiation of " + cls);
        } else {
          throw new JPFConfigException(key, cls, "\n> exception in "
              + getMethodSignature(ctor) + ":\n>> " + tx, tx);
        }
      } catch (InstantiationException ivt) {
        throw new JPFConfigException(key, cls,
            "\n> abstract class cannot be instantiated");
      } catch (ExceptionInInitializerError eie) {
        throw new JPFConfigException(key, cls, "\n> static initialization failed:\n>> "
            + eie.getException(), eie.getException());
      }
    }


    if (!type.isInstance(o)) {
      throw new JPFConfigException(key, cls, "\n> instance not of type: "
          + type.getName());
    }

    if (id != null) { 
      singletons.put(id, o);
    }

    return type.cast(o); 
  }

  public String getMethodSignature(Constructor<?> ctor) {
    StringBuilder sb = new StringBuilder(ctor.getName());
    sb.append('(');
    Class<?>[] argTypes = ctor.getParameterTypes();
    for (int i = 0; i < argTypes.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(argTypes[i].getName());
    }
    sb.append(')');
    return sb.toString();
  }

  public boolean hasValue(String key) {
    String v = getProperty(key);
    return ((v != null) && (v.length() > 0));
  }

  public boolean hasValueIgnoreCase(String key, String value) {
    String v = getProperty(key);
    if (v != null) {
      return v.equalsIgnoreCase(value);
    }

    return false;
  }

  public int getChoiceIndexIgnoreCase(String key, String[] choices) {
    String v = getProperty(key);

    if ((v != null) && (choices != null)) {
      for (int i = 0; i < choices.length; i++) {
        if (v.equalsIgnoreCase(choices[i])) {
          return i;
        }
      }
    }

    return -1;
  }

  public URL getURL (String key){
    String v = getProperty(key);
    if (v != null) {
      try {
        return FileUtils.getURL(v);
      } catch (Throwable x){
        throw exception("malformed URL: " + v);
      }
    } else {
      return null;
    }
  }

  public File[] getPathArray (String key) {    
    String v = getProperty(key);
    if (v != null) {
      String[] pe = removeEmptyStrings( pathSplit(v));
      
      if (pe != null && pe.length > 0) {
        File[] files = new File[pe.length];
        for (int i=0; i<files.length; i++) {
          String path = FileUtils.asPlatformPath(pe[i]);
          files[i] = new File(path);
        }
        return files;
      }      
    }

    return new File[0];
  }

  public File getPath (String key) {
    String v = getProperty(key);
    if (v != null) {
      return new File(FileUtils.asPlatformPath(v));
    }
    
    return null;
  }

  static final char[] UNIX_PATH_SEPARATORS = {',', ';', ':' };
  static final char[] WINDOWS_PATH_SEPARATORS = {',', ';' };

  protected String[] pathSplit (String input){
    if (File.pathSeparatorChar == ':'){
      return split( input, UNIX_PATH_SEPARATORS);
    } else {
      return split( input, WINDOWS_PATH_SEPARATORS);
    }
  }

  static final char[] DELIMS = { ',', ';' };

  
  protected String[] split (String input){
    return split(input, DELIMS);
  }

  private boolean isDelim(char[] delim, char c){
    for (int i=0; i<delim.length; i++){
      if (c == delim[i]){
        return true;
      }
    }
    return false;
  }

  protected String[] split (String input, char[] delim){
    int n = input.length();
    ArrayList<String> elements = new ArrayList<String>();
    boolean quote = false;

    char[] buf = new char[128];
    int k=0;

    for (int i=0; i<n; i++){
      char c = input.charAt(i);

      if (!quote) {
        if (isDelim(delim,c)){ 
          elements.add( new String(buf, 0, k));
          k = 0;
          continue;
        } else if (c=='`') {
          quote = true;
          continue;
        }
      }

      if (k >= buf.length){
        char[] newBuf = new char[buf.length+128];
        System.arraycopy(buf, 0, newBuf, 0, k);
        buf = newBuf;
      }
      buf[k++] = c;
      quote = false;
    }

    if (k>0){
      elements.add( new String(buf, 0, k));
    }

    return elements.toArray(new String[elements.size()]);
  }

  static final String UNINITIALIZED = "uninitialized";

  String initialNativeClasspath = UNINITIALIZED, 
          initialClasspath = UNINITIALIZED, 
          initialSourcepath = UNINITIALIZED, 
          initialPeerPackages = UNINITIALIZED,
          initialNativeLibraries = UNINITIALIZED;
  

  
  public void resetGlobalPaths() {
    if (initialNativeClasspath == UNINITIALIZED){
      initialNativeClasspath = getString("native_classpath");
    } else {
      put0( "native_classpath", initialNativeClasspath);
    }

    if (initialClasspath == UNINITIALIZED){
      initialClasspath = getString("classpath");
    } else {
      put0( "classpath", initialClasspath);
    }
    
    if (initialSourcepath == UNINITIALIZED){
      initialSourcepath = getString("sourcepath");
    } else {
      put0( "sourcepath", initialSourcepath);
    }

    if (initialPeerPackages == UNINITIALIZED){
      initialPeerPackages = getString("peer_packages");
    } else {
      put0( "peer_packages", initialPeerPackages);
    }

    if (initialNativeLibraries == UNINITIALIZED){
      initialNativeLibraries = getString("native_libraries");
    } else {
      put0( "native_libraries", initialNativeLibraries);
    }
  }

  
  public void collectGlobalPaths() {
        


    String[] keys = getEntrySequence();

    String nativeLibKey = "." + System.getProperty("os.name") +
            '.' + System.getProperty("os.arch") + ".native_libraries";

    for (int i = keys.length-1; i>=0; i--){
      String k = keys[i];
      if (k.endsWith(".native_classpath")){
        appendPath("native_classpath", k);
        
      } else if (k.endsWith(".classpath")){
        appendPath("classpath", k);
        
      } else if (k.endsWith(".sourcepath")){        
        appendPath("sourcepath", k);
        
      } else if (k.endsWith("peer_packages")){
        append("peer_packages", getString(k), ",");
        
      } else if (k.endsWith(nativeLibKey)){
        appendPath("native_libraries", k);
      }
    }
  }

  
  static Pattern absPath = Pattern.compile("(?:[a-zA-Z]:)?[/\\\\].*");

  void appendPath (String pathKey, String key){
    String projName = key.substring(0, key.indexOf('.'));
    String pathPrefix = null;

    if (projName.isEmpty()){
      pathPrefix = new File(".").getAbsolutePath();
    } else {
      pathPrefix = getString(projName);
    }

    if (pathPrefix != null){
      pathPrefix += '/';

      String[] elements = getCompactStringArray(key);
      if (elements != null){
        for (String e : elements) {
          if (e != null && e.length()>0){



            if (!(absPath.matcher(e).matches()) && !e.startsWith(pathPrefix)) {
              e = pathPrefix + e;
            }

            append(pathKey, e);
          }
        }
      }

    } else {

    }
  }




  
  public void promotePropertyCategory (String keyPrefix){
    int prefixLen = keyPrefix.length();
    

    ArrayList<Map.Entry<Object,Object>> promoted = null;
    
    for (Map.Entry<Object,Object> e : entrySet()){
      Object k = e.getKey();
      if (k instanceof String){
        String key = (String)k;
        if (key.startsWith(keyPrefix)){
          Object v = e.getValue();
          if (! IGNORE_VALUE.equals(v)){
            if (promoted == null){
              promoted = new ArrayList<Map.Entry<Object,Object>>();
            }
            promoted.add(e);
          }
        }
      }
    }
    
    if (promoted != null){
      for (Map.Entry<Object, Object> e : promoted) {
        String key = (String) e.getKey();
        key = key.substring(prefixLen);

        put(key, e.getValue());
      }
    }
  }

  
  @Override
  public Object setProperty (String key, String newValue) {    
    Object oldValue = put(key, newValue);    
    notifyPropertyChangeListeners(key, (String)oldValue, newValue);
    return oldValue;
  }

  public void parse (String s) {
    
    int i = s.indexOf("=");
    if (i > 0) {
      String key, val;
      
      if (i > 1 && s.charAt(i-1)=='+') { 
        key = s.substring(0, i-1).trim();
        val = s.substring(i+1); 
        append(key, val);
        
      } else { 
        key = s.substring(0, i).trim();
        val = s.substring(i+1);
        setProperty(key, val);
      }
      
    }
  }
  
  protected void notifyPropertyChangeListeners (String key, String oldValue, String newValue) {
    if (changeListeners != null) {
      for (ConfigChangeListener l : changeListeners) {
        l.propertyChanged(this, key, oldValue, newValue);
      }
    }    
  }
  
  public String[] asStringArray (String s){
    return split(s);
  }
  
  public TreeMap<Object,Object> asOrderedMap() {
    TreeMap<Object,Object> map = new TreeMap<Object,Object>();
    map.putAll(this);
    return map;
  }
  


  public void print (PrintWriter pw) {
    pw.println("----------- Config contents");


    TreeSet<String> kset = new TreeSet<String>();
    for (Enumeration<?> e = propertyNames(); e.hasMoreElements();) {
      Object k = e.nextElement();
      if (k instanceof String) {
        kset.add( (String)k);
      }
    }

    for (String key : kset) {
      String val = getProperty(key);
      pw.print(key);
      pw.print(" = ");
      pw.println(val);
    }

    pw.flush();
  }

  public void printSources (PrintWriter pw) {
    pw.println("----------- Config sources");
    for (Object src : sources){
      pw.println(src);
    }    
  }
  
  public void printEntries() {
    PrintWriter pw = new PrintWriter(System.out);
    print(pw);
  }

  public String getSourceName (Object src){
    if (src instanceof File){
      return ((File)src).getAbsolutePath();
    } else if (src instanceof URL){
      return ((URL)src).toString();
    } else {
      return src.toString();
    }
  }
  
  public List<Object> getSources() {
    return sources;
  }
  
  public void printStatus(Logger log) {
    int idx = 0;
    
    for (Object src : sources){
      if (src instanceof File){
        log.config("configuration source " + idx++ + " : " + getSourceName(src));
      }
    }
  }


}
