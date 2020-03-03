/* 
 * Copyright 2019 Georgia Institute of Technology
 * All rights reserved.
 *
 * Author(s): Xiangyu Li <xiangyu.li@cc.gatech.edu>
 *
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package anonymous.domain.enlighten.subjectmodel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import anonymous.domain.enlighten.ExtProperties;
import anonymous.domain.enlighten.data.SourceLocation;
import anonymous.domain.enlighten.files.LookupFilesByNameVisitor;

public class SubjectProgram {
  
  private Path rootDir;
  private Path dataDir;
  private Info programInfo;
  
  private URLClassLoader subjectProgramClassesLoader;
  
  private Set<Class<?>> testClasses;
  private Map<String, Path> appClassNamePathMap;
  
  
  public static SubjectProgram openSubjectProgram(Path rootDir, Path dataDir) throws IOException {
    return new SubjectProgram(rootDir, dataDir);
  }
  
  protected SubjectProgram(Path rootDir, Path dataDir) throws IOException {
    this.rootDir = rootDir;
    this.dataDir = dataDir;
    programInfo = Info.readSubjectProjectInfo(getProjectInfoFilePath().toFile());
    initializeClassLoader();
  }
  
  public Info getProgramInfo() {
    return programInfo;
  }

  public Path getRootDir() {
    return rootDir;
  }
  
  public List<Path> getAppSourceDirs() {
    return resolvePathStrListAgainstRoot(programInfo.getAppSourcePaths());
  }
  
  public List<Path> getTestSourceDirs() {
    return resolvePathStrListAgainstRoot(programInfo.getTestSourcePaths());
  }
  
  public List<Path> getLibPaths() {
    return resolvePathStrListAgainstRoot(programInfo.getLibPaths());
  }
  
  public String getAppPackage() {
    return programInfo.getAppPackage();
  }
  
  public Path getProjectInfoFilePath() {
    return getRootDir().resolve("subject_program.info");
  }
  
  public String getFaultyClassName() {
    return programInfo.getProperty("faulty_class");
  }
  
  public String getFaultyMethodSignature() {
    return programInfo.getProperty("faulty_method");
  }
  
  public List<SourceLocation> getFaultySourceLocations() {
    List<SourceLocation> faultySourceLocations = new ArrayList<>();
    List<String> faultySourceLineDescs = programInfo.getPropertyValueList("faulty_lines");
    if (faultySourceLineDescs != null) {
      for (String faultySourceLineDesc : faultySourceLineDescs) {
        faultySourceLocations.add(SourceLocation.get(faultySourceLineDesc));
      }
    }
    return faultySourceLocations;
  }
  
  public Path getDataDirRoot() {
    return dataDir;
  }
  
  public Path getCoverageDir() {
    return getDataDirRoot().resolve("coverage");
  }
  
  public Path getExecutionProfileDir() {
    return getDataDirRoot().resolve("exec_profile");
  }
  
  public Class<?> loadClass(String className) throws ClassNotFoundException {
    return subjectProgramClassesLoader.loadClass(className);
  }

  
  public Set<Class<?>> listTestClasses() throws IOException {
    if (testClasses != null) {
      return testClasses;
    }
    testClasses = new HashSet<>();
    

    List<String> testClassNames = programInfo.getTestClassNames();
    if (testClassNames != null && !testClassNames.isEmpty()) {
      for (String testClassName : testClassNames) {
        Class<?> classObj = null;
        try {
          classObj = loadClass(testClassName);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("Fatal: Could not load class " + testClassName);
        }
        testClasses.add(classObj);
      }
      return testClasses;
    }
    


    int classFileNumTracking = 0;
    for (Path testSourceDir : getTestSourceDirs()) {
      LookupFilesByNameVisitor lookupClassFiles = new LookupFilesByNameVisitor(".*\\.class");
      Files.walkFileTree(testSourceDir, lookupClassFiles);
      List<Path> classFilePaths = lookupClassFiles.getMatchedFilesPaths();
      classFileNumTracking += classFilePaths.size();
      for (Path classFilePath : classFilePaths) {
        String relativePath = testSourceDir.relativize(classFilePath).toString();
        String className = relativePath.substring(0, relativePath.length() - 6)
            .replaceAll(Pattern.quote(File.separator), ".");
        Class<?> classObj = null;
        try {
          classObj = loadClass(className);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("Internal error: Could not load class " + className);
        }
        JUnitTestClassModel testModel = JUnitTestClassModel.getJUnitTestClassModel(classObj);



        if (testModel != null && testModel.getTestMethods().size() != 0) {
          testClasses.add(classObj);
        }
      }
    }
    if (classFileNumTracking == 0) {
      System.err.println("Warning: No class files found in the test directory "
          + "of the subject program " + rootDir.toString());
      System.err.println("Forgot to compile it first?");
    }
    return testClasses;
  }
  
  public boolean isAppClass(String className) {
    if (appClassNamePathMap == null) {
      try {
        initAppClassNameSet();
      } catch (IOException e) {
        throw new RuntimeException("Unable to read app classes.", e);
      }
    }
    return appClassNamePathMap.containsKey(className);
  }
  
  public Path getAppClassFilePath(String className) {
    if (appClassNamePathMap == null) {
      try {
        initAppClassNameSet();
      } catch (IOException e) {
        throw new RuntimeException("Unable to read app classes.", e);
      }
    }
    return appClassNamePathMap.get(className);
  }
  
  private void initializeClassLoader() {
    List<Path> appClassesPaths = new ArrayList<>();
    appClassesPaths.addAll(getAppSourceDirs());
    appClassesPaths.addAll(getTestSourceDirs());
    appClassesPaths.addAll(getLibPaths());
    URL[] classpathURLs = new URL[appClassesPaths.size()];
    for (int i = 0; i < appClassesPaths.size(); ++i) {
      try {
        String cpStr = getFileURLSpec(appClassesPaths.get(i));
        classpathURLs[i] = new URL(cpStr);
      } catch (MalformedURLException e) {
        throw new RuntimeException("Internal error: Invalid file URL spec: " 
            + getFileURLSpec(appClassesPaths.get(i)));
      }
    }
    subjectProgramClassesLoader = new URLClassLoader(classpathURLs, getClass().getClassLoader());
  }
  
  private String getFileURLSpec(Path file) {
    file = file.toAbsolutePath().normalize();
    String urlSpec = "file://" + file.toString();
    if (Files.isDirectory(file)) {
      if (!urlSpec.endsWith("/")) {
        urlSpec += "/";
      }
    }
    return urlSpec;
  }
  
  private List<Path> resolvePathStrListAgainstRoot(List<String> pathStrs) {
    List<Path> resolvedPaths = new ArrayList<>();
    if (pathStrs != null) {
      for (String pathStr : pathStrs) {
        Path literalPath = Paths.get(pathStr);
        if (!literalPath.isAbsolute()) {
          Path resolvedPath = getRootDir().resolve(pathStr);
          resolvedPaths.add(resolvedPath);
        } else {
          resolvedPaths.add(literalPath);
        }
      }
    }
    return resolvedPaths;
  }
  
  private void initAppClassNameSet() throws IOException {
    appClassNamePathMap = new HashMap<>();
    for (Path appSourceDir : getAppSourceDirs()) {
      LookupFilesByNameVisitor lookupClassFiles = new LookupFilesByNameVisitor(".*\\.class");
      Files.walkFileTree(appSourceDir, lookupClassFiles);
      List<Path> classFilePaths = lookupClassFiles.getMatchedFilesPaths();
      for (Path classFilePath : classFilePaths) {
        String relativePath = appSourceDir.relativize(classFilePath).toString();
        String className = relativePath.substring(0, relativePath.length() - 6)
            .replaceAll(Pattern.quote(File.separator), ".");
        appClassNamePathMap.put(className, classFilePath.toAbsolutePath());
      }
    }
    if (appClassNamePathMap.size() == 0) {
      System.err.println("Warning: No class files found in the app source directory "
          + "of the subject program " + rootDir.toString());
      System.err.println("Forgot to compile it first?");
    }
    List<String> excludedAppClassNames = programInfo.getAppClassExclusions();
    if (excludedAppClassNames != null) {
      for (String excluded : excludedAppClassNames) {
        appClassNamePathMap.remove(excluded);
      }
    }
  }
  
  public static class Info extends ExtProperties {
    
    private static final long serialVersionUID = 1L;

    public static Info newSubjectProjectInfo() {
      Info info = new Info();
      info.setProperty("app_source.paths", "src");
      info.setProperty("test_source.paths", "test");
      info.setProperty("lib.paths", "");
      info.setProperty("app_package", "");
      info.setProperty("test_classes", "");
      return info;
    }
    
    public static Info readSubjectProjectInfo(File infoFile) throws IOException {
      Info info = new Info();
      info.loadFromFile(infoFile);
      return info;
    }
    
    protected Info() {}
    
    public void setAppSourcePaths(List<String> appSourcePaths) {
      setProperty("app_source.paths", appSourcePaths);
    }
    
    public List<String> getAppSourcePaths() {
      return getPropertyValueList("app_source.paths");
    }
    
    public void setTestSourcePaths(List<String> testSourcePaths) {
      setProperty("test_source.paths", testSourcePaths);
    }
    
    public List<String> getTestSourcePaths() {
      return getPropertyValueList("test_source.paths");
    }
    
    public void setLibPaths(List<String> libPaths) {
      setProperty("lib.paths", libPaths);
    }
    
    public List<String> getLibPaths() {
      return getPropertyValueList("lib.paths");
    }
    
    public void setAppPackage(String appPackage) {
      setProperty("app_package", appPackage);
    }
    
    public String getAppPackage() {
      return getProperty("app_package");
    }
    
    public void setTestClassNames(List<String> testClassNames) {
      setProperty("test_classes", testClassNames);
    }
    
    public List<String> getTestClassNames() {
      return getPropertyValueList("test_classes");
    }
    
    public void setAppClassExclusions(List<String> excludedAppClassNames) {
      setProperty("app_class_exclusions", excludedAppClassNames);
    }
    
    public List<String> getAppClassExclusions() {
      return getPropertyValueList("app_class_exclusions");
    }
  }
}
