/*
 * Copyright (c) 2009-2013, 2015-2017, 2021 Eike Stepper (Loehne, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - bug 230832
 */
package org.eclipse.emf.cdo.internal.net4j.protocol;

import org.eclipse.emf.cdo.common.CDOCommonSession.Options.LockNotificationMode;
import org.eclipse.emf.cdo.common.CDOCommonSession.Options.PassiveUpdateMode;
import org.eclipse.emf.cdo.common.protocol.CDODataInput;
import org.eclipse.emf.cdo.common.protocol.CDODataOutput;
import org.eclipse.emf.cdo.common.protocol.CDOProtocolConstants;

import org.eclipse.net4j.util.om.monitor.OMMonitor;

import org.eclipse.emf.spi.cdo.CDOSessionProtocol.OpenSessionResult;

import java.io.IOException;

/**
 * @author Eike Stepper
 */
public class OpenSessionRequest extends CDOClientRequestWithMonitoring<OpenSessionResult>
{
  private String repositoryName;

  private int sessionID;

  private String userID;

  private boolean passiveUpdateEnabled;

  private PassiveUpdateMode passiveUpdateMode;

  private LockNotificationMode lockNotificationMode;

  private boolean subscribed;

  public OpenSessionRequest(CDOClientProtocol protocol, String repositoryName, int sessionID, String userID, boolean passiveUpdateEnabled,
      PassiveUpdateMode passiveUpdateMode, LockNotificationMode lockNotificationMode, boolean subscribed)
  {
    super(protocol, CDOProtocolConstants.SIGNAL_OPEN_SESSION);
    this.repositoryName = repositoryName;
    this.sessionID = sessionID;
    this.userID = userID;
    this.passiveUpdateEnabled = passiveUpdateEnabled;
    this.passiveUpdateMode = passiveUpdateMode;
    this.lockNotificationMode = lockNotificationMode;
    this.subscribed = subscribed;
  }

  @Override
  protected void requesting(CDODataOutput out, OMMonitor monitor) throws IOException
  {
    out.writeString(repositoryName);
    out.writeXInt(sessionID);
    out.writeString(userID);
    out.writeBoolean(passiveUpdateEnabled);
    out.writeEnum(passiveUpdateMode);
    out.writeEnum(lockNotificationMode);
    out.writeBoolean(subscribed);
  }

  @Override
  protected OpenSessionResult confirming(CDODataInput in, OMMonitor monitor) throws IOException
  {
    int sessionID = in.readXInt();
    if (sessionID == 0)
    {
      // The user has canceled the authentication
      return null;
    }

    return new OpenSessionResult(in, sessionID);
  }
}
