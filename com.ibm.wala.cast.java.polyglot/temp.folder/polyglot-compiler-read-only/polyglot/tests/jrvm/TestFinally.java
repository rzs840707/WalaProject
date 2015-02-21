/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */

class TestFinally {
  private static int foo() {
    try {
      int a = 1;
      int b = 0;
      return a / b;
    } catch (Exception e) {
      return 1;
    } finally {
      return 2;
    }
  }

  private static int foo2() {
    try {
      throw new Exception();
    } finally {
      return 3;
    }
  }

  public static void main(String[] args) {
    System.out.println("TestFinally.main()");
    System.out.println(TestFinally.foo());
    System.out.println(TestFinally.foo2());
    try {
      System.out.println("hi");      // jsr
      return;
    } finally {
      System.out.println("bye");
    }                              // ret
  }
}
