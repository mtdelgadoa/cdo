/*
 * Copyright (c) 2022 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.lm.ui;

import org.eclipse.emf.cdo.lm.internal.client.LMResourceSetConfiguration;
import org.eclipse.emf.cdo.lm.ui.dialogs.SelectModuleResourcesDialog;
import org.eclipse.emf.cdo.ui.CDOLoadResourceProvider;
import org.eclipse.emf.cdo.ui.CDOLoadResourceProvider.ImageProvider;

import org.eclipse.net4j.util.factory.ProductCreationException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eike Stepper
 */
public class LMLoadResourceProvider implements CDOLoadResourceProvider, ImageProvider
{
  public LMLoadResourceProvider()
  {
  }

  @Override
  public String getButtonText(ResourceSet resourceSet)
  {
    return "&Modules...";
  }

  @Override
  public Image getButtonImage(ResourceSet resourceSet)
  {
    return null;
  }

  @Override
  public boolean canHandle(ResourceSet resourceSet)
  {
    LMResourceSetConfiguration configuration = LMResourceSetConfiguration.of(resourceSet);
    return configuration != null;
  }

  @Override
  public List<URI> browseResources(ResourceSet resourceSet, Shell shell, boolean multi)
  {
    LMResourceSetConfiguration configuration = LMResourceSetConfiguration.of(resourceSet);

    SelectModuleResourcesDialog dialog = new SelectModuleResourcesDialog(shell, multi, configuration);
    if (dialog.open() == SelectModuleResourcesDialog.OK)
    {
      return new ArrayList<>(dialog.getURIs());
    }

    return null;
  }

  /**
   * @author Eike Stepper
   */
  public static final class Factory extends CDOLoadResourceProvider.Factory
  {
    public static final String TYPE = "lm";

    public Factory()
    {
      super(TYPE);
    }

    @Override
    public CDOLoadResourceProvider create(String description) throws ProductCreationException
    {
      return new LMLoadResourceProvider();
    }
  }
}
