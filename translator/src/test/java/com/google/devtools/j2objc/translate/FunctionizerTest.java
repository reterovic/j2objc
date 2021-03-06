/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc.translate;

import com.google.devtools.j2objc.GenerationTest;
import com.google.devtools.j2objc.Options;

import java.io.IOException;

/**
 * Tests for {@link Functionizer}.
 *
 * @author Tom Ball
 */
public class FunctionizerTest extends GenerationTest {

  @Override
  protected void setUp() throws IOException {
    super.setUp();
    Options.enableFinalMethodsAsFunctions();
  }

  @Override
  protected void tearDown() throws Exception {
    Options.resetFinalMethodsAsFunctions();
    super.tearDown();
  }

  public void testPrivateInstanceMethodNoArgs() throws IOException {
    String translation = translateSourceFile(
      "class A { String test(String msg) { return str(); } " +
      "  private String str() { return toString(); }}",
      "A", "A.h");
    String functionHeader = "__attribute__ ((unused)) static NSString * A_str_(A * self)";
    assertNotInTranslation(translation, functionHeader);
    translation = getTranslatedFile("A.m");
    assertTranslation(translation, functionHeader + ";");
    assertTranslation(translation, functionHeader + " {");
    assertTranslation(translation, "return A_str_(self);");
    assertTranslation(translation, "return [self description];");
  }

  // Verify one function calls another with the instance parameter.
  public void testPrivateToPrivate() throws IOException {
    String translation = translateSourceFile(
      "class A { private String test(String msg) { return str(); } " +
      "  private String str() { return toString(); }}",
      "A", "A.m");
    assertTranslation(translation, "return A_str_(self);");
    assertTranslation(translation, "return [self description];");
  }

  public void testPrivateInstanceMethod() throws IOException {
    String translation = translateSourceFile(
      "class A { String test(String msg) { return str(msg, getClass()); } " +
      "  private String str(String msg, Class<?> cls) { return msg + cls; }}",
      "A", "A.h");
    String functionHeader =
        "__attribute__ ((unused)) static " +
        "NSString * A_str_(A * self, NSString * msg, IOSClass * cls)";
    assertNotInTranslation(translation, functionHeader);
    translation = getTranslatedFile("A.m");
    assertTranslation(translation, functionHeader + ";");
    assertTranslatedLines(translation, functionHeader + " {",
        "return [NSString stringWithFormat:@\"%@%@\", msg, cls];");
    assertTranslation(translation, "return A_str_(self, msg, [self getClass]);");
  }

  // Verify non-private instance method is generated normally.
  public void testNonPrivateInstanceMethod() throws IOException {
    String translation = translateSourceFile(
      "class A { String test(String msg) { return str(msg, getClass()); } " +
      "  String str(String msg, Class<?> cls) { return msg + cls; }}",
      "A", "A.m");
    assertTranslatedLines(translation,
        "- (NSString *)strWithNSString:(NSString *)msg",
        "withIOSClass:(IOSClass *)cls {");
    assertTranslation(translation,
        "return [self strWithNSString:msg withIOSClass:[self getClass]];");
  }

  // Verify instance field access in function.
  public void testFieldAccessInFunction() throws IOException {
    String translation = translateSourceFile(
        "class A { String hello = \"hello\";" +
        "  String test() { return str(); } " +
        "  private String str() { return hello; }}",
        "A", "A.m");
      assertTranslatedLines(translation,
          "- (NSString *)test {",
          "return A_str_(self);");
      assertTranslatedLines(translation,
          "__attribute__ ((unused)) static NSString * A_str_(A * self) {",
          "return self->hello_;");
  }

  // Verify super field access in function.
  public void testSuperFieldAccessInFunction() throws IOException {
    String translation = translateSourceFile(
        "class A { String hello = \"hello\";" +
        "  static class B extends A { void use() { str(); }" +
        "  private String str() { super.hello = \"hi\"; return super.hello; }}}",
        "A", "A.m");
      assertTranslatedLines(translation,
          "__attribute__ ((unused)) static NSString * A_B_str_(A_B * self) {",
          "A_set_hello_(self, @\"hi\");",
          "return self->hello_;");
  }

