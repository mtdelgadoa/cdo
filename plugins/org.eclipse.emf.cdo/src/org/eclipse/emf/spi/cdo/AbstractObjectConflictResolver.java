/***************************************************************************
 * Copyright (c) 2004 - 2010 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.spi.cdo;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.common.util.CDOException;
import org.eclipse.emf.cdo.spi.common.revision.CDORevisionMerger;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;
import org.eclipse.emf.cdo.transaction.CDOCommitContext;
import org.eclipse.emf.cdo.transaction.CDOConflictResolver2;
import org.eclipse.emf.cdo.transaction.CDODefaultTransactionHandler;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.transaction.CDOTransactionHandler;
import org.eclipse.emf.cdo.util.CDOUtil;
import org.eclipse.emf.cdo.view.CDOAdapterPolicy;
import org.eclipse.emf.cdo.view.CDOViewInvalidationEvent;

import org.eclipse.emf.internal.cdo.CDOObjectMerger;
import org.eclipse.emf.internal.cdo.CDOStateMachine;
import org.eclipse.emf.internal.cdo.bundle.OM;
import org.eclipse.emf.internal.cdo.messages.Messages;

import org.eclipse.net4j.util.collection.Pair;
import org.eclipse.net4j.util.event.IEvent;
import org.eclipse.net4j.util.event.IListener;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Eike Stepper
 * @since 2.0
 */
public abstract class AbstractObjectConflictResolver implements CDOConflictResolver2
{
  private CDOTransaction transaction;

  public AbstractObjectConflictResolver()
  {
  }

  public CDOTransaction getTransaction()
  {
    return transaction;
  }

  public void setTransaction(CDOTransaction transaction)
  {
    if (this.transaction != transaction)
    {
      if (this.transaction != null)
      {
        unhookTransaction(this.transaction);
      }

      this.transaction = transaction;

      if (this.transaction != null)
      {
        hookTransaction(this.transaction);
      }
    }
  }

  public void resolveConflicts(Set<CDOObject> conflicts)
  {
    Map<CDOID, CDORevisionDelta> localDeltas = transaction.getRevisionDeltas();
    for (CDOObject conflict : conflicts)
    {
      CDORevisionDelta localDelta = localDeltas.get(conflict.cdoID());
      resolveConflict(conflict, localDelta);
    }
  }

  /**
   * Resolves the conflict of a single object in the current transaction.
   */
  protected void resolveConflict(CDOObject conflict, CDORevisionDelta localDelta)
  {
    // Do nothing
  }

  /**
   * @since 3.1
   */
  public void resolveConflicts(Map<CDOObject, Pair<CDORevision, CDORevisionDelta>> conflicts,
      List<CDORevisionDelta> allRemoteDeltas)
  {
    Map<CDOID, CDORevisionDelta> localDeltas = transaction.getRevisionDeltas();
    for (Entry<CDOObject, Pair<CDORevision, CDORevisionDelta>> entry : conflicts.entrySet())
    {
      CDOObject conflict = entry.getKey();
      CDORevision oldRevision = entry.getValue().getElement1();
      CDORevisionDelta remoteDelta = entry.getValue().getElement2();
      CDORevisionDelta localDelta = localDeltas.get(conflict.cdoID());
      resolveConflict(conflict, oldRevision, localDelta, remoteDelta, allRemoteDeltas);
    }
  }

  /**
   * Resolves the conflict of a single object in the current transaction. Depending on the decision taken to resolve the
   * conflict, it may be necessary to adjust the notification that will be sent to the adapters in the current
   * transaction. This can be achieved by adjusting the {@link CDORevisionDelta} in <code>deltas</code>.
   * 
   * @since 3.1
   */
  protected void resolveConflict(CDOObject conflict, CDORevision oldRemoteRevision, CDORevisionDelta localDelta,
      CDORevisionDelta remoteDelta, List<CDORevisionDelta> allRemoteDeltas)
  {
    throw new UnsupportedOperationException("Must be overridden");
  }

  protected void hookTransaction(CDOTransaction transaction)
  {
  }

