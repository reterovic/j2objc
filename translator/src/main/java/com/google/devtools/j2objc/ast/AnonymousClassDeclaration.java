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
 * Anonymous class declaration node. Must be the child of a
 * ClassInstanceCreation node.
 */
public class AnonymousClassDeclaration extends TreeNode {

  private ChildList<BodyDeclaration> bodyDeclarations =
      ChildList.create(BodyDeclaration.class, this);

  public AnonymousClassDeclaration(AnonymousClassDeclaration other) {
    super(other);
    bodyDeclarations.copyFrom(other.getBodyDeclarations());
  }

  public List<BodyDeclaration> getBodyDeclarations() {
    return bodyDeclarations;
  }

  @Override
  protected void acceptInner(TreeVisitor visitor) {
    if (visitor.visit(this)) {
      bodyDeclarations.accept(visitor);
    }
    visitor.endVisit(this);
  }

  @Override
  public AnonymousClassDeclaration copy() {
    return new AnonymousClassDeclaration(this);
  }
}