  // Verify there isn't any super method invocations in functions.
  public void testSuperMethodInvocationInFunction() throws IOException {
    String translation = translateSourceFile(
        "class A { " +
        "  private String hello() { return \"hello\"; } " +
        "  public String shout() { return \"HELLO\"; } " +
        "  void use() { hello(); } " +
        "  static class B extends A { " +
        "  private String test1() { return super.hello(); } " +
        "  private String test2() { return super.shout(); }" +
        "  void use() { test1(); test2(); }}}",
        "A", "A.m");
      assertTranslatedLines(translation,
          "- (NSString *)test1 {",
          "return [super hello];");
      assertTranslatedLines(translation,
          "- (NSString *)test2 {",
          "return [super shout];");
  }

  // Verify functions can call other functions, correctly passing the instance variable.
  // Also tests that overloaded functions work.
  public void testFunctionCallingFunction() throws IOException {
    String translation = translateSourceFile(
        "class A { String hello = \"hello\";" +
        "  String test() { return str(0); } " +
        "  private String str(int i) { return str(); }" +
        "  private String str() { return hello; } }",
        "A", "A.m");
    translation = getTranslatedFile("A.m");
    assertTranslatedLines(translation,
        "- (NSString *)test {",
        "return A_str_(self, 0);");
    assertTranslatedLines(translation,
        "__attribute__ ((unused)) static NSString * A_str_(A * self, int i) {",
        "return A_str_2(self);");
    assertTranslatedLines(translation,
        "__attribute__ ((unused)) static NSString * A_str_2(A * self) {",
        "return self->hello_;");
  }

  // Verify this expressions are changed to self parameters in functions.
  public void testThisParameters() throws IOException {
    String translation = translateSourceFile(
        "class A { private void test(java.util.List list) { list.add(this); }}",
        "A", "A.m");
    assertTranslatedLines(translation, "[((id<JavaUtilList>) nil_chk(list)) addWithId:self];");
  }

  // Verify that a call to a private method in an outer class is converted correctly.
  public void testOuterCall() throws IOException {
    String translation = translateSourceFile(
        "class A { int outerN = str(); private int str() { return 0; }" +
        "  class B { " +
        "    private int test1() { return str(); } " +
        "    private int test2() { return A.this.str(); }" +
        "    private int test3() { return A.this.outerN; }}}",
        "A", "A.m");
    assertTranslatedLines(translation,
        "__attribute__ ((unused)) static int A_str_(A * self) {",
        "return 0;");
    assertTranslatedLines(translation,
        "- (int)test1 {",
        "return A_str_(this$0_);");
    assertTranslatedLines(translation,
        "- (int)test2 {",
        "return A_str_(this$0_);");
    assertTranslatedLines(translation,
        "- (int)test3 {",
        "return this$0_->outerN_;");
  }

  // Verify that a call to a private method in an outer class is converted correctly.
  public void testInnerOuterCall() throws IOException {
    String translation = translateSourceFile(
        "class A { private int str() { return 0; }" +
        "  class B { " +
        "    private int test() { return str(); }}" +
        "  class C { " +
        "    private void test(B b) { b.test(); }}}",
        "A", "A.m");
    assertTranslatedLines(translation,
        "- (void)testWithA_B:(A_B *)b {",
        "A_B_test_(nil_chk(b));");
  }

  // Verify annotation parameters are ignored.
  public void testAnnotationParameters() throws IOException {
    String translation = translateSourceFile("import java.lang.annotation.*; " +
    		"@Target({ElementType.METHOD}) public @interface Test {}", "Test", "Test.m");
    assertNotInTranslation(translation, "self");
  }

  public void testStaticMethod() throws IOException {
    String translation = translateSourceFile(
      "class A { String test(String msg) { return str(msg, getClass()); } " +
      "  private static String str(String msg, Class<?> cls) { return msg + cls; }}",
      "A", "A.h");
    String functionHeader = "NSString * A_str_(NSString * msg, IOSClass * cls)";
    assertNotInTranslation(translation, functionHeader + ';');
    translation = getTranslatedFile("A.m");
    // Check new function.
    assertTranslatedLines(translation, functionHeader + " {",
        "A_init();",
        "return [NSString stringWithFormat:@\"%@%@\", msg, cls];");
    // Check wrapper.
    assertTranslatedLines(translation,
        "+ (NSString *)strWithNSString:(NSString *)msg",
        "withIOSClass:(IOSClass *)cls {",
        "return A_str_(msg, cls);");
    // Check invocation.
    assertTranslatedLines(translation,
        "- (NSString *)testWithNSString:(NSString *)msg {",
        "return A_str_(msg, [self getClass]);");
  }

