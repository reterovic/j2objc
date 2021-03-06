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

import java.util.List;

/**
 * Array initializer node type.
 */
public class ArrayInitializer extends Expression {

  private ChildList<Expression> expressions = ChildList.create(Expression.class, this);

  public ArrayInitializer(org.eclipse.jdt.core.dom.ArrayInitializer jdtNode) {
    super(jdtNode);
    for (Object expression : jdtNode.expressions()) {
      expressions.add((Expression) TreeConverter.convert(expression));
    }
  }

  public ArrayInitializer(ArrayInitializer other) {
    super(other);
    expressions.copyFrom(other.getExpressions());
  }

  public List<Expression> getExpressions() {
    return expressions;
  }

  @Override
  protected void acceptInner(TreeVisitor visitor) {
    if (visitor.visit(this)) {
      for (Expression expression : expressions) {
        expression.accept(visitor);
      }
    }
    visitor.endVisit(this);
  }

  @Override
  public ArrayInitializer copy() {
    return new ArrayInitializer(this);
  }
}
