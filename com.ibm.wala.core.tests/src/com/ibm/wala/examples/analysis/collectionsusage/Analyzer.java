package com.ibm.wala.examples.analysis.collectionsusage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;

import javax.jws.soap.SOAPBinding.Use;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;
import com.sun.javafx.collections.MappingChange.Map;
import com.sun.xml.internal.ws.developer.UsesJAXBContext;

public class Analyzer extends AbstractAnalysisEngine {
  private Set<JarFile> applicationJars;
  private IClassHierarchy classHierarchy;
  private CallGraph callGraph;
  
  private String[] allowedListApis = new String[] {"add", "get", "iterator"};
  private String[] allowedMapApis = new String[] {"put", "get", "contains", "entrySet", "iterator"};
  private String[] allowedCollections = new String[] {
      "ArrayList", "LinkedList", "HashMap", "TreeMap", "LinkedHashMap"
  };
  
  
  public Analyzer(Set<JarFile> applicationJars) {
    this.applicationJars = applicationJars;
  }
  
  /***
   * Creates a JarFile corresponding to an absolute path
   * @param pathToJarFile Absolute path to the application jar file
   * @throws IOException
   */
  public Analyzer(String pathToJarFile) throws IOException {
    HashSet<JarFile> jars = HashSetFactory.make();
    jars.add(new JarFile("/Users/samir/Work/energy/Wala/tests/SimpleTest.jar"));
    this.applicationJars = jars;
  }
  
