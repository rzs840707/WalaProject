package com.ibm.wala.cast.java.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile; 

import org.eclipse.core.runtime.Plugin;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.core.plugin.*;
import com.ibm.wala.core.tests.plugin.CoreTestsPlugin;

 
 
public class WalaTest {


  
  
  public static void main(String args[]) throws IOException, WalaException, ClassNotFoundException {
 
      File exFile=new FileProvider().getFile(
          "/Users/samir/Work/energy/Wala/tests/Java60RegressionExclusions.txt");
      
      String jarFile = "/Users/samir/Work/energy/Wala/tests/SimpleTest.jar";
      
      final List<String> rtJar =
          Arrays.asList(WalaProperties.getJ2SEJarFiles());


      //AnalysisScope scope = MyAnalysisScopeReader.makeJavaBinaryAnalysisScope(
        //  jarFile, rtJar, exFile);
      
      AnalysisScope s = AnalysisScope.createJavaAnalysisScope(); 
      for(String jar : rtJar) {
        s.addToScope(ClassLoaderReference.Primordial, new JarFile(jar));
      }
       
      s.addToScope(ClassLoaderReference.Application, new JarFile(jarFile)); 
      //IClassHierarchy cha = ClassHierarchy.make(s); 
      
      
      
      
      IClassHierarchy ch = ClassHierarchy.make(s);
          
      for (IClass cl : ch) { 
      //if (cl.getClassLoader().getReference().equals(ClassLoaderReference.Application)) { 
        for (IMethod m : cl.getAllMethods()) { 
            String ac = ""; 
            if (m.isAbstract()) ac = ac + "abstract "; 
            if (m.isClinit()) ac = ac + "clinit "; 
            if (m.isFinal()) ac = ac + "final ";  
            if (m.isInit()) ac = ac + "init ";  
            if (m.isNative()) ac = ac + "native ";  
            if (m.isPrivate()) ac = ac + "private "; 
            if (m.isProtected()) ac = ac + "protected ";  
            if (m.isPublic()) ac = ac + "public ";  
            if (m.isSynchronized()) ac = ac + "synchronized ";  
            System.out.println(ac + m.getSignature()); 
        } 
    //} 
} 
      System.out.println("Done");
  }
}
 

class MyModule implements Module
{
  List<ModuleEntry> entries = new ArrayList<ModuleEntry>();
  
  void addEntry(ModuleEntry entry)
  {
    this.entries.add(entry);
  }
  
  @Override
  public Iterator<ModuleEntry> getEntries() {
    return entries.iterator();
  }
  
}


class MyAnalysisScopeReader extends AnalysisScopeReader {
  
  public static AnalysisScope makeJavaBinaryAnalysisScope(
      String classPath, List<String> scjJar, File exclusionsFile) throws IOException {
      return makeJavaBinaryAnalysisScope(classPath, scjJar, exclusionsFile, CoreTestsPlugin.getDefault());
    }

    /**
     * @param classPath class path to analyze, delimited by File.pathSeparator
     * @param exclusionsFile file holding class hierarchy exclusions. may be null
     * @throws IOException 
     * @throws IllegalStateException if there are problems reading wala properties
     */
    public static AnalysisScope makeJavaBinaryAnalysisScope(
        String application, 
        List<String> primordial, File exclusionsFile, Plugin plugIn) throws IOException {     
      AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();      
      
      if (primordial != null && !primordial.isEmpty())
      {
        for(String path : primordial)
          addClassPathToScope(path, scope, scope.getLoader(AnalysisScope.PRIMORDIAL));
        
        if (new File(application).exists()) {
          Module appMixed = new FileProvider().getJarFileModule(application, AnalysisScopeReader.class.getClassLoader());     
          Iterator<? extends ModuleEntry> myit = appMixed.getEntries();
                
          MyModule app = new MyModule();
          
          while (myit.hasNext())
          {
            ModuleEntry entry = myit.next();
            if (!entry.getClassName().startsWith("java") && 
                !entry.getClassName().startsWith("com") && 
                !entry.getClassName().startsWith("joprt") && 
                !entry.getClassName().startsWith("util/Dbg") && 
                !entry.getClassName().startsWith("util/Timer") ){               
              if (entry.isClassFile()) 
                app.addEntry(entry);                
            }
            
          }
          
          scope.addToScope(ClassLoaderReference.Application, 
              new JarFile(application));
                            
          //scope.addToScope(scope.getLoader(AnalysisScope.APPLICATION), app);
        } else {
          System.out.println("File not found "+application);
        }
        
      } else 
      {
        Module appMixed = new FileProvider().getJarFileModule(application, AnalysisScopeReader.class.getClassLoader());     
        Iterator<? extends ModuleEntry> myit = appMixed.getEntries();
              
        MyModule app = new MyModule(), prim = new MyModule();
        
        while (myit.hasNext())
        {
          ModuleEntry entry = myit.next();
          if (entry.getClassName().startsWith("java") || 
              entry.getClassName().startsWith("com") || 
              entry.getClassName().startsWith("joprt") || 
              entry.getClassName().startsWith("util/Dbg") || entry.getClassName().startsWith("util/Timer") ){
              prim.addEntry(entry);
          }     
          else
          {
            if (entry.isClassFile())
              app.addEntry(entry);                        
          }
          
        }
                
        scope.addToScope(scope.getLoader(AnalysisScope.PRIMORDIAL), prim);
        scope.addToScope(scope.getLoader(AnalysisScope.APPLICATION), app);        
      }
      
      return scope;
    }
}