  protected void unhookTransaction(CDOTransaction transaction)
  {
  }

  public static void rollbackObject(CDOObject object)
  {
    CDOStateMachine.INSTANCE.rollback((InternalCDOObject)object);
  }

  public static void readObject(CDOObject object)
  {
    CDOStateMachine.INSTANCE.read((InternalCDOObject)object);
  }

  /**
   * TODO See {@link CDOObjectMerger}!!!
   */
  public static void changeObject(CDOObject object, CDORevisionDelta revisionDelta)
  {
    readObject(object);

    InternalCDORevision revision = (InternalCDORevision)object.cdoRevision().copy();
    ((InternalCDORevisionDelta)revisionDelta).setVersion(revision.getVersion());

    CDORevisionMerger merger = new CDORevisionMerger();
    merger.merge(revision, revisionDelta);
    ((InternalCDOObject)object).cdoInternalSetRevision(revision);
    ((InternalCDOObject)object).cdoInternalSetState(CDOState.DIRTY);
    ((InternalCDOObject)object).cdoInternalPostLoad();
  }

  /**
   * A conflict resolver implementation that takes all the new remote state of the conflicting objects and then applies
   * the locally existing changes of the current transaction.
   * 
   * @author Eike Stepper
   * @since 2.0
   */
  public static class TakeRemoteChangesThenApplyLocalChanges extends AbstractObjectConflictResolver
  {
    public TakeRemoteChangesThenApplyLocalChanges()
    {
    }

    @Override
    public void resolveConflicts(Map<CDOObject, Pair<CDORevision, CDORevisionDelta>> conflicts,
        List<CDORevisionDelta> allRemoteDeltas)
    {
      // Do nothing
    }

    @Override
    protected void resolveConflict(CDOObject conflict, CDORevisionDelta localDelta)
    {
      rollbackObject(conflict);
      changeObject(conflict, localDelta);
    }
  }

  /**
   * @author Eike Stepper
   * @since 2.0
   */
  public static abstract class ThreeWayMerge extends AbstractObjectConflictResolver implements CDOAdapterPolicy
  {
    private CDOTransactionHandler handler = new CDODefaultTransactionHandler()
    {
      @Override
      public void modifyingObject(CDOTransaction transaction, CDOObject object, CDOFeatureDelta ignored)
      {
        if (getTransaction() == transaction)
        {
          adapter.attach(object);
        }
      }

      @Override
      public void committedTransaction(CDOTransaction transaction, CDOCommitContext commitContext)
      {
        if (getTransaction() == transaction)
        {
          adapter.reset();
          collector.reset();
        }
      }

      @Override
      public void rolledBackTransaction(CDOTransaction transaction)
      {
        // Reset the accumulation only if it rolled back the transaction completely
        if (getTransaction() == transaction && transaction.getLastSavepoint().getPreviousSavepoint() == null)
        {
          adapter.reset();
          collector.reset();
        }
      }
    };

    private ChangeSubscriptionAdapter adapter = new ChangeSubscriptionAdapter();

    private RevisionDeltaCollector collector = new RevisionDeltaCollector();

    public ThreeWayMerge()
    {
    }

    public boolean isValid(EObject object, Adapter adapter)
    {
      return adapter instanceof ChangeSubscriptionAdapter;
    }

    @Override
    protected void hookTransaction(CDOTransaction transaction)
    {
      transaction.addTransactionHandler(handler);
      transaction.options().addChangeSubscriptionPolicy(this);
      transaction.addListener(collector);
    }

    @Override
    protected void unhookTransaction(CDOTransaction transaction)
    {
      transaction.removeListener(collector);
      transaction.options().removeChangeSubscriptionPolicy(this);
      transaction.removeTransactionHandler(handler);
    }

    @Override
    public void resolveConflicts(Set<CDOObject> conflicts)
    {
      // Do nothing
    }

    @Override
    protected void resolveConflict(CDOObject conflict, CDORevision oldRemoteRevision, CDORevisionDelta localDelta,
        CDORevisionDelta remoteDelta, List<CDORevisionDelta> allRemoteDeltas)
    {
      resolveConflict(conflict, localDelta, collector.getDeltas(conflict));
    }

