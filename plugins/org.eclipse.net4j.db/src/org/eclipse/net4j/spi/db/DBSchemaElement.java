/*
 * Copyright (c) 2004 - 2012 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.net4j.spi.db;

import org.eclipse.net4j.db.ddl.IDBSchemaElement;
import org.eclipse.net4j.db.ddl.IDBSchemaVisitor;
import org.eclipse.net4j.db.ddl.IDBSchemaVisitor.StopRecursion;
import org.eclipse.net4j.util.StringUtil;
import org.eclipse.net4j.util.collection.PositionProvider;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Eike Stepper
 * @since 4.2
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class DBSchemaElement extends DBNamedElement implements IDBSchemaElement
{
  private static final long serialVersionUID = 1L;

  private static final IDBSchemaElement[] NO_ELEMENTS = {};

  private transient IDBSchemaElement[] elements;

  public DBSchemaElement(String name)
  {
    super(name);
  }

  /**
   * Constructor for deserialization.
   */
  protected DBSchemaElement()
  {
  }

  public final int compareTo(IDBSchemaElement element2)
  {
    int result = getSchemaElementType().compareTo(element2.getSchemaElementType());
    if (result == 0)
    {
      if (this instanceof PositionProvider && element2 instanceof PositionProvider)
      {
        PositionProvider withPosition1 = (PositionProvider)this;
        PositionProvider withPosition2 = (PositionProvider)element2;
        return withPosition1.getPosition() - withPosition2.getPosition();
      }

      result = getName().compareTo(element2.getName());
    }

    return result;
  }

  public final boolean isEmpty()
  {
    return getElements().length == 0;
  }

  public final IDBSchemaElement[] getElements()
  {
    if (elements == null)
    {
      List<IDBSchemaElement> result = new ArrayList<IDBSchemaElement>();
      collectElements(result);

      if (result.isEmpty())
      {
        elements = NO_ELEMENTS;
      }
      else
      {
        elements = result.toArray(new IDBSchemaElement[result.size()]);
        Arrays.sort(elements);
      }
    }

    return elements;
  }

  protected final void resetElements()
  {
    elements = null;
  }

  protected abstract void collectElements(List<IDBSchemaElement> elements);

  public final <T extends IDBSchemaElement> T getElement(Class<T> type, String name)
  {
    name = name(name);
    for (IDBSchemaElement element : getElements())
    {
      if (element.getName() == name && type.isAssignableFrom(element.getClass()))
      {
        @SuppressWarnings("unchecked")
        T result = (T)element;
        return result;
      }
    }

    return null;
  }

  public final void accept(IDBSchemaVisitor visitor)
  {
    try
    {
      doAccept(visitor);
    }
    catch (StopRecursion ex)
    {
      return;
    }

    for (IDBSchemaElement element : getElements())
    {
      element.accept(visitor);
    }
  }

  protected abstract void doAccept(IDBSchemaVisitor visitor);

  @Override
  public void dump(Writer writer) throws IOException
  {
    SchemaElementType schemaElementType = getSchemaElementType();
    int level = schemaElementType.getLevel();
    for (int i = 0; i < level; i++)
    {
      writer.append("  ");
    }

    writer.append(schemaElementType.toString());
    writer.append(" ");
    writer.append(getName());
    dumpAdditionalProperties(writer);
    writer.append(StringUtil.NL);

    for (IDBSchemaElement element : getElements())
    {
      ((DBSchemaElement)element).dump(writer);
    }
  }

  protected void dumpAdditionalProperties(Writer writer) throws IOException
  {
  }
}
