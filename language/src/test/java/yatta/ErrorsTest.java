package yatta;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import yatta.runtime.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ErrorsTest {
  private Context context;

  @BeforeEach
  public void initEngine() {
    context = Context.newBuilder().allowAllAccess(true).build();
  }

  @AfterEach
  public void dispose() {
    context.close();
  }

  @Test
  public void oneArgFunctionTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "\\arg -> argx").execute(6);
      } catch (PolyglotException ex) {
        assertEquals("Identifier 'argx' not found in the current scope", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void invocationInLet1Test() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "let\n" +
            "funone = \\arg -> arg\n" +
            "alias = 6\n" +
            "funalias = \\arg -> funoneX alias\n" +
            "in funalias").execute(5).asLong();
      } catch (PolyglotException ex) {
        assertEquals("Identifier 'funoneX' not found in the current scope", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void invocationInLet2Test() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "let\n" +
            "funone = \\arg -> arg\n" +
            "alias = 6\n" +
            "funalias = \\arg -> funoneX alias\n" +
            "in whatever").execute(5).asLong();
      } catch (PolyglotException ex) {
        assertEquals("Identifier 'whatever' not found in the current scope", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void invalidNumberOfArgsTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "let \n" +
            "funone = \\arg -> arg \n" +
            "alias = 6\n" +
            "funalias = \\arg -> funone alias 7\n" +
            "in funalias").execute(5).asLong();
      } catch (PolyglotException ex) {
        assertEquals("Unexpected number of arguments when calling '$lambda-0': 2 expected: 1", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void callOfNonFunctionTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "let\n" +
            "funone = \\arg -> arg\n" +
            "alias = 6\n" +
            "funalias = \\arg -> alias 7 funone\n" +
            "in funalias").execute(5).asLong();
      } catch (PolyglotException ex) {
        assertEquals("Cannot invoke non-function node: SimpleIdentifierNode{name='alias'}", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void callOfPrivateModuleFunctionTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "let\n" +
            "testMod = module TestMod exports funone as\n" +
            "funone argone = funtwo argone\n" +
            "funtwo argone = argone\n" +
            "in testMod.funtwo 6").asLong();
      } catch (PolyglotException ex) {
        assertEquals("Function funtwo is not present in Module{fqn=TestMod, exports=[funone], functions={funone=funone, funtwo=funtwo}}", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void freeVarOverridePatternTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "\\arg -> case arg of\n" +
            "(1, 2, 3) -> 6\n" +
            "(1, secondArg, 3) -> 2 + secondArg\n" +
            "(1, _, _) -> 1 + secondArg\n" +
            "(2, 3) -> 5\n" +
            "_ -> 9\n" +
            "end\n").execute(new Tuple(1l, 5l, 6l)).asLong();
      } catch (PolyglotException ex) {
        assertEquals("Identifier 'secondArg' not found in the current scope", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void simpleIntNoMatchPatternTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "\\arg -> case arg of\n" +
            "1 -> 2\n" +
            "2 -> 3\n" +
            "end\n").execute(3l).asLong();
      } catch (PolyglotException ex) {
        assertEquals("NoMatchException", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void asyncRaiseTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "async \\-> raise :something \"Error description\"\n");
      } catch (PolyglotException ex) {
        assertEquals("YattaError <something>: Error description", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void logicalNotOnNonBooleanValueTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "!\"hello\"");
      } catch (PolyglotException ex) {
        assertEquals("Type error at Unnamed line 1 col 1: operation \"!\" not defined for String \"hello\"", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void logicalNotOnNonBooleanPromiseTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "!(async \\->\"hello\")");
      } catch (PolyglotException ex) {
        assertEquals("Type error at Unnamed line 1 col 1: operation \"!\" not defined for String \"hello\"", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void binaryNotOnNonIntegerValueTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "~\"hello\"");
      } catch (PolyglotException ex) {
        assertEquals("Type error at Unnamed line 1 col 1: operation \"~\" not defined for String \"hello\"", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void binaryNotOnNonIntegerPromiseTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "~(async \\->\"hello\")");
      } catch (PolyglotException ex) {
        assertEquals("Type error at Unnamed line 1 col 1: operation \"~\" not defined for String \"hello\"", ex.getMessage());
        throw ex;
      }
    });
  }

  @Test
  public void asyncWrongCallbackTest() {
    assertThrows(PolyglotException.class, () -> {
      try {
        context.eval(YattaLanguage.ID, "async \\a b -> a + b");
      } catch (PolyglotException ex) {
        assertEquals("async function accepts only functions with zero arguments. Function $lambda-0 expects 2arguments", ex.getMessage());
        throw ex;
      }
    });
  }
}