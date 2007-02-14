/***************************************************************************
 * Copyright (c) 2004, 2005, 2006 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.internal.net4j.transport.tcp;

import org.eclipse.net4j.transport.Connector;
import org.eclipse.net4j.transport.tcp.TCPConnectorFactory;

/**
 * @author Eike Stepper
 */
public class TCPConnectorFactoryImpl implements TCPConnectorFactory
{
  public String getID()
  {
    return ID;
  }

  public Connector createConnector()
  {
    ClientTCPConnectorImpl connector = new ClientTCPConnectorImpl();
    return connector;
  }
}