  @Override
  protected CallGraphBuilder getCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
    return Util.makeZeroCFABuilder(options, cache, cha, scope);
  }

  /**
   * Given a root path, add it to the set if it is a jar, or traverse it
   * recursively if it is a directory.
   */
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


  /**
   * Take the given set of JarFiles that constitute the program, and return a
   * set of Module files as expected by the WALA machinery.
   */
  private Set<JarFileModule> getModuleFiles() {
    Set<JarFileModule> result = HashSetFactory.make();
    for (Iterator<JarFile> jars = this.applicationJars.iterator(); jars.hasNext();) {
      result.add(new JarFileModule(jars.next()));
    }

    return result;
  }

  /**
   * The heart of the analysis.
   * 
   * @throws CancelException
   * @throws IllegalArgumentException
   */
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

  public static void dumpSlice(Collection<Statement> slice) {
    dumpSlice(slice, new PrintWriter(System.err));
  }

  public static void dumpSlice(Collection<Statement> slice, PrintWriter w) {

    w.println("SLICE:\n");
    int i = 1;
    for (Statement s : slice) {
      String line = (i++) + "   " + s;
      w.println(line);
      w.flush();
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
  
  public void analyze() throws IOException, IllegalArgumentException, CallGraphBuilderCancelException {
    loadClasses();
    
    this.classHierarchy = buildClassHierarchy();
    List<IClass> classes = getApplicationClasses();
    
    analyzeCollectionsUsage(classes);
    
    System.err.println("Done with analyze()");
  }
  
  private void analyzeCollectionsUsage(List<IClass> classes) throws IllegalArgumentException, CallGraphBuilderCancelException {
    for(IClass klass : classes) {
      analyzeCollectionsUsageInClass(klass);
    }
    
    System.err.println("Done analyzing collections usage in all application classes");
  }

  private void analyzeCollectionsUsageInClass(IClass klass) throws IllegalArgumentException, CallGraphBuilderCancelException {
    // precondition
    assert this.classHierarchy != null;
   
    
    String className = klass.getName().toString();
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
        scope, this.classHierarchy, className);
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, 
        new AnalysisCache(), this.classHierarchy, scope);
    this.callGraph = builder.makeCallGraph(options, null);
    
    List<String> methodNames = getMethodNames(klass);

    analyzeMethodsFromCallGraph(methodNames, klass);
    
    
    System.err.println("Done analyzing collecitons usage in class: " + className);
  }

  private List<String> getMethodNames(IClass klass) {
    List<String> toret = new ArrayList<String>(); 
    for(IMethod method : klass.getDeclaredMethods()) {
      toret.add(method.getName().toString());
    }
    
    
    System.err.println("Methods for " + klass.getName().toString());
    System.err.println(toret);
    
    return toret;
  }

  private void analyzeMethodsFromCallGraph(List<String> methodNames, IClass klass) {
    /* Currently, I can list all IRs from all methods of a particular class
     * Now, I need a DS that can store Class->Method->CollectionsApi->counts
     * Going over the IRs, i can insert into this DS
     * Later, printing it out shall do it.
     * */
    
    for(String methodName : methodNames) {      
      System.err.println("Method " + methodName +  " found");
      CGNode methodNode = null;
      try {
        methodNode = findMethod(this.callGraph, methodName);
      }
      catch (UnimplementedError ue) {
        continue;
      }
            
      Usage usage = getStatsForMethod(methodNode);      
      if(usage == null || usage.uses.isEmpty()) continue;
      
      UsageStatistics.addStat(klass, methodName, usage);
      
      

      System.err.println("------------------------");     
    }
    System.err.println("Analyzing methods for the class in the call graph");
  }

  private Usage getStatsForMethod(CGNode methodNode) {
    ArrayList<String> listApis = new ArrayList<String>();
    listApis.addAll(Arrays.asList(this.allowedListApis));
    
    Usage usage = new Usage();
    
    IR ir = methodNode.getIR();
    if(ir == null) return null;
    
    for(Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
      SSAInstruction s = it.next();                
      
      if(s.toString().contains("Primordial"))  {
        return null;
      }
      
      if(s instanceof SSANewInstruction) {
        SSANewInstruction newInstruction = (SSANewInstruction) s;
        if(!(newInstruction.getConcreteType().toString().contains("ArrayList")
            || newInstruction.getConcreteType().toString().contains("LinkedList"))) {
          continue;
        }
        
        int def = newInstruction.getDef();
        usage.nInstance.add(def);
        usage.uses.put(def, new TreeMap<String, Integer>());
        usage.classTypes.put(def, newInstruction.getNewSite().getDeclaredType().getName().toString());
        
        System.err.println(newInstruction.toString());
//        System.err.println("DEF: " + newInstruction.getDef());
      }
      else if(s instanceof SSAInvokeInstruction) {
        SSAInvokeInstruction invokeInstruction = (SSAInvokeInstruction) s;
        
//        System.err.println(invokeInstruction.toString());
//        System.err.println("def(): " + invokeInstruction.getDef());
//        System.err.println("getNumberOfParameters(): " + invokeInstruction.getNumberOfParameters());
//        System.err.println("getReceiver(): " + invokeInstruction.getReceiver());
        
        int receiver = invokeInstruction.getReceiver();
        int nParams = invokeInstruction.getNumberOfParameters() - 1;
        
//        System.err.println("getInvocationString(): " + invokeInstruction.getCallSite().getInvocationString());
        
        if(!usage.nInstance.contains(receiver)) continue;
        
////    if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {

        String apiCall = invokeInstruction.getCallSite().getDeclaredTarget().getName().toString();
        if(!listApis.contains(apiCall))
          continue;
        
        apiCall += ":" + nParams;
        
        TreeMap<String, Integer> callStatistics = usage.uses.get(receiver);
        
        if(!callStatistics.containsKey(apiCall)) {
          callStatistics.put(apiCall, 1);
        }
        else {
          int currentFreq = callStatistics.get(apiCall);
          callStatistics.put(apiCall,  currentFreq + 1);
        }
               
      }
      
      /* Now we have a map
       * {instanceN -> { apiCall : count, 
       *                  apiCall : count } }
       * */
      
    }
    
    return usage;
  }

  private List<IClass> getApplicationClasses() {
    List<IClass> toret = new ArrayList<IClass>();
    
    for(Iterator<IClass> it = this.classHierarchy.iterator(); it.hasNext(); ) {
      IClass klass = it.next();
      if(klass.getClassLoader().getReference().equals(ClassLoaderReference.Application)) {
        toret.add(klass);
      }
    }
    
    System.err.println("All classes extracted from Class Hierarchy. Here's the list: ");
    System.err.println(toret);
    
    return toret;
  }

  /***
   * Load class from the application jar and populates the scope
   * @throws IOException
   */
  private void loadClasses()  throws IOException {
    setModuleFiles(getModuleFiles());
    setJ2SELibraries(getSystemJars());
    File exFile=new FileProvider().getFile(
        "/Users/samir/Work/energy/Wala/tests/Java60RegressionExclusions.txt");
    
    scope = AnalysisScopeReader.readJavaScope("primordial.txt", exFile, getClass()
        .getClassLoader());
    for (int i = 0; i < j2seLibs.length; i++) {
      scope.addToScope(scope.getPrimordialLoader(), j2seLibs[i]);
    }

    addApplicationModulesToScope();
    
    System.err.println("Classes loaded in scope");
  }
  
  /*
   * 1. From class hierarchy, get a list of class names
   * 2. Create entry points to each class by class name
   * 3. Iterate over each method of the class
   *     a. The class hierarchy gives a list of methods and their corresponding selectors
   *     b. we can use these selectors to put into the call graph to get their IR
   *     c. From the IR, we can look for specific instructions 
   * 4. In IR, Look for new ArrayList instructions
   * 5. Trace the instance in that method, gathering usage stats
   * 6. Discard methods where the instance is a parameter to a method call
   * */
  
  
  public static void main(String[] args) throws IOException, IllegalArgumentException, CallGraphBuilderCancelException {
    Analyzer analyzer = new Analyzer("/Users/samir/Work/energy/Wala/tests/SimpleTest.jar"); 
    analyzer.analyze();
    UsageStatistics.getReport();
  }

  static class Usage {
    TreeSet<Integer> nInstance = new TreeSet<Integer>();
    TreeMap<Integer, TreeMap<String, Integer>> uses = new TreeMap<Integer, TreeMap<String,Integer>>();
    TreeMap<Integer, String> classTypes = new TreeMap<Integer, String>();
  } 
}
