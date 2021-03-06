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

import com.google.common.base.Preconditions;

/**
 * Tree node for a package declaration.
 */
public class PackageDeclaration extends TreeNode {

  private ChildLink<Name> name = ChildLink.create(Name.class, this);

  public PackageDeclaration(org.eclipse.jdt.core.dom.PackageDeclaration jdtNode) {
    super(jdtNode);
    name.set((Name) TreeConverter.convert(jdtNode.getName()));
  }

  public PackageDeclaration(PackageDeclaration other) {
    super(other);
    name.copyFrom(other.getName());
  }

  public Name getName() {
    return name.get();
  }

  public void setName(Name newName) {
    name.set(newName);
  }

  @Override
  protected void acceptInner(TreeVisitor visitor) {
    if (visitor.visit(this)) {
      name.accept(visitor);
    }
    visitor.endVisit(this);
  }

  @Override
  public PackageDeclaration copy() {
    return new PackageDeclaration(this);
  }

  @Override
  public void validateInner() {
    super.validateInner();
    Preconditions.checkNotNull(name);
  }
}
