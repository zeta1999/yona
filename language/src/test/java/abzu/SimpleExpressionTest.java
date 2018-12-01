package abzu;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleExpressionTest {

  private Context context;

  @Before
  public void initEngine() {
    context = Context.create();
  }

  @After
  public void dispose() {
    context.close();
  }

  @Test
  public void longValueTest() {
    long ret = context.eval("abzu", "5").asLong();
    assertEquals(5l, ret);
  }

  @Test
  public void byteValueTest() {
    byte ret = context.eval("abzu", "5b").asByte();
    assertEquals(5, ret);
  }

  @Test
  public void floatValueTest() {
    double ret = context.eval("abzu", "5.0").asDouble();
    assertEquals(5.0, ret, 0);
  }

  @Test
  public void unitValueTest() {
    assertEquals("NONE", context.eval("abzu", "()").toString());
  }

  @Test
  public void stringValueTest() {
    String ret = context.eval("abzu", "\"abzu-string\"").asString();
    assertEquals("abzu-string", ret);
  }

  @Test
  public void symbolValueTest() {
    String ret = context.eval("abzu", ":abzuSymbol").asString();
    assertEquals("abzuSymbol", ret);
  }

  @Test
  public void tupleValueTest() {
    Value tuple = context.eval("abzu", "(1, 2, 3)");
    assertEquals(3, tuple.getArraySize());

    Object[] array = tuple.as(Object[].class);
    assertEquals(1l, array[0]);
    assertEquals(2l, array[1]);
    assertEquals(3l, array[2]);
  }

  @Test
  public void zeroArgFunctionTest() {
    long ret = context.eval("abzu", "fun = 5").execute().asLong();
    assertEquals(5l, ret);
  }

  @Test
  public void oneArgFunctionTest() {
    long ret = context.eval("abzu", "fun arg = arg").execute(6).asLong();
    assertEquals(6l, ret);
  }

  @Test
  public void twoArgFunctionFirstTest() {
    long ret = context.eval("abzu", "fun argone argtwo = argone").execute(5, 6).asLong();
    assertEquals(5l, ret);
  }

  @Test
  public void twoArgFunctionSecondTest() {
    long ret = context.eval("abzu", "fun argone argtwo = argtwo").execute(5, 6).asLong();
    assertEquals(6l, ret);
  }

  @Test
  public void moduleTest() {
    String src = "module test exports fun as\n" +
        "fun = 6\n" +
        "other_fun = 7";
    Value modVal = context.eval("abzu", src);

    assertTrue(modVal.hasMember("fun"));
    assertTrue(modVal.hasMember("other_fun"));
    assertFalse(modVal.hasMember("whatever"));
  }

  @Test
  public void letOneAliasTest() {
    long ret = context.eval("abzu", "fun test = let alias := test in alias").execute(5).asLong();
    assertEquals(5l, ret);
  }

  @Test
  public void letTwoAliasesTest() {
    Value ret = context.eval("abzu", "fun test =\n" +
                                     "let alias    := test\n" +
                                     "    aliastwo := 6\n" +
                                     "in\n" +
                                     "(alias, aliastwo)").execute(5);
    assertEquals(2, ret.getArraySize());

    Object[] array = ret.as(Object[].class);
    assertEquals(5l, array[0]);
    assertEquals(6l, array[1]);
  }

  @Test
  public void letNotInFunctionTest() {
    long ret = context.eval("abzu", "let alias := 6 in alias").asLong();
    assertEquals(6l, ret);
  }

  @Test
  public void letFunctionAliasTest() {
    long ret = context.eval("abzu", "let funalias := fun arg = arg in funalias").execute(5).asLong();
    assertEquals(5l, ret);
  }
}
