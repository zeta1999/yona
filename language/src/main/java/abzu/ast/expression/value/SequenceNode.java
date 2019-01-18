package abzu.ast.expression.value;

import abzu.ast.ExpressionNode;
import abzu.runtime.Sequence;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import java.util.Objects;

@NodeInfo
public final class SequenceNode extends ExpressionNode {
  @Node.Children
  public final ExpressionNode[] expressions;

  public SequenceNode(ExpressionNode[] expressions) {
    this.expressions = expressions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SequenceNode sequenceNode = (SequenceNode) o;
    return Objects.equals(expressions, sequenceNode.expressions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expressions);
  }

  @Override
  public String toString() {
    return "SequenceNode{" +
        "expressions=" + expressions +
        '}';
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return execute(frame);
  }

  @Override
  public Sequence executeSequence(VirtualFrame frame) throws UnexpectedResultException {
    return execute(frame);
  }

  private Sequence execute(VirtualFrame frame) {
    Object[] values = new Object[expressions.length];
    int i = 0;
    for (ExpressionNode expressionNode : expressions) {
      values[i++] = expressionNode.executeGeneric(frame);
    }

    return Sequence.sequence(values);
  }
}
