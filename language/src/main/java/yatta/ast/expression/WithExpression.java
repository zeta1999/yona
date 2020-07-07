package yatta.ast.expression;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import yatta.TypesGen;
import yatta.YattaException;
import yatta.YattaLanguage;
import yatta.ast.ExpressionNode;
import yatta.ast.expression.value.ContextLookupNode;
import yatta.ast.expression.value.FunctionNode;
import yatta.runtime.*;
import yatta.runtime.async.Promise;
import yatta.runtime.exceptions.BadArgException;

@NodeInfo(shortName = "with")
public final class WithExpression extends ExpressionNode {
  private final String name;
  @Child
  public ExpressionNode contextExpression;
  @Child
  public FunctionNode bodyExpression;
  @Child
  private InteropLibrary library;
  @Child
  public ExpressionNode resultNode;

  public WithExpression(String name, ExpressionNode contextExpression, FunctionNode bodyExpression) {
    this.name = name;
    this.contextExpression = contextExpression;
    this.bodyExpression = bodyExpression;
    this.library = InteropLibrary.getFactory().createDispatched(3);

    if (this.name != null) {
      resultNode = new LetNode(new NameAliasNode[]{new NameAliasNode(name, new ContextLookupNode(name))}, bodyExpression);
    } else {
      resultNode = bodyExpression;
    }
  }

  @Override
  public void setIsTail(boolean isTail) {
    super.setIsTail(isTail);
    bodyExpression.setIsTail(isTail);
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object contextValue = contextExpression.executeGeneric(frame);
    return executeBodyWithoutIdentifierContext(frame, contextValue);
  }

  private Object executeBodyWithIdentifier(final VirtualFrame frame, final String name, final Function wrapFunction, final Object contextManagerData) {
    Context context = lookupContextReference(YattaLanguage.class).get();

    if (context.containsLocalContext(name)) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      throw new BadArgException("Duplicate context identifier '" + name + "'", this);
    } else {
      return executeResultNode(frame, name, context, wrapFunction, contextManagerData);
    }
  }

  private Object executeResultNode(final VirtualFrame frame, final String name, final Context context, final Function wrapFunction, final Object contextValue) {
    boolean shouldCleanup = true;
    try {
      ContextManager contextManager = new ContextManager<>(name, wrapFunction, contextValue);
      context.putLocalContext(name, contextManager);
      // Execute the body. The result should be a function, or a promise with a function. This function is then passed as an argument to the wrapping function from the ctx manager.
      Object resultValue = resultNode.executeGeneric(frame);

      if (resultValue instanceof Promise) {
        shouldCleanup = false;
        Promise resultPromise = (Promise) resultValue;

        return resultPromise.map(value -> {
            try {
                return library.execute(wrapFunction, contextManager, TypesGen.expectFunction(value));
            } catch (UnsupportedTypeException | UnsupportedMessageException | ArityException | UnexpectedResultException e) {
                throw new YattaException(e, this);
            } finally {
                context.removeLocalContext(name);
            }
        }, exception -> {
          context.removeLocalContext(name);
          return exception;
        }, this);
      } else {
        return library.execute(wrapFunction, contextManager, TypesGen.expectFunction(resultValue));
      }
    } catch (UnsupportedTypeException | UnsupportedMessageException | ArityException | UnexpectedResultException e) {
      throw new YattaException(e, this);
    } finally {
      if (shouldCleanup) {
        context.removeLocalContext(name);
      }
    }
  }

  private Object executeBodyWithoutIdentifierContext(final VirtualFrame frame, final Object contextValue) {
    if (contextValue instanceof ContextManager) {
      ContextManager<?> contextManager = (ContextManager<?>) contextValue;
      return executeBodyWithIdentifier(frame, name != null ? name : contextManager.contextIdentifier().asJavaString(this), contextManager.wrapperFunction(), contextManager.data());
    } else if (contextValue instanceof Tuple) {
      Object contextValueObj = ContextManager.ensureValid((Tuple) contextValue, this);
      return executeBodyWithoutIdentifierContext(frame, contextValueObj);
    } else if (contextValue instanceof Promise) {
      Promise contextValuePromise = (Promise) contextValue;
      CompilerDirectives.transferToInterpreterAndInvalidate();
      MaterializedFrame materializedFrame = frame.materialize();
      return contextValuePromise.map((result) -> executeBodyWithoutIdentifierContext(materializedFrame, result), this);
    } else if (contextValue instanceof Object[]) {
      ContextManager<Object> contextManager = ContextManager.fromItems((Object[]) contextValue, this);
      return executeBodyWithoutIdentifierContext(frame, contextManager);
    } else {
      throw ContextManager.invalidContextException(contextValue, null, this);
    }
  }

  @Override
  protected String[] requiredIdentifiers() {
    return DependencyUtils.catenateRequiredIdentifiers(contextExpression, bodyExpression);
  }
}
