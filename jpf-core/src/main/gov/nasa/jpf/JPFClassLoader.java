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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;


public class JPFClassLoader extends URLClassLoader {

  String[] nativeLibs;


  static {

  }
  
  public JPFClassLoader (URL[] urls){
    super(urls);
  }

  public JPFClassLoader (URL[] urls, String[] libs, ClassLoader parent){
    super(urls, parent);

    nativeLibs = libs;
  }

  @Override
  protected String findLibrary (String libBaseName){

    if (nativeLibs != null){
      String libName = File.separator + System.mapLibraryName(libBaseName);

      for (String libPath : nativeLibs) {
        if (libPath.endsWith(libName)) {
          return libPath;
        }
      }
    }

    return null; 
  }

  
  @Override
  public void addURL (URL url){
    if (url != null){
      super.addURL(url);
    }
  }
  
  public void setNativeLibs (String[] libs){
    nativeLibs = libs;
  }
}
