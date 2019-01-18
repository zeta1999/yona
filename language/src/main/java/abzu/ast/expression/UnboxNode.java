package abzu.ast.expression;

import abzu.AbzuException;
import abzu.ast.ExpressionNode;
import abzu.ast.interop.ForeignToAbzuTypeNode;
import abzu.runtime.*;
import abzu.runtime.Module;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

@NodeChild("child")
public abstract class UnboxNode extends ExpressionNode {

  @Specialization
  protected long unboxLong(long value) {
    return value;
  }

  @Specialization
  protected boolean unboxBoolean(boolean value) {
    return value;
  }

  @Specialization
  protected byte unboxByte(byte value) {
    return value;
  }

  @Specialization
  protected float unboxFloat(float value) {
    return value;
  }

  @Specialization
  protected String unboxString(String value) {
    return value;
  }

  @Specialization
  protected Function unboxFunction(Function value) {
    return value;
  }

  @Specialization
  protected Unit unboxNull(Unit value) {
    return value;
  }

  @Specialization
  protected Tuple unboxTuple(Tuple value) {
    return value;
  }

  @Specialization
  protected Module unboxModule(Module value) {
    return value;
  }

  @Specialization
  protected StringList unboxStringList(StringList value) {
    return value;
  }

  @Specialization
  protected Sequence unboxSequence(Sequence value) {
    return value;
  }

  @Specialization
  protected ExpressionNode unboxValue(ExpressionNode value) {
    return value;
  }

  @Specialization(guards = "isBoxedPrimitive(value)")
  protected Object unboxBoxed(
      Object value,
      @Cached("create()") ForeignToAbzuTypeNode foreignToAbzu) {
    return foreignToAbzu.unbox((TruffleObject) value);
  }

  @Specialization(guards = "!isBoxedPrimitive(value)")
  protected Object unboxGeneric(Object value) {
    return value;
  }

  @Node.Child private Node isBoxed;

  protected boolean isBoxedPrimitive(Object value) {
    if (value instanceof TruffleObject) {
      if (isBoxed == null) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        isBoxed = insert(Message.IS_BOXED.createNode());
      }
      if (ForeignAccess.sendIsBoxed(isBoxed, (TruffleObject) value)) {
        return true;
      }
    }
    return false;
  }

  @Fallback
  protected Object typeError(Object value) {
    throw AbzuException.typeError(this, value);
  }

}
