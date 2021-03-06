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

package com.google.devtools.j2objc.ast;

import com.google.devtools.j2objc.types.Types;

import org.eclipse.jdt.core.dom.IMethodBinding;

import java.util.List;

/**
 * Node for a super constructor invocation. (i.e. "super(...);")
 */
public class SuperConstructorInvocation extends Statement {

  private IMethodBinding methodBinding = null;
  private ChildList<Expression> arguments = ChildList.create(Expression.class, this);

  public SuperConstructorInvocation(org.eclipse.jdt.core.dom.SuperConstructorInvocation jdtNode) {
    super(jdtNode);
    methodBinding = Types.getMethodBinding(jdtNode);
    for (Object argument : jdtNode.arguments()) {
      arguments.add((Expression) TreeConverter.convert(argument));
    }
  }

  public SuperConstructorInvocation(SuperConstructorInvocation other) {
    super(other);
    methodBinding = other.getMethodBinding();
    arguments.copyFrom(other.getArguments());
  }

  public IMethodBinding getMethodBinding() {
    return methodBinding;
  }

  public List<Expression> getArguments() {
    return arguments;
  }

  @Override
  protected void acceptInner(TreeVisitor visitor) {
    if (visitor.visit(this)) {
      arguments.accept(visitor);
    }
    visitor.endVisit(this);
  }

  @Override
  public SuperConstructorInvocation copy() {
    return new SuperConstructorInvocation(this);
  }
}
