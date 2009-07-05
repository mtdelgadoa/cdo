/**
 * Copyright (c) 2004 - 2009 Martin Taal and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Martin Taal - initial api
 *    Eike Stepper - maintenance
 */
package org.eclipse.emf.cdo.server.internal.hibernate.tuplizer;

import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDTemp;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.server.hibernate.id.CDOIDHibernate;
import org.eclipse.emf.cdo.server.hibernate.internal.id.CDOIDHibernateFactoryImpl;
import org.eclipse.emf.cdo.server.internal.hibernate.HibernateUtil;

import org.eclipse.net4j.util.WrappedException;

import org.hibernate.Hibernate;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;

/**
 * Persists a CDOID in the DB.
 */
public class CDOIDUserType implements UserType
{
  /**
   * 1) entityname, 2) id, 3) id class name
   */
  private static final int[] SQL_TYPES = { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR };

  /** Constructor by id */
  private final HashMap<String, Constructor<?>> constructors = new HashMap<String, Constructor<?>>();

  public CDOIDUserType()
  {
  }

  public int[] sqlTypes()
  {
    return SQL_TYPES;
  }

  public Class<?> returnedClass()
  {
    return CDOID.class;
  }

  public boolean isMutable()
  {
    return false;
  }

  public Object deepCopy(Object value)
  {
    return value;
  }

  public boolean equals(Object x, Object y)
  {
    if (x == y)
    {
      return true;
    }

    if (x == null || y == null)
    {
      return false;
    }

    return x.equals(y);
  }

  public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws SQLException
  {
    String entityName = (String)Hibernate.STRING.nullSafeGet(rs, names[0]);
    if (rs.wasNull())
    {
      return null;
    }

    String idStr = (String)Hibernate.STRING.nullSafeGet(rs, names[1]);
    if (rs.wasNull())
    {
      return null;
    }

    String idClassName = (String)Hibernate.STRING.nullSafeGet(rs, names[2]);
    if (rs.wasNull())
    {
      return null;
    }

    Serializable id = getId(idStr, idClassName);
    return CDOIDHibernateFactoryImpl.getInstance().createCDOID(id, entityName);
  }

  public void nullSafeSet(PreparedStatement statement, Object value, int index) throws SQLException
  {
    if (value == null || value instanceof CDOID && ((CDOID)value).isNull())
    {
      statement.setNull(index, Types.VARCHAR);
      statement.setNull(index + 1, Types.VARCHAR);
      statement.setNull(index + 2, Types.VARCHAR);
    }

    // try to resolve the temp id
    else if (value instanceof CDOIDTemp)
    {
      final CDORevision revision = HibernateUtil.getInstance().getCDORevisionNullable((CDOID)value);
      if (revision != null)
      {
        value = revision.getID();
      }

      // still a temp one, don't do anything for now
      if (value instanceof CDOIDTemp)
      {
        statement.setNull(index, Types.VARCHAR);
        statement.setNull(index + 1, Types.VARCHAR);
        statement.setNull(index + 2, Types.VARCHAR);
      }
      else
      {
        final CDOIDHibernate id = (CDOIDHibernate)value;
        statement.setString(index, id.getEntityName());
        statement.setString(index + 1, id.getId().toString());
        statement.setString(index + 2, id.getId().getClass().getName());
      }
    }
    else
    {
      CDOIDHibernate id;
      if (value instanceof CDORevision)
      {
        id = HibernateUtil.getInstance().getCDOIDHibernate((CDORevision)value);
      }
      else
      {
        id = (CDOIDHibernate)value;
      }

      statement.setString(index, id.getEntityName());
      statement.setString(index + 1, id.getId().toString());
      statement.setString(index + 2, id.getId().getClass().getName());
    }
  }

  public Serializable disassemble(Object value)
  {
    return (Serializable)value;
  }

  public Object assemble(Serializable cachedValue, Object owner)
  {
    return cachedValue;
  }

  public Object replace(Object original, Object target, Object owner)
  {
    return original;
  }

  public int hashCode(Object x)
  {
    return x.hashCode();
  }

  /** Creates an id object of the correct type */
  private Serializable getId(String idStr, String idType)
  {
    try
    {
      Constructor<?> constructor = constructors.get(idType);
      if (constructor == null)
      {
        Class<?> idClass = this.getClass().getClassLoader().loadClass(idType);
        constructor = idClass.getConstructor(new Class[] { String.class });
        constructors.put(idType, constructor);
      }

      return (Serializable)constructor.newInstance(new Object[] { idStr });
    }
    catch (Exception e)
    {
      throw WrappedException.wrap(e);
    }
  }
}
