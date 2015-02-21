package com.ibm.wala.examples.analysis.collectionsusage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.TreeMap;

import com.ibm.wala.classLoader.IClass;




public class UsageStatistics {
  private static StringBuilder sb  = new StringBuilder();
  private static String pathToReport = "/Users/samir/Work/energy/Wala/tests/WALA_report.txt";
  
  public static void addStat(IClass klass, String methodName, 
      Analyzer.Usage usage) {
      
    String className = klass.getName().toString();
    String line = className + "." + methodName;
    for(Integer instance : usage.uses.keySet()) {
      TreeMap<String, Integer> use = usage.uses.get(instance);
      line += "\n\t";
      line += "list" + instance;
      line += " (" + usage.classTypes.get(instance) + ")";
      line += "\n\tUsage: [ ";
      for(String apiCall : use.keySet()) {
        line += "(" + apiCall + ", " + use.get(apiCall) + ") ";            
      }
      line += "]\n";
    }
    sb.append(line + "\n");
    
    System.err.println("UsageStatistics added: " + line);
  }
  
  public static String getReport() {
    try {
      FileOutputStream fos = new FileOutputStream(pathToReport);
      fos.write(sb.toString().getBytes());
      fos.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    return sb.toString();    
  }
  
  class Key implements Comparable<Key> {
    String className;
    String classType;
    String methodName;
    String instanceName;
    String apiCall;
    
    public Key(String className, String classType, String methodName,
        String instanceName, String apiCall) {
      this.className = className;
      this.classType = classType;
      this.methodName = methodName;
      this.instanceName = instanceName;
      this.apiCall = apiCall;
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((apiCall == null) ? 0 : apiCall.hashCode());
      result = prime * result + ((className == null) ? 0 : className.hashCode());
      result = prime * result + ((instanceName == null) ? 0 : instanceName.hashCode());
      result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
      return result;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Key other = (Key) obj;
      if (!getOuterType().equals(other.getOuterType()))
        return false;
      if (apiCall == null) {
        if (other.apiCall != null)
          return false;
      } else if (!apiCall.equals(other.apiCall))
        return false;
      if (className == null) {
        if (other.className != null)
          return false;
      } else if (!className.equals(other.className))
        return false;
      if (instanceName == null) {
        if (other.instanceName != null)
          return false;
      } else if (!instanceName.equals(other.instanceName))
        return false;
      if (methodName == null) {
        if (other.methodName != null)
          return false;
      } else if (!methodName.equals(other.methodName))
        return false;
      return true;
    }
    private UsageStatistics getOuterType() {
      return UsageStatistics.this;
    }

    
    @Override
    public int compareTo(Key o) {
      if(!this.className.equals(o.className))
        return this.className.compareTo(o.className);
      
      if(!this.methodName.equals(o.methodName))
        return this.methodName.compareTo(o.methodName);
      
      if(!this.instanceName.equals(o.instanceName))
        return this.instanceName.compareTo(o.instanceName);

      
      return 0;
    }     
  }
  
  private HashMap<Key, Integer> counts = new HashMap<Key, Integer>();
  
  public void addUsage(String className, String classType,
      String methodName, String instanceName, String apiCall) {
    Key key = new Key(className, classType, methodName, instanceName, apiCall);
    if(!counts.containsKey(key)) {
      counts.put(key, 1);
    }
    else {
      counts.put(key, counts.get(key) + 1);
    }    
  }
  
  @Override
  public String toString() {
    return null;
  }
}


