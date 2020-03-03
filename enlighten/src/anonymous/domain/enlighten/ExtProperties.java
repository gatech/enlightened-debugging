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


package anonymous.domain.enlighten;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;


public class ExtProperties extends Properties {

  private static final long serialVersionUID = 1L;
  
  public static ExtProperties readConfigFile(File configFile) throws IOException {
    ExtProperties instance = new ExtProperties();
    instance.loadFromFile(configFile);
    return instance;
  }
  
  public ExtProperties() {}
  
  public ExtProperties(Properties prop) {
    super(prop);
  }

  
  public void setProperty(String key, List<String> valueList) {
    StringBuilder valueBuilder = new StringBuilder();
    for (int i = 0; i < valueList.size(); ++i) {
      String value = valueList.get(i);
      validatePropertyValue(value);
      valueBuilder.append(value);
      if (i != valueList.size() - 1) {
        valueBuilder.append(", ");
      }
    }
    super.setProperty(key, valueBuilder.toString());
  }
  
  public List<String> getPropertyValueList(String key) {
    return parseAsList(getProperty(key));
  }
  
  
  @Override
  public Object setProperty(String key, String value) {
    validatePropertyValue(value);
    return super.setProperty(key, value);
  }
  
  
  public List<String> parseAsList(String rawPropertyValue) {
    if (rawPropertyValue == null) {
      return null;
    }
    List<String> valueList = new ArrayList<>();
    String[] valueComponents = rawPropertyValue.split(Pattern.quote(", "));
    for (String valueComponent : valueComponents) {
      valueComponent = valueComponent.trim();
      if (!valueComponent.isEmpty()) {
        valueList.add(valueComponent);
      }
    }
    return valueList;
  }
  
  public void loadFromFile(File configFile) throws IOException {
    InputStream stream = new FileInputStream(configFile);
    load(stream);
    stream.close();
  }
  
  public void storeToFile(File configFile) throws IOException {
    OutputStream stream = new FileOutputStream(configFile);
    store(stream, null);
    stream.close();
  }
  


  private void validatePropertyValue(String singleValue) {
    if (singleValue.contains(", ")) {
      throw new RuntimeException(
          "Config property values are not allowed to contain the sequence \", \"");
    }
  }
}
