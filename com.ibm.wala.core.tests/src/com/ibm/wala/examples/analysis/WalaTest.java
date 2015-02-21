package com.ibm.wala.examples.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile; 

import org.eclipse.core.runtime.Plugin;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.perf.Stopwatch;
import com.ibm.wala.util.ref.ReferenceCleanser;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.core.plugin.*;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.plugin.CoreTestsPlugin;
import com.ibm.wala.core.tests.util.TestConstants;

 
 
public class WalaTest {

  /**
   * Should we periodically clear out soft reference caches in an attempt to help the GC?
   */
  private final static boolean PERIODIC_WIPE_SOFT_CACHES = true;

  /**
   * Interval which defines the period to clear soft reference caches
   */
  private final static int WIPE_SOFT_CACHE_INTERVAL = 2500;

  /**
   * Counter for wiping soft caches
   */
  private static int wipeCount = 0;

  /**
   * First command-line argument should be location of scope file for application to analyze
   * 
   * @throws IOException
   * @throws ClassHierarchyException
   */
  
  private final static ClassLoader MY_CLASSLOADER = CountParameters.class.getClassLoader();
  
  public static void main(String args[]) throws IOException, WalaException, ClassNotFoundException, IllegalArgumentException, CallGraphBuilderCancelException {
    final ClassLoader MY_CLASSLOADER = CountParameters.class.getClassLoader();
 
      File exFile=new FileProvider().getFile(
          "/Users/samir/Work/energy/Wala/tests/Java60RegressionExclusions.txt");
      
      String jarFile = "/Users/samir/Work/energy/Wala/tests/SimpleTest.jar";

            
      String scopeFile = args[0];

      // measure running time
      Stopwatch s = new Stopwatch();
      s.start();
      AnalysisScope scope = AnalysisScopeReader.readJavaScope(scopeFile, null, WalaTest.class.getClassLoader());

      // build a type hierarchy
      System.out.print("building class hierarchy...");
      ClassHierarchy cha = ClassHierarchy.make(scope);
      System.out.println("done");
      
      


      // register class hierarchy and AnalysisCache with the reference cleanser, so that their soft references are appropriately wiped
//      ReferenceCleanser.registerClassHierarchy(cha);
//      AnalysisCache cache = new AnalysisCache();
//      ReferenceCleanser.registerCache(cache);
//      //AnalysisOptions options = new AnalysisOptions();
//
//      System.out.print("building IRs...");
//      for (IClass klass : cha) {
//        System.out.println(klass);
//       for (IMethod method : klass.getDeclaredMethods()) {
//          
//       }
////          //wipeSoftCaches();
////          // construct an IR; it will be cached
////          //cache.getSSACache().findOrCreateIR(method, Everywhere.EVERYWHERE, options.getSSAOptions());
////        }
//      }
//      System.out.println("done");
//      s.stop();
//      System.out.println("RUNNING TIME: " + s.getElapsedMillis());

    }
  
  public static CGNode findMainMethod(CallGraph cg) {
    Descriptor d = Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V");
    Atom name = Atom.findOrCreateUnicodeAtom("main");
    return findMethod(cg, d, name);
  }
  private static CGNode findMethod(CallGraph cg, Descriptor d, Atom name) {
    for (Iterator<? extends CGNode> it = cg.getSuccNodes(cg.getFakeRootNode()); it.hasNext();) {
      CGNode n = it.next();
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
        return n;
      }
    }
    // if it's not a successor of fake root, just iterate over everything
    for (CGNode n : cg) {
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
        return n;
      }
    }
    Assertions.UNREACHABLE("failed to find method " + name);
    return null;
  }
  public static CGNode findMethod(CallGraph cg, String name) {
    Atom a = Atom.findOrCreateUnicodeAtom(name);
    for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
      CGNode n = it.next();
      if (n.getMethod().getName().equals(a)) {
        return n;
      }
    }
    System.err.println("call graph " + cg);
    Assertions.UNREACHABLE("failed to find method " + name);
    return null;
  }
  public static Statement findCallTo(CGNode n, String methodName) {
    IR ir = n.getIR();
    for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
      SSAInstruction s = it.next();
      if (s instanceof SSAInvokeInstruction) {
        SSAInvokeInstruction call = (SSAInvokeInstruction) s;
        if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
          IntSet indices = ir.getCallInstructionIndices(((SSAInvokeInstruction) s).getCallSite());
          Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
          return new NormalStatement(n, indices.intIterator().next());
        }
      }
    }
    Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
    return null;
  }
  

    private static void wipeSoftCaches() {
      if (PERIODIC_WIPE_SOFT_CACHES) {
        wipeCount++;
        if (wipeCount >= WIPE_SOFT_CACHE_INTERVAL) {
          wipeCount = 0;
          ReferenceCleanser.clearSoftCaches();
        }
      }
      
    }
    
    private JarFile[] getSystemJars() throws IOException {
      String javaHomePath = "garbage";
      Set<JarFile> jarFiles = HashSetFactory.make();

      // first, see if wala.properties has been set up
      try {
        Properties p = WalaProperties.loadProperties();
        javaHomePath = p.getProperty(WalaProperties.J2SE_DIR);
      } catch (WalaException e) {
        // no luck.
      }

      // if not, try assuming the running JRE looks normal
      File x = new File(javaHomePath);
      if (!(x.exists() && x.isDirectory())) {
        javaHomePath = System.getProperty("java.home");

        if (!javaHomePath.endsWith(File.separator)) {
          javaHomePath = javaHomePath + File.separator;
        }

        javaHomePath = javaHomePath + "lib";
      }

      // find jars from chosen JRE lib path
      collectJars(new File(javaHomePath), jarFiles);

      return jarFiles.toArray(new JarFile[jarFiles.size()]);
    }
    private void collectJars(File f, Set<JarFile> result) throws IOException {
      if (f.isDirectory()) {
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
          collectJars(files[i], result);
        }
      } else if (f.getAbsolutePath().endsWith(".jar")) {
        result.add(new JarFile(f));
      }
    }

    public void setJ2SELibraries(JarFile[] libs) {
      if (libs == null) {
        throw new IllegalArgumentException("libs is null");
      }
      //this.j2seLibs = new Module[libs.length];
      for (int i = 0; i < libs.length; i++) {
        //j2seLibs[i] = new JarFileModule(libs[i]);
      }
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
