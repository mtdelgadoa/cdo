/**
 * Copyright (c) 2004 - 2011 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Simon McDuff - initial API and implementation
 *    Eike Stepper - maintenance
 *    Simon McDuff - bug 213402
 */
package org.eclipse.emf.cdo.common.revision.delta;

import org.eclipse.emf.cdo.common.id.CDOID;

import org.eclipse.emf.ecore.EReference;

/**
 * @author Simon McDuff
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface CDOContainerFeatureDelta extends CDOFeatureDelta
{
  /**
   * @since 2.0
   */
  public static final EReference CONTAINER_FEATURE = new org.eclipse.emf.cdo.internal.common.revision.delta.CDOContainerFeatureDeltaImpl.ContainerFeature();

  /**
   * @since 2.0
   */
  public CDOID getResourceID();

  /**
   * @since 2.0
   */
  public Object getContainerID();

  public int getContainerFeatureID();
}
