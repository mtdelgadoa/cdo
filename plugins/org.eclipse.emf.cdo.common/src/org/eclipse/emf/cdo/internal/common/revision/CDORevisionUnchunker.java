/*
 * Copyright (c) 2004-2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.internal.common.revision;

import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;

/**
 * @author Eike Stepper
 * @since 4.2
 */
public interface CDORevisionUnchunker
{
  public void ensureChunks(InternalCDORevision revision, int chunkSize);
}
