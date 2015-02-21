/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.examples.analysis;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
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
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

public class Testing extends AbstractAnalysisEngine {

  private final Set<JarFile> applicationJarFiles;
  private final String applicationMainClass;

  /**
   * The two input parameters define the program to analyze: the jars of .class
   * files and the main class to start from.
   */
  public Testing(Set<JarFile> applicationJarFiles, String applicationMainClass) {
    this.applicationJarFiles = applicationJarFiles;
    this.applicationMainClass = applicationMainClass;
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
   * Collect the set of JarFiles that constitute the system libraries of the
   * running JRE.
   */
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

  /**
   * Take the given set of JarFiles that constitute the program, and return a
   * set of Module files as expected by the WALA machinery.
   */
  private Set<JarFileModule> getModuleFiles() {
    Set<JarFileModule> result = HashSetFactory.make();
    for (Iterator<JarFile> jars = applicationJarFiles.iterator(); jars.hasNext();) {
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
  public void gatherClasses() throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {

    //
    // set the application to analyze
    //
    setModuleFiles(getModuleFiles());

    //
    // set the system jar files to use.
    // change this if you want to use a specific jre version
    //
    setJ2SELibraries(getSystemJars());

    //
    // the application and libraries are set, now build the scope...
    //
    
    File exFile=new FileProvider().getFile(
        "/Users/samir/Work/energy/Wala/tests/Java60RegressionExclusions.txt");
    

    scope = AnalysisScopeReader.readJavaScope("primordial.txt", exFile, getClass()
        .getClassLoader());
    for (int i = 0; i < j2seLibs.length; i++) {
      scope.addToScope(scope.getPrimordialLoader(), j2seLibs[i]);
    }

    // add user stuff
    addApplicationModulesToScope();

    IClassHierarchy cha = buildClassHierarchy();
    assert cha != null : "failed to create class hierarchy";
    setClassHierarchy(cha);
    
    System.err.println("Class hierarchy built...");
    
    Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(
        scope, cha, "LSimpleTest");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);

    CallGraphBuilder builder = Util.makeZeroOneCFABuilder(options, new AnalysisCache(), cha, scope);
    CallGraph cg = builder.makeCallGraph(options, null);
    
    System.err.println("Call graph built...");

    CGNode main = findMainMethod(cg);
    
    System.err.println("Main method found...");

    Statement stmt = findCallTo(main, "println");
    
  
    IR ir = main.getIR();
    
    for(Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
      SSAInstruction s = it.next();
      System.out.println("SSAInstruction: " + s);
      System.out.println("hasDef(): " + s.hasDef()); 
      if(s.hasDef()) {
        
      }
      
      
      System.out.println("getDef(): " + s.getDef());
      for(int i = 0; i < s.getNumberOfUses(); ++i) 
      {
        System.out.println("\tUsed v" + s.getUse(i));
      }
      
    }
    
    
    System.out.println("\n\nStatement: " + stmt);

    Collection<Statement> computeBackwardSlice = Slicer.computeBackwardSlice(stmt, cg, builder.getPointerAnalysis(),
        DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
    
    Collection<Statement> slice = computeBackwardSlice;
    
    dumpSlice(slice);
    
    
    
    
    //find new ArrayList instruction
//    ir = main.getIR();
//    for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
//      SSAInstruction s = it.next();
//      
//      if (s instanceof SSANewInstruction) {
//        SSANewInstruction instr = (SSANewInstruction) s;
//        System.out.println(instr.toString());
//        System.out.println(instr.getConcreteType().toString());
//        
//        
//        Statement nm = new NormalStatement(main, instr.iindex);
//        computeBackwardSlice = Slicer.computeBackwardSlice(nm, cg, builder.getPointerAnalysis(),
//            DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
//        
////        Collection<Statement> computeForwardSlice = Slicer.computeForwardSlice(
////            nm, cg, builder.getPointerAnalysis(),
////            DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
////        
//        slice = computeBackwardSlice;
//        //Collection<Statement> slice = computeForwardSlice;
//        
//        dumpSlice(slice);
//        
//        
////        if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
////          IntSet indices = ir.getCallInstructionIndices(((SSAInvokeInstruction) s).getCallSite());
////          Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
////          return new NormalStatement(n, indices.intIterator().next());
////        }
//      }
//    }

  
    System.out.println("Primordial: " + cha.getLoader(scope.getPrimordialLoader()).getNumberOfClasses());
    System.out.println("Application: " + cha.getLoader(scope.getApplicationLoader()).getNumberOfClasses());
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
  
  public static void main(String[] args) throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException {

    Set<JarFile> jars = HashSetFactory.make();
    jars.add(new JarFile("/Users/samir/Work/energy/Wala/tests/SimpleTest.jar"));
    // jars.add(new
    // JarFile("/Users/samir/git/WALA/com.ibm.wala.core.testdata/hello_hash.jar"));
    //String mainClassName = "Main";

    new Testing(jars, null).gatherClasses();

  }
}