    protected abstract void resolveConflict(CDOObject conflict, CDORevisionDelta localDelta,
        List<CDORevisionDelta> remoteDeltas);

    /**
     * @author Eike Stepper
     * @since 2.0
     */
    private static class ChangeSubscriptionAdapter extends AdapterImpl
    {
      private Set<CDOObject> notifiers = new HashSet<CDOObject>();

      public ChangeSubscriptionAdapter()
      {
      }

      public void attach(CDOObject notifier)
      {
        if (notifiers.add(notifier))
        {
          notifier.eAdapters().add(this);
        }
      }

      public void reset()
      {
        for (CDOObject notifier : notifiers)
        {
          notifier.eAdapters().remove(this);
        }

        notifiers.clear();
      }
    }

    /**
     * @author Eike Stepper
     */
    private static class RevisionDeltaCollector implements IListener
    {
      private Map<CDOObject, List<CDORevisionDelta>> deltas = new HashMap<CDOObject, List<CDORevisionDelta>>();

      public RevisionDeltaCollector()
      {
      }

      public List<CDORevisionDelta> getDeltas(CDOObject notifier)
      {
        List<CDORevisionDelta> list = deltas.get(CDOUtil.getEObject(notifier));
        if (list == null)
        {
          return Collections.emptyList();
        }

        return list;
      }

      public void reset()
      {
        deltas.clear();
      }

      public void notifyEvent(IEvent event)
      {
        try
        {
          if (event instanceof CDOViewInvalidationEvent)
          {
            CDOViewInvalidationEvent e = (CDOViewInvalidationEvent)event;
            for (Map.Entry<CDOObject, CDORevisionDelta> entry : e.getRevisionDeltas().entrySet())
            {
              CDOObject notifier = entry.getKey();
              List<CDORevisionDelta> list = deltas.get(notifier);
              if (list == null)
              {
                list = new ArrayList<CDORevisionDelta>();
                deltas.put(notifier, list);
              }

              CDORevisionDelta delta = entry.getValue();
              list.add(delta);
            }
          }
        }
        catch (Exception ex)
        {
          OM.LOG.error(ex);
        }
      }
    }
  }

  /**
   * @author Eike Stepper
   * @since 2.0
   */
  public static class MergeLocalChangesPerFeature extends ThreeWayMerge
  {
    public MergeLocalChangesPerFeature()
    {
    }

    @Override
    protected void resolveConflict(CDOObject conflict, CDORevisionDelta localDelta, List<CDORevisionDelta> remoteDeltas)
    {
      if (hasFeatureConflicts(localDelta, remoteDeltas))
      {
        // TODO localDelta may be corrupt already and the transaction will not be able to restore it!!!
        throw new CDOException(Messages.getString("AbstractObjectConflictResolver.0")); //$NON-NLS-1$
      }

      rollbackObject(conflict);

      // Add remote deltas to local delta
      for (CDORevisionDelta remoteDelta : remoteDeltas)
      {
        for (CDOFeatureDelta remoteFeatureDelta : remoteDelta.getFeatureDeltas())
        {
          // TODO Add public API for this:
          ((InternalCDORevisionDelta)localDelta).addFeatureDelta(remoteFeatureDelta);
        }
      }

      changeObject(conflict, localDelta);
    }

    protected boolean hasFeatureConflicts(CDORevisionDelta localDelta, List<CDORevisionDelta> remoteDeltas)
    {
      Set<EStructuralFeature> features = new HashSet<EStructuralFeature>();
      for (CDOFeatureDelta localFeatureDelta : localDelta.getFeatureDeltas())
      {
        features.add(localFeatureDelta.getFeature());
      }

      for (CDORevisionDelta remoteDelta : remoteDeltas)
      {
        for (CDOFeatureDelta remoteFeatureDelta : remoteDelta.getFeatureDeltas())
        {
          EStructuralFeature feature = remoteFeatureDelta.getFeature();
          if (features.contains(feature))
          {
            return true;
          }
        }
      }

      return false;
    }
  }
}