  public void testFunctionParameter() throws IOException {
    String translation = translateSourceFile(
      "class A { private String test(String msg) { return echo(str(msg)); } " +
      "  private String echo(String msg) { return msg; } " +
      "  private String str(String msg) { return msg; }}",
      "A", "A.m");
    assertTranslatedLines(translation, "A_echo_(self, A_str_(self, msg))");
  }

  public void testStaticVarargsMethod() throws IOException {
    String translation = translateSourceFile(
      "class A { String test(String msg) { return strchars('a', 'b', 'c'); } " +
      "  private static String strchars(char... args) { return String.valueOf(args); }}",
      "A", "A.h");
    String functionHeader = "NSString * A_strchars_(IOSCharArray * args)";
    assertNotInTranslation(translation, functionHeader + ';');
    translation = getTranslatedFile("A.m");
    assertTranslation(translation, functionHeader + " {");
    assertTranslation(translation, "return A_strchars_([" +
    		"IOSCharArray arrayWithChars:(unichar[]){ 'a', 'b', 'c' } count:3]);");
  }

  public void testAssertInFunction() throws IOException {
    String translation = translateSourceFile(
        "class A { void use(String s) { test(s); } " +
        "  private void test(String msg) { assert msg != null : \"null msg\"; }}",
        "A", "A.m");
    assertTranslation(translation, "NSCAssert");
    assertNotInTranslation(translation, "NSAssert");
  }

  public void testSynchronizedFunction() throws IOException {
    String translation = translateSourceFile(
      "class A { void test() { str(); } private synchronized String str() { return toString(); }}",
      "A", "A.m");
    assertTranslation(translation, "@synchronized (self)");
    translation = translateSourceFile(
        "class A { void test() { str(); } " +
        "  private String str() { synchronized(this) { return toString(); }}}",
        "A", "A.m");
    assertTranslation(translation, "@synchronized (self)");
    translation = translateSourceFile(
        "class A { void test() { str(); } " +
        "  private static synchronized String str() { return \"abc\"; }}",
        "A", "A.m");
    assertTranslation(translation, "@synchronized ([IOSClass classWithClass:[A class]])");
    translation = translateSourceFile(
        "class A { void test() { str(); } " +
        "  private String str() { synchronized(this.getClass()) { return \"abc\"; }}}",
        "A", "A.m");
    assertTranslation(translation, "@synchronized ([self getClass])");
    translation = translateSourceFile(
        "class A { void test() { str(); } " +
        "  private static String str() { synchronized(A.class) { return \"abc\"; }}}",
        "A", "A.m");
    assertTranslation(translation, "@synchronized ([IOSClass classWithClass:[A class]])");
  }

  public void testSetter() throws IOException {
    String translation = translateSourceFile(
        "class A { Object o; private void setO(Object o) { this.o = o; }}",
        "A", "A.m");
    assertTranslation(translation, "A_set_o_(self, o)");
  }

  public void testClassInitializerCalledFromFunction() throws IOException {
    String translation = translateSourceFile(
        "class A { static Object o = new Object(); " +
        "  private static Object foo() { return o; }" +
        "  void test() { A.foo(); }" +
        "  private void test2() {}" +
        "  void use() { test2(); }}",
        "A", "A.m");
    // Verify static class function calls class init.
    assertTranslatedLines(translation, "id A_foo_() {", "A_init();", "return A_o_;", "}");
    // Verify class method doesn't call class init.
    assertTranslatedLines(translation, "- (void)test {", "A_foo_();", "}");
    // Verify non-static class function doesn't call class init.
    assertTranslatedLines(translation,
        "__attribute__ ((unused)) static void A_test2_(A * self) {", "}");
  }

  public void testPrivateNativeMethod() throws IOException {
    String translation = translateSourceFile(
        "class A { Object o; void use() { setO(null); } " +
        "  private native void setO(Object o) /*-[ self->o_ = o; ]-*/; }",
        "A", "A.m");
    assertTranslation(translation, "static void A_setO_(A * self, id o) { self->o_ = o; }");
    assertTranslatedLines(translation, "- (void)setOWithId:(id)o {", "A_setO_(self, o);", "}");
  }
}

