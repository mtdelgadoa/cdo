/**
 * Copyright (c) 2004 - 2009 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 *    Simon McDuff - maintenance
 *    Victor Roldan Betancort - maintenance
 *    Gonzague Reydet - bug 298334
 */
package org.eclipse.emf.internal.cdo.transaction;

import org.eclipse.emf.cdo.CDOObject;
import org.eclipse.emf.cdo.CDOState;
import org.eclipse.emf.cdo.common.branch.CDOBranch;
import org.eclipse.emf.cdo.common.branch.CDOBranchManager;
import org.eclipse.emf.cdo.common.branch.CDOBranchPoint;
import org.eclipse.emf.cdo.common.commit.CDOCommit;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.eclipse.emf.cdo.common.id.CDOIDAndVersion;
import org.eclipse.emf.cdo.common.id.CDOIDProvider;
import org.eclipse.emf.cdo.common.id.CDOIDTemp;
import org.eclipse.emf.cdo.common.id.CDOIDUtil;
import org.eclipse.emf.cdo.common.io.CDODataInput;
import org.eclipse.emf.cdo.common.io.CDODataOutput;
import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.common.model.CDOPackageRegistry;
import org.eclipse.emf.cdo.common.model.CDOPackageUnit;
import org.eclipse.emf.cdo.common.model.EMFUtil;
import org.eclipse.emf.cdo.common.revision.CDOListFactory;
import org.eclipse.emf.cdo.common.revision.CDORevision;
import org.eclipse.emf.cdo.common.revision.CDORevisionFactory;
import org.eclipse.emf.cdo.common.revision.delta.CDOFeatureDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDelta;
import org.eclipse.emf.cdo.common.revision.delta.CDORevisionDeltaUtil;
import org.eclipse.emf.cdo.common.util.CDOException;
import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.eresource.CDOResourceFolder;
import org.eclipse.emf.cdo.eresource.CDOResourceNode;
import org.eclipse.emf.cdo.eresource.EresourceFactory;
import org.eclipse.emf.cdo.eresource.impl.CDOResourceImpl;
import org.eclipse.emf.cdo.eresource.impl.CDOResourceNodeImpl;
import org.eclipse.emf.cdo.internal.common.io.CDODataInputImpl;
import org.eclipse.emf.cdo.internal.common.io.CDODataOutputImpl;
import org.eclipse.emf.cdo.spi.common.model.InternalCDOPackageUnit;
import org.eclipse.emf.cdo.spi.common.revision.CDOIDMapper;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevision;
import org.eclipse.emf.cdo.spi.common.revision.InternalCDORevisionDelta;
import org.eclipse.emf.cdo.transaction.CDOConflictResolver;
import org.eclipse.emf.cdo.transaction.CDOSavepoint;
import org.eclipse.emf.cdo.transaction.CDOTransaction;
import org.eclipse.emf.cdo.transaction.CDOTransactionConflictEvent;
import org.eclipse.emf.cdo.transaction.CDOTransactionFinishedEvent;
import org.eclipse.emf.cdo.transaction.CDOTransactionHandler;
import org.eclipse.emf.cdo.transaction.CDOTransactionStartedEvent;
import org.eclipse.emf.cdo.transaction.CDOUserSavepoint;
import org.eclipse.emf.cdo.util.CDOURIUtil;
import org.eclipse.emf.cdo.view.CDOViewResourcesEvent;

import org.eclipse.emf.internal.cdo.CDOObjectMerger;
import org.eclipse.emf.internal.cdo.CDOStateMachine;
import org.eclipse.emf.internal.cdo.bundle.OM;
import org.eclipse.emf.internal.cdo.messages.Messages;
import org.eclipse.emf.internal.cdo.revision.CDOListWithElementProxiesImpl;
import org.eclipse.emf.internal.cdo.util.CompletePackageClosure;
import org.eclipse.emf.internal.cdo.util.FSMUtil;
import org.eclipse.emf.internal.cdo.util.IPackageClosure;
import org.eclipse.emf.internal.cdo.view.CDOViewImpl;

import org.eclipse.net4j.util.ObjectUtil;
import org.eclipse.net4j.util.WrappedException;
import org.eclipse.net4j.util.collection.FastList;
import org.eclipse.net4j.util.event.IListener;
import org.eclipse.net4j.util.io.ExtendedDataInputStream;
import org.eclipse.net4j.util.io.ExtendedDataOutputStream;
import org.eclipse.net4j.util.om.trace.ContextTracer;
import org.eclipse.net4j.util.options.OptionsEvent;
import org.eclipse.net4j.util.transaction.TransactionException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.util.EContentsEList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EContentsEList.FeatureIterator;
import org.eclipse.emf.spi.cdo.CDOTransactionStrategy;
import org.eclipse.emf.spi.cdo.InternalCDOObject;
import org.eclipse.emf.spi.cdo.InternalCDOSavepoint;
import org.eclipse.emf.spi.cdo.InternalCDOSession;
import org.eclipse.emf.spi.cdo.InternalCDOTransaction;
import org.eclipse.emf.spi.cdo.InternalCDOUserSavepoint;
import org.eclipse.emf.spi.cdo.CDOSessionProtocol.CommitTransactionResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Eike Stepper
 */
public class CDOTransactionImpl extends CDOViewImpl implements InternalCDOTransaction
{
  private static final ContextTracer TRACER = new ContextTracer(OM.DEBUG_TRANSACTION, CDOTransactionImpl.class);

  private FastList<CDOTransactionHandler> transactionHandlers = new FastList<CDOTransactionHandler>()
  {
    @Override
    protected CDOTransactionHandler[] newArray(int length)
    {
      return new CDOTransactionHandler[length];
    }
  };

  private InternalCDOSavepoint lastSavepoint = createSavepoint(null);

  private InternalCDOSavepoint firstSavepoint = lastSavepoint;

  private boolean dirty;

  private int conflict;

  private AtomicInteger lastTemporaryID = new AtomicInteger();

  private CDOTransactionStrategy transactionStrategy;

  // Bug 283985 (Re-attachment)
  private WeakHashMap<InternalCDOObject, InternalCDORevision> formerRevisions = new WeakHashMap<InternalCDOObject, InternalCDORevision>();

  // Bug 283985 (Re-attachment)
  private final ThreadLocal<Boolean> providingCDOID = new InheritableThreadLocal<Boolean>()
  {
    @Override
    protected Boolean initialValue()
    {
      return false;
    }
  };

  public CDOTransactionImpl(CDOBranch branch)
  {
    super(branch, UNSPECIFIED_DATE);
  }

  /**
   * @since 2.0
   */
  @Override
  public OptionsImpl options()
  {
    return (OptionsImpl)super.options();
  }

  /**
   * @since 2.0
   */
  @Override
  protected OptionsImpl createOptions()
  {
    return new OptionsImpl();
  }

  @Override
  public boolean isReadOnly()
  {
    return false;
  }

  @Override
  protected boolean setBranchPoint(CDOBranchPoint branchPoint)
  {
    if (branchPoint.getTimeStamp() != UNSPECIFIED_DATE)
  {
      throw new IllegalArgumentException("Changing the target time is not supported by transactions");
  }

    if (isDirty() && !getBranch().equals(branchPoint.getBranch()))
  {
      throw new IllegalStateException("Changing the target branch is impossible while transaction is dirty");
  }

    return super.setBranchPoint(branchPoint);
  }

  public void addTransactionHandler(CDOTransactionHandler handler)
  {
    transactionHandlers.add(handler);
  }

  public void removeTransactionHandler(CDOTransactionHandler handler)
  {
    transactionHandlers.remove(handler);
  }

  public CDOTransactionHandler[] getTransactionHandlers()
  {
    return transactionHandlers.get();
  }

  @Override
  public boolean isDirty()
  {
    if (isClosed())
    {
      return false;
    }

    return dirty;
  }

  @Override
  public boolean hasConflict()
  {
    checkActive();
    return conflict != 0;
  }

  public void setConflict(InternalCDOObject object)
  {
    ConflictEvent event = new ConflictEvent(object, conflict == 0);
    ++conflict;
    IListener[] listeners = getListeners();
    if (listeners != null)
    {
      fireEvent(event, listeners);
    }
  }

  /**
   * @since 2.0
   */
  public Set<CDOObject> getConflicts()
  {
    Set<CDOObject> conflicts = new HashSet<CDOObject>();
    for (CDOObject object : getDirtyObjects().values())
    {
      if (object.cdoConflict())
      {
        conflicts.add(object);
      }
    }

    for (CDOObject object : getDetachedObjects().values())
    {
      if (object.cdoConflict())
      {
        conflicts.add(object);
      }
    }

    return conflicts;
  }

  /**
   * @since 2.0
   */
  public void resolveConflicts(CDOConflictResolver... resolvers)
  {
    Set<CDOObject> conflicts = getConflicts();
    handleConflicts(conflicts, resolvers);
  }

  public void handleConflicts(Set<CDOObject> conflicts)
  {
    handleConflicts(conflicts, options().getConflictResolvers());
  }

  private void handleConflicts(Set<CDOObject> conflicts, CDOConflictResolver[] resolvers)
  {
    if (resolvers.length == 0)
    {
      return;
    }

    // Remember original state to be able to restore it after an exception
    List<CDOState> states = new ArrayList<CDOState>(conflicts.size());
    List<CDORevision> revisions = new ArrayList<CDORevision>(conflicts.size());
    for (CDOObject conflict : conflicts)
    {
      states.add(conflict.cdoState());
      revisions.add(conflict.cdoRevision());
    }

    int resolved = 0;

    try
    {
      Set<CDOObject> remaining = new HashSet<CDOObject>(conflicts);
      for (CDOConflictResolver resolver : resolvers)
      {
        resolver.resolveConflicts(Collections.unmodifiableSet(remaining));
        for (Iterator<CDOObject> it = remaining.iterator(); it.hasNext();)
        {
          CDOObject object = it.next();
          if (!object.cdoConflict())
          {
            ++resolved;
            it.remove();
          }
        }
      }
    }
    catch (Exception ex)
    {
      // Restore original state
      Iterator<CDOState> state = states.iterator();
      Iterator<CDORevision> revision = revisions.iterator();
      for (CDOObject object : conflicts)
      {
        ((InternalCDOObject)object).cdoInternalSetState(state.next());
        ((InternalCDOObject)object).cdoInternalSetRevision(revision.next());
      }

      throw WrappedException.wrap(ex);
    }

    conflict -= resolved;
  }

  @Override
  public void getCDOIDAndVersion(Map<CDOID, CDOIDAndVersion> uniqueObjects, Collection<? extends CDOObject> cdoObjects)
  {
    Map<CDOID, CDORevisionDelta> deltaMap = getRevisionDeltas();
    for (CDOObject cdoObject : cdoObjects)
    {
      CDORevision cdoRevision = CDOStateMachine.INSTANCE.readNoLoad((InternalCDOObject)cdoObject);
      CDOID cdoId = cdoObject.cdoID();
      if (cdoRevision != null && !cdoId.isTemporary() && !uniqueObjects.containsKey(cdoId))
      {
        int version = cdoRevision.getVersion();
        if (deltaMap != null)
        {
          CDORevisionDelta delta = deltaMap.get(cdoId);
          if (delta != null)
          {
            version = delta.getVersion();
          }
        }

        uniqueObjects.put(cdoId, CDOIDUtil.createIDAndVersion(cdoId, version));
      }
    }
  }

  public CDOIDTemp getNextTemporaryID()
  {
    return CDOIDUtil.createTempObject(lastTemporaryID.incrementAndGet());
  }

  /**
   * @since 2.0
   */
  @Override
  protected CDOResourceImpl createRootResource()
  {
    return (CDOResourceImpl)getOrCreateResource(CDOResourceNode.ROOT_PATH);
  }

  public CDOResource createResource(String path)
  {
    checkActive();
    URI uri = CDOURIUtil.createResourceURI(this, path);
    return (CDOResource)getResourceSet().createResource(uri);
  }

  public CDOResource getOrCreateResource(String path)
  {
    checkActive();

    try
    {
      CDOID id = getResourceNodeID(path);
      if (!CDOIDUtil.isNull(id))
      {
        return (CDOResource)getObject(id);
      }
    }
    catch (Exception ignore)
    {
      // Just create the missing resource
    }

    return createResource(path);
  }

  /**
   * @since 2.0
   */
  @Override
  public void attachResource(CDOResourceImpl resource)
  {
    if (resource.isExisting())
    {
      super.attachResource(resource);
    }
    else
    {
      // ResourceSet.createResource(uri) was called!!
      attachNewResource(resource);
    }
  }

  private void attachNewResource(CDOResourceImpl resource)
  {
    URI uri = resource.getURI();
    List<String> names = CDOURIUtil.analyzePath(uri);
    String resourceName = names.isEmpty() ? null : names.remove(names.size() - 1);

    CDOResourceFolder folder = getOrCreateResourceFolder(names);
    attachNewResourceNode(folder, resourceName, resource);
  }

  /**
   * @return never <code>null</code>;
   * @since 2.0
   */
  public CDOResourceFolder getOrCreateResourceFolder(List<String> names)
  {
    CDOResourceFolder folder = null;
    for (String name : names)
    {
      CDOResourceNode node;

      try
      {
        CDOID folderID = folder == null ? null : folder.cdoID();
        node = getResourceNode(folderID, name);
      }
      catch (CDOException ex)
      {
        node = EresourceFactory.eINSTANCE.createCDOResourceFolder();
        attachNewResourceNode(folder, name, node);
      }

      if (node instanceof CDOResourceFolder)
      {
        folder = (CDOResourceFolder)node;
      }
      else
      {
        throw new CDOException(MessageFormat.format(Messages.getString("CDOTransactionImpl.0"), node)); //$NON-NLS-1$
      }
    }

    return folder;
  }

  private void attachNewResourceNode(CDOResourceFolder folder, String name, CDOResourceNode newNode)
  {
    CDOResourceNodeImpl node = (CDOResourceNodeImpl)newNode;
    node.basicSetName(name, false);
    if (folder == null)
    {
      if (node.isRoot())
      {
        CDOStateMachine.INSTANCE.attach(node, this);
      }
      else
      {
        getRootResource().getContents().add(node);
      }
    }
    else
    {
      node.basicSetFolder(folder, false);
    }
  }

  /**
   * @since 2.0
   */
  public void detach(CDOResourceImpl cdoResource)
  {
    CDOStateMachine.INSTANCE.detach(cdoResource);
    IListener[] listeners = getListeners();
    if (listeners != null)
    {
      fireEvent(new ResourcesEvent(cdoResource.getPath(), ResourcesEvent.Kind.REMOVED), listeners);
    }
  }

  /**
   * @since 2.0
   */
  public InternalCDOSavepoint getLastSavepoint()
  {
    checkActive();
    return lastSavepoint;
  }

  /**
   * @since 2.0
   */
  public CDOTransactionStrategy getTransactionStrategy()
  {
    if (transactionStrategy == null)
    {
      transactionStrategy = CDOTransactionStrategy.DEFAULT;
      transactionStrategy.setTarget(this);
    }

    return transactionStrategy;
  }

  /**
   * @since 2.0
   */
  public void setTransactionStrategy(CDOTransactionStrategy transactionStrategy)
  {
    if (this.transactionStrategy != null)
    {
      this.transactionStrategy.unsetTarget(this);
    }

    this.transactionStrategy = transactionStrategy;
    if (this.transactionStrategy != null)
    {
      this.transactionStrategy.setTarget(this);
    }
  }

  /**
   * @since 2.0
   */
  @Override
  protected CDOID getRootOrTopLevelResourceNodeID(String name)
  {
    if (dirty)
    {
      CDOResourceNode node = getRootResourceNode(name, getDirtyObjects().values());
      if (node != null)
      {
        return node.cdoID();
      }

      node = getRootResourceNode(name, getNewObjects().values());
      if (node != null)
      {
        return node.cdoID();
      }

      node = getRootResourceNode(name, getNewResources().values());
      if (node != null)
      {
        return node.cdoID();
      }
    }

    CDOID id = super.getRootOrTopLevelResourceNodeID(name);
    if (getLastSavepoint().getAllDetachedObjects().containsKey(id) || getDirtyObjects().containsKey(id))
    {
      throw new CDOException(MessageFormat.format(Messages.getString("CDOTransactionImpl.1"), name)); //$NON-NLS-1$
    }

    return id;
  }

  private CDOResourceNode getRootResourceNode(String name, Collection<? extends CDOObject> objects)
  {
    for (CDOObject object : objects)
    {
      if (object instanceof CDOResourceNode)
      {
        CDOResourceNode node = (CDOResourceNode)object;
        if (node.getFolder() == null && ObjectUtil.equals(name, node.getName()))
        {
          return node;
        }
      }
    }

    return null;
  }

  // TTT Map<InternalEObject, CDOIDDanglingImpl> danglingObjects = new
  // HashMap<InternalEObject, CDOIDDanglingImpl>();
  //
  // @Override
  // public CDOIDDangling convertDanglingObjectToID(InternalCDOObject source,
  // EStructuralFeature feature,
  // InternalEObject target)
  // {
  // CDOIDDanglingImpl id;
  // synchronized (danglingObjects)
  // {
  // id = danglingObjects.get(target);
  // if (id == null)
  // {
  // id = new CDOIDDanglingImpl(lastTemporaryID.incrementAndGet(), target);
  // danglingObjects.put(target, id);
  // }
  // }
  //
  // id.addReference(source, feature);
  // return id;
  // }

  /**
   * @since 2.0
   */
  @Override
  public InternalCDOObject getObject(CDOID id, boolean loadOnDemand)
  {
    checkActive();
    if (CDOIDUtil.isNull(id))
    {
      return null;
    }

    if (id.isTemporary() && isDetached(id))
    {
      FSMUtil.validate(id, null);
    }

    return super.getObject(id, loadOnDemand);
  }

  private boolean isDetached(CDOID id)
  {
    return lastSavepoint.getSharedDetachedObjects().contains(id);
  }

  /**
   * @since 2.0
   */
  public InternalCDOCommitContext createCommitContext()
  {
    return new CDOCommitContextImpl();
  }

  /**
   * @since 2.0
   */
  public CDOCommit commit(IProgressMonitor progressMonitor) throws TransactionException
  {
    checkActive();
    synchronized (getSession().getInvalidationLock())
    {
      getLock().lock();

      try
      {
        if (hasConflict())
        {
          throw new TransactionException(Messages.getString("CDOTransactionImpl.2")); //$NON-NLS-1$
        }

        if (progressMonitor == null)
        {
          progressMonitor = new NullProgressMonitor();
        }

        return getTransactionStrategy().commit(this, progressMonitor);
      }
      catch (TransactionException ex)
      {
        throw ex;
      }
      catch (Exception ex)
      {
        throw new TransactionException(ex);
      }
      finally
      {
        getLock().unlock();
      }
    }
  }

  public CDOCommit commit() throws TransactionException
  {
    return commit(null);
  }

  /**
   * @since 2.0
   */
  public void rollback()
  {
    checkActive();
    rollback(firstSavepoint);
    cleanUp();
  }

  private void removeObjects(Collection<? extends CDOObject> objects)
  {
    if (!objects.isEmpty())
    {
      for (CDOObject object : objects)
      {
        ((InternalCDOObject)object).cdoInternalSetState(CDOState.TRANSIENT);
        removeObject(object.cdoID());

        if (object instanceof CDOResource)
        {
          getResourceSet().getResources().remove(object);
        }

        ((InternalCDOObject)object).cdoInternalSetID(null);
        ((InternalCDOObject)object).cdoInternalSetRevision(null);
        ((InternalCDOObject)object).cdoInternalSetView(null);
      }
    }
  }

  private Set<CDOID> rollbackCompletely(CDOUserSavepoint savepoint)
  {
    Set<CDOID> idsOfNewObjectWithDeltas = new HashSet<CDOID>();

    // Start from the last savepoint and come back up to the active
    for (InternalCDOSavepoint itrSavepoint = lastSavepoint; itrSavepoint != null; itrSavepoint = itrSavepoint
        .getPreviousSavepoint())
    {
      // Rollback new objects created after the save point
      removeObjects(itrSavepoint.getNewResources().values());
      removeObjects(itrSavepoint.getNewObjects().values());

      // Bug 283985 (Re-attachment): Objects that were reattached must
      // also be removed
      Collection<CDOObject> reattachedObjects = itrSavepoint.getReattachedObjects().values();
      removeObjects(reattachedObjects);

      Map<CDOID, CDORevisionDelta> revisionDeltas = itrSavepoint.getRevisionDeltas();
      if (!revisionDeltas.isEmpty())
      {
        for (CDORevisionDelta dirtyObject : revisionDeltas.values())
        {
          if (dirtyObject.getID().isTemporary())
          {
            idsOfNewObjectWithDeltas.add(dirtyObject.getID());
          }
        }
      }

      // Rollback all persisted objects
      Map<CDOID, CDOObject> detachedObjects = itrSavepoint.getDetachedObjects();
      if (!detachedObjects.isEmpty())
      {
        for (Entry<CDOID, CDOObject> entryDirty : detachedObjects.entrySet())
        {
          if (entryDirty.getKey().isTemporary())
          {
            idsOfNewObjectWithDeltas.add(entryDirty.getKey());
          }
          else
          {
            InternalCDOObject internalDirtyObject = (InternalCDOObject)entryDirty.getValue();
            cleanObject(internalDirtyObject, getRevision(entryDirty.getKey(), true));
          }
        }
      }

      for (Entry<CDOID, CDOObject> entryDirtyObject : itrSavepoint.getDirtyObjects().entrySet())
      {
        // Rollback all persisted objects
        if (!entryDirtyObject.getKey().isTemporary())
        {
          InternalCDOObject internalDirtyObject = (InternalCDOObject)entryDirtyObject.getValue();

          // Bug 283985 (Re-attachment): Skip objects that were
          // reattached, because
          // they were already reset to TRANSIENT earlier in this
          // method
          if (!reattachedObjects.contains(internalDirtyObject))
          {
            CDOStateMachine.INSTANCE.rollback(internalDirtyObject);
          }
        }
      }

      if (savepoint == itrSavepoint)
      {
        break;
      }
    }

    return idsOfNewObjectWithDeltas;
  }

  private void loadSavepoint(CDOSavepoint savepoint, Set<CDOID> idsOfNewObjectWithDeltas)
  {
    lastSavepoint.recalculateSharedDetachedObjects();

    Map<CDOID, CDOObject> dirtyObjects = getDirtyObjects();
    Map<CDOID, CDOObject> newObjMaps = getNewObjects();
    Map<CDOID, CDOResource> newResources = getNewResources();
    Map<CDOID, CDORevision> newBaseRevision = getBaseNewObjects();
    Map<CDOID, CDOObject> detachedObjects = getDetachedObjects();

    // Reload the objects (NEW) with their base.
    for (CDOID id : idsOfNewObjectWithDeltas)
    {
      if (detachedObjects.containsKey(id))
      {
        continue;
      }

      InternalCDOObject object = (InternalCDOObject)newObjMaps.get(id);
      if (object == null)
      {
        object = (InternalCDOObject)newResources.get(id);
      }

      CDORevision revision = newBaseRevision.get(id);
      if (revision != null)
      {
        object.cdoInternalSetRevision(revision.copy());
        object.cdoInternalSetView(this);
        object.cdoInternalSetID(revision.getID());
        object.cdoInternalSetState(CDOState.NEW);

        // Load the object from revision to EObject
        object.cdoInternalPostLoad();
      }
    }

    // We need to register back new objects that are not removed anymore
    // there.
    for (Entry<CDOID, CDOObject> entryNewObject : newObjMaps.entrySet())
    {
      InternalCDOObject object = (InternalCDOObject)entryNewObject.getValue();

      // Go back to the previous state
      cleanObject(object, object.cdoRevision());
      object.cdoInternalSetState(CDOState.NEW);
    }

    for (Entry<CDOID, CDOObject> entryDirtyObject : dirtyObjects.entrySet())
    {
      if (detachedObjects.containsKey(entryDirtyObject.getKey()))
      {
        continue;
      }

      // Rollback every persisted objects
      InternalCDOObject internalDirtyObject = (InternalCDOObject)entryDirtyObject.getValue();
      cleanObject(internalDirtyObject, getRevision(entryDirtyObject.getKey(), true));
    }

    for (InternalCDOSavepoint itrSavepoint = firstSavepoint; itrSavepoint != savepoint; itrSavepoint = itrSavepoint
        .getNextSavepoint())
    {
      CDOObjectMerger merger = new CDOObjectMerger();
      for (CDORevisionDelta delta : itrSavepoint.getRevisionDeltas().values())
      {
        if (delta.getID().isTemporary() && !idsOfNewObjectWithDeltas.contains(delta.getID())
            || detachedObjects.containsKey(delta.getID()))
        {
          continue;
        }

        Map<CDOID, CDOObject> map = delta.getID().isTemporary() ? newObjMaps : dirtyObjects;
        InternalCDOObject object = (InternalCDOObject)map.get(delta.getID());
        if (object == null)
        {
          object = (InternalCDOObject)newResources.get(delta.getID());
        }

        // Change state of the objects
        merger.merge(object, delta);

        // Load the object from revision to EObject
        object.cdoInternalPostLoad();
      }
    }

    dirty = savepoint.wasDirty();
  }

  /**
   * @since 2.0
   */
  public void detachObject(InternalCDOObject object)
  {
    CDOTransactionHandler[] handlers = getTransactionHandlers();
    if (handlers != null)
    {
      for (int i = 0; i < handlers.length; i++)
      {
        CDOTransactionHandler handler = handlers[i];
        handler.detachingObject(this, object);
      }
    }

    // deregister object
    if (object.cdoState() == CDOState.NEW)
    {
      Map<CDOID, ? extends CDOObject> map = object instanceof CDOResource ? getLastSavepoint().getNewResources()
          : getLastSavepoint().getNewObjects();

      // Determine if we added object
      if (map.containsKey(object.cdoID()))
      {
        // deregister object
        deregisterObject(object);
        map.remove(object.cdoID());
      }
      else
      {
        getLastSavepoint().getDetachedObjects().put(object.cdoID(), object);
      }
    }
    else
    {
      getLastSavepoint().getDetachedObjects().put(object.cdoID(), object);

      if (!formerRevisions.containsKey(object))
      {
        formerRevisions.put(object, object.cdoRevision());
      }

      // Object may have been reattached previously, in which case it must
      // here be removed from the collection of reattached objects
      lastSavepoint.getReattachedObjects().remove(object.cdoID());
    }

    if (!dirty)
    {
      dirty = true;
      IListener[] listeners = getListeners();
      if (listeners != null)
      {
        fireEvent(new StartedEvent(), listeners);
      }
    }
  }

  /**
   * @since 2.0
   */
  public void rollback(CDOUserSavepoint savepoint)
  {
    checkActive();
    getTransactionStrategy().rollback(this, (InternalCDOUserSavepoint)savepoint);
  }

  /**
   * @since 2.0
   */
  public void handleRollback(InternalCDOSavepoint savepoint)
  {
    if (savepoint == null)
    {
      throw new IllegalArgumentException(Messages.getString("CDOTransactionImpl.3")); //$NON-NLS-1$
    }

    if (savepoint.getTransaction() != this)
    {
      throw new IllegalArgumentException(MessageFormat.format(Messages.getString("CDOTransactionImpl.4"), savepoint)); //$NON-NLS-1$
    }

    if (TRACER.isEnabled())
    {
      TRACER.trace("handleRollback()"); //$NON-NLS-1$
    }

    try
    {
      if (!savepoint.isValid())
      {
        throw new IllegalArgumentException(MessageFormat.format(Messages.getString("CDOTransactionImpl.6"), savepoint)); //$NON-NLS-1$
      }

      // Use the state lock since rollback mechanism is playing with
      // EObject and CDORevisions. We do not want to
      // receives notifications during that process! neither the user
      // should callload any objects.
      ReentrantLock viewLock = getStateLock();
      viewLock.lock();

      try
      {
        // Rollback objects
        Set<CDOID> idsOfNewObjectWithDeltas = rollbackCompletely(savepoint);

        lastSavepoint = savepoint;
        // Make savepoint active. Erase savepoint that could have be
        // after
        lastSavepoint.setNextSavepoint(null);
        lastSavepoint.clear();

        // Load from first savepoint up to current savepoint
        loadSavepoint(lastSavepoint, idsOfNewObjectWithDeltas);

        if (lastSavepoint == firstSavepoint && options().isAutoReleaseLocksEnabled())
        {
          // Unlock all objects
          unlockObjects(null, null);
        }
      }
      finally
      {
        viewLock.unlock();
      }

      Map<CDOIDTemp, CDOID> idMappings = Collections.emptyMap();
      IListener[] listeners = getListeners();
      if (listeners != null)
      {
        fireEvent(new FinishedEvent(CDOTransactionFinishedEvent.Type.ROLLED_BACK, idMappings), listeners);
      }

      CDOTransactionHandler[] handlers = getTransactionHandlers();
      if (handlers != null)
      {
        for (int i = 0; i < handlers.length; i++)
        {
          CDOTransactionHandler handler = handlers[i];

          try
          {
            handler.rolledBackTransaction(this);
          }
          catch (RuntimeException ex)
          {
            OM.LOG.error(ex);
          }
        }
      }
    }
    catch (RuntimeException ex)
    {
      throw ex;
    }
    catch (Exception ex)
    {
      throw new TransactionException(ex);
    }
  }

  /**
   * @since 2.0
   */
  public InternalCDOSavepoint handleSetSavepoint()
  {
    addToBase(lastSavepoint.getNewObjects());
    addToBase(lastSavepoint.getNewResources());

    lastSavepoint = createSavepoint(lastSavepoint);
    return lastSavepoint;
  }

  protected CDOSavepointImpl createSavepoint(InternalCDOSavepoint lastSavepoint)
  {
    return new CDOSavepointImpl(this, lastSavepoint);
  }

  /**
   * @since 2.0
   */
  public InternalCDOSavepoint setSavepoint()
  {
    checkActive();
    return (InternalCDOSavepoint)getTransactionStrategy().setSavepoint(this);
  }

  private void addToBase(Map<CDOID, ? extends CDOObject> objects)
  {
    for (CDOObject object : objects.values())
    {
      // Load instance to revision
      ((InternalCDOObject)object).cdoInternalPreCommit();
      lastSavepoint.getBaseNewObjects().put(object.cdoID(), object.cdoRevision().copy());
    }
  }

  @Override
  public String toString()
  {
    return MessageFormat.format("CDOTransaction({0})", getViewID()); //$NON-NLS-1$
  }

  public void registerNew(InternalCDOObject object)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Registering new object {0}", object); //$NON-NLS-1$
    }

    registerNewPackage(object.eClass().getEPackage());

    CDOTransactionHandler[] handlers = getTransactionHandlers();
    if (handlers != null)
    {
      for (int i = 0; i < handlers.length; i++)
      {
        CDOTransactionHandler handler = handlers[i];
        handler.attachingObject(this, object);
      }
    }

    if (object instanceof CDOResourceImpl)
    {
      registerNew(lastSavepoint.getNewResources(), object);
    }
    else
    {
      registerNew(lastSavepoint.getNewObjects(), object);
    }
  }

  private void registerNewPackage(EPackage ePackage)
  {
    CDOPackageRegistry packageRegistry = getSession().getPackageRegistry();
    if (!packageRegistry.containsKey(ePackage.getNsURI()))
    {
      packageRegistry.putEPackage(ePackage);
    }
  }

  /**
   * Receives notification for new and dirty objects
   */
  public void registerFeatureDelta(InternalCDOObject object, CDOFeatureDelta featureDelta)
  {
    boolean needToSaveFeatureDelta = true;
    if (object.cdoState() == CDOState.NEW)
    {
      // Register Delta for new objects only if objectA doesn't belong to
      // this savepoint
      if (getLastSavepoint().getPreviousSavepoint() == null || featureDelta == null)
      {
        needToSaveFeatureDelta = false;
      }
      else
      {
        Map<CDOID, ? extends CDOObject> map = object instanceof CDOResource ? getLastSavepoint().getNewResources()
            : getLastSavepoint().getNewObjects();
        needToSaveFeatureDelta = !map.containsKey(object.cdoID());
      }
    }

    if (needToSaveFeatureDelta)
    {
      CDORevisionDelta revisionDelta = lastSavepoint.getRevisionDeltas().get(object.cdoID());
      if (revisionDelta == null)
      {
        revisionDelta = CDORevisionDeltaUtil.create(object.cdoRevision());
        lastSavepoint.getRevisionDeltas().put(object.cdoID(), revisionDelta);
      }

      ((InternalCDORevisionDelta)revisionDelta).addFeatureDelta(featureDelta);
    }

    CDOTransactionHandler[] handlers = getTransactionHandlers();
    if (handlers != null)
    {
      for (int i = 0; i < handlers.length; i++)
      {
        CDOTransactionHandler handler = handlers[i];
        handler.modifyingObject(this, object, featureDelta);
      }
    }
  }

  public void registerRevisionDelta(CDORevisionDelta revisionDelta)
  {
    lastSavepoint.getRevisionDeltas().putIfAbsent(revisionDelta.getID(), revisionDelta);
  }

  public void registerDirty(InternalCDOObject object, CDOFeatureDelta featureDelta)
  {
    if (TRACER.isEnabled())
    {
      TRACER.format("Registering dirty object {0}", object); //$NON-NLS-1$
    }

    if (featureDelta != null)
    {
      registerFeatureDelta(object, featureDelta);
    }

    registerNew(lastSavepoint.getDirtyObjects(), object);
  }

  /**
   * TODO Simon: Should this method go to CDOSavePointImpl?
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void registerNew(Map map, InternalCDOObject object)
  {
    Object old = map.put(object.cdoID(), object);
    if (old != null)
    {
      throw new IllegalStateException(MessageFormat.format(Messages.getString("CDOTransactionImpl.10"), object)); //$NON-NLS-1$
    }

    if (!dirty)
    {
      dirty = true;
      IListener[] listeners = getListeners();
      if (listeners != null)
      {
        fireEvent(new StartedEvent(), listeners);
      }
    }
  }

  private List<CDOPackageUnit> analyzeNewPackages()
  {
    CDOPackageRegistry packageRegistry = getSession().getPackageRegistry();
    Set<EPackage> usedPackages = new HashSet<EPackage>();
    Set<EPackage> usedNewPackages = new HashSet<EPackage>();
    for (CDOObject object : getNewObjects().values())
    {
      EPackage ePackage = object.eClass().getEPackage();
      if (usedPackages.add(ePackage))
      {
        EPackage topLevelPackage = EMFUtil.getTopLevelPackage(ePackage);
        if (ePackage == topLevelPackage || usedPackages.add(topLevelPackage))
        {
          if (!CDOModelUtil.isSystemPackage(topLevelPackage))
          {
            CDOPackageUnit packageUnit = packageRegistry.getPackageUnit(topLevelPackage);
            if (packageUnit.getState() == CDOPackageUnit.State.NEW)
            {
              usedNewPackages.add(topLevelPackage);
            }
          }
        }
      }
    }

    if (usedNewPackages.size() > 0)
    {
      Set<CDOPackageUnit> result = new HashSet<CDOPackageUnit>();
      for (EPackage usedNewPackage : analyzeNewPackages(usedNewPackages, packageRegistry))
      {
        CDOPackageUnit packageUnit = packageRegistry.getPackageUnit(usedNewPackage);
        result.add(packageUnit);
      }

      return new ArrayList<CDOPackageUnit>(result);
    }

    return Collections.emptyList();
  }

  private static List<EPackage> analyzeNewPackages(Collection<EPackage> usedTopLevelPackages,
      CDOPackageRegistry packageRegistry)
  {
    // Determine which of the corresdonding EPackages are new
    List<EPackage> newPackages = new ArrayList<EPackage>();

    IPackageClosure closure = new CompletePackageClosure();
    usedTopLevelPackages = closure.calculate(usedTopLevelPackages);

    for (EPackage usedPackage : usedTopLevelPackages)
    {
      if (!CDOModelUtil.isSystemPackage(usedPackage))
      {
        CDOPackageUnit packageUnit = packageRegistry.getPackageUnit(usedPackage);
        if (packageUnit == null)
        {
          throw new CDOException(MessageFormat.format(Messages.getString("CDOTransactionImpl.11"), usedPackage)); //$NON-NLS-1$
        }

        if (packageUnit.getState() == CDOPackageUnit.State.NEW)
        {
          newPackages.add(usedPackage);
        }
      }
    }

    return newPackages;
  }

  private void cleanUp()
  {
    lastSavepoint = firstSavepoint;
    firstSavepoint.clear();
    firstSavepoint.setNextSavepoint(null);
    firstSavepoint.getSharedDetachedObjects().clear();

    // Bug 283985 (Re-attachment)
    formerRevisions.clear();

    dirty = false;
    conflict = 0;
    lastTemporaryID.set(0);
  }

  public CDOSavepoint[] exportChanges(OutputStream stream) throws IOException
  {
    CDODataOutput out = new CDODataOutputImpl(new ExtendedDataOutputStream(stream))
    {
      public CDOIDProvider getIDProvider()
      {
        return CDOTransactionImpl.this;
      }

      public CDOPackageRegistry getPackageRegistry()
      {
        return getSession().getPackageRegistry();
      }
    };

    List<CDOSavepoint> savepoints = new ArrayList<CDOSavepoint>();
    InternalCDOSavepoint savepoint = firstSavepoint;
    while (savepoint != null)
    {
      Collection<CDOResource> newResources = savepoint.getNewResources().values();
      Collection<CDOObject> newObjects = savepoint.getNewObjects().values();
      Collection<CDORevisionDelta> revisionDeltas = savepoint.getRevisionDeltas().values();
      if (newResources.isEmpty() && newObjects.isEmpty() && revisionDeltas.isEmpty())
      {
        savepoint = savepoint.getNextSavepoint();
        continue;
      }

      savepoints.add(savepoint);
      out.writeBoolean(true);

      out.writeInt(newResources.size() + newObjects.size());
      for (CDOResource newResource : newResources)
      {
        out.writeCDORevision(newResource.cdoRevision(), CDORevision.UNCHUNKED);
      }

      for (CDOObject newObject : newObjects)
      {
        out.writeCDORevision(newObject.cdoRevision(), CDORevision.UNCHUNKED);
      }

      out.writeInt(revisionDeltas.size());
      for (CDORevisionDelta revisionDelta : revisionDeltas)
      {
        out.writeCDORevisionDelta(revisionDelta);
      }

      savepoint = savepoint.getNextSavepoint();
    }

    out.writeBoolean(false);
    return savepoints.toArray(new CDOSavepoint[savepoints.size()]);
  }

  public CDOSavepoint[] importChanges(InputStream stream, boolean reconstructSavepoints) throws IOException
  {
    List<CDOSavepoint> savepoints = new ArrayList<CDOSavepoint>();
    if (stream.available() > 0)
    {
      CDODataInput in = new CDODataInputImpl(new ExtendedDataInputStream(stream))
      {
        @Override
        protected CDOPackageRegistry getPackageRegistry()
        {
          return getSession().getPackageRegistry();
        }

        @Override
        protected CDOBranchManager getBranchManager()
        {
          return getSession().getBranchManager();
        }

        @Override
        protected CDORevisionFactory getRevisionFactory()
        {
          return getSession().getRevisionManager().getFactory();
        }

        @Override
        protected CDOListFactory getListFactory()
        {
          return CDOListWithElementProxiesImpl.FACTORY;
        }
      };

      Map<CDOIDTemp, CDOID> idMappings = new HashMap<CDOIDTemp, CDOID>();
      while (in.readBoolean())
      {
        if (reconstructSavepoints)
        {
          InternalCDOSavepoint savepoint = setSavepoint();
          savepoints.add(savepoint);
        }

        // Import revisions and deltas
        List<InternalCDORevision> revisions = new ArrayList<InternalCDORevision>();
        importNewRevisions(in, revisions, idMappings);
        List<InternalCDORevisionDelta> revisionDeltas = importRevisionDeltas(in);

        // Re-map temp IDs
        CDOIDMapper idMapper = new CDOIDMapper(idMappings);
        for (InternalCDORevision revision : revisions)
        {
          revision.adjustReferences(idMapper);
        }

        for (InternalCDORevisionDelta delta : revisionDeltas)
        {
          delta.adjustReferences(idMapper);
        }

        // Create new objects
        for (InternalCDORevision revision : revisions)
        {
          InternalCDOObject object = newInstance(revision);
          registerObject(object);
          registerNew(object);
        }

        // Apply deltas
        CDOObjectMerger merger = new CDOObjectMerger();
        for (InternalCDORevisionDelta delta : revisionDeltas)
        {
          InternalCDOObject object = getObject(delta.getID());
          CDORevision revision = object.cdoRevision().copy();
          merger.merge(object, delta);
          registerRevisionDelta(delta);
          registerDirty(object, null);

          if (delta.getVersion() < revision.getVersion())
          {
            setConflict(object);
          }
        }
      }
    }

    return savepoints.toArray(new CDOSavepoint[savepoints.size()]);
  }

  private void importNewRevisions(CDODataInput in, List<InternalCDORevision> revisions, Map<CDOIDTemp, CDOID> idMappings)
      throws IOException
  {
    int size = in.readInt();
    for (int i = 0; i < size; i++)
    {
      InternalCDORevision revision = (InternalCDORevision)in.readCDORevision();

      CDOIDTemp oldID = (CDOIDTemp)revision.getID();
      CDOIDTemp newID = getNextTemporaryID();
      idMappings.put(oldID, newID);
      revision.setID(newID);

      revisions.add(revision);
    }
  }

  private List<InternalCDORevisionDelta> importRevisionDeltas(CDODataInput in) throws IOException
  {
    int size = in.readInt();
    List<InternalCDORevisionDelta> deltas = new ArrayList<InternalCDORevisionDelta>(size);
    for (int i = 0; i < size; i++)
    {
      InternalCDORevisionDelta delta = (InternalCDORevisionDelta)in.readCDORevisionDelta();
      deltas.add(delta);
    }

    return deltas;
  }

  private InternalCDOObject newInstance(InternalCDORevision revision)
  {
    InternalCDOObject object = newInstance(revision.getEClass());
    object.cdoInternalSetID(revision.getID());
    object.cdoInternalSetRevision(revision);
    object.cdoInternalSetState(CDOState.NEW);
    object.cdoInternalSetView(this);
    return object;
  }

  public Map<CDOID, CDOObject> getDirtyObjects()
  {
    checkActive();
    return lastSavepoint.getAllDirtyObjects();
  }

  public Map<CDOID, CDOObject> getNewObjects()
  {
    checkActive();
    return lastSavepoint.getAllNewObjects();
  }

  public Map<CDOID, CDOResource> getNewResources()
  {
    checkActive();
    return lastSavepoint.getAllNewResources();
  }

  /**
   * @since 2.0
   */
  public Map<CDOID, CDORevision> getBaseNewObjects()
  {
    checkActive();
    return lastSavepoint.getAllBaseNewObjects();
  }

  public Map<CDOID, CDORevisionDelta> getRevisionDeltas()
  {
    checkActive();
    return lastSavepoint.getAllRevisionDeltas();
  }

  /**
   * @since 2.0
   */
  public Map<CDOID, CDOObject> getDetachedObjects()
  {
    checkActive();
    return lastSavepoint.getAllDetachedObjects();
  }

  public Map<InternalCDOObject, InternalCDORevision> getFormerRevisions()
  {
    return formerRevisions;
  }

  @Override
  protected CDOID getID(InternalCDOObject object, boolean onlyPersistedID)
  {
    CDOID id = super.getID(object, onlyPersistedID);

    // The super implementation will return null for a transient (unattached) object;
    // but in a tx, an transient object may previously have been attached, so we consult
    // the formerRevisions -- unless this is being called indirectly through provideCDOID.
    // The latter case occurs when deltas or revisions are being written out to a stream; in
    // which case null must be returned (for transients) so that the caller will detect a
    // dangling reference
    if (!providingCDOID.get().booleanValue() && id == null)
    {
      CDORevision formerRevision = formerRevisions.get(object);
      if (formerRevision != null)
      {
        id = formerRevision.getID();
      }
    }

    return id;
  }

  @Override
  public CDOID provideCDOID(Object idOrObject)
  {
    try
    {
      providingCDOID.set(true);
      return super.provideCDOID(idOrObject);
    }
    finally
    {
      providingCDOID.set(false);
    }
  }

  /**
   * @since 2.0
   */
  @Override
  protected void doDeactivate() throws Exception
  {
    options().disposeConflictResolvers();
    lastSavepoint = null;
    firstSavepoint = null;
    transactionStrategy = null;
    super.doDeactivate();
  }

  /**
   * @since 3.0
   */
  @Override
  public Set<CDOObject> handleInvalidationWithoutNotification(Set<CDOIDAndVersion> dirtyOIDs,
      Collection<CDOID> detachedOIDs, Set<InternalCDOObject> dirtyObjects, Set<InternalCDOObject> detachedObjects,
      boolean async)
  {
    // Bugzilla 298561: This override removes references to remotely
    // detached objects that are present in any DIRTY or NEW objects
    removeCrossReferences(getDirtyObjects().values(), detachedOIDs);
    removeCrossReferences(getNewObjects().values(), detachedOIDs);

    return super.handleInvalidationWithoutNotification(dirtyOIDs, detachedOIDs, dirtyObjects, detachedObjects, async);
  }

  private void removeCrossReferences(Collection<CDOObject> objects, Collection<CDOID> referencedOIDs)
  {
    for (CDOObject object : objects)
    {
      for (EContentsEList.FeatureIterator<EObject> it = (FeatureIterator<EObject>)object.eCrossReferences().iterator(); it
          .hasNext();)
      {
        EObject crossReferencedObject = it.next();
        if (crossReferencedObject instanceof CDOObject
            && referencedOIDs.contains(((CDOObject)crossReferencedObject).cdoID()))
        {
          EReference eReference = (EReference)it.feature();
          Setting setting = ((InternalEObject)object).eSetting(eReference);
          EcoreUtil.remove(setting, crossReferencedObject);
        }
      }
    }
  }

  /**
   * @author Simon McDuff
   */
  private final class CDOCommitContextImpl implements InternalCDOCommitContext
  {
    private Map<CDOID, CDOResource> newResources;

    private Map<CDOID, CDOObject> newObjects;

    private Map<CDOID, CDOObject> dirtyObjects;

    private Map<CDOID, CDORevisionDelta> revisionDeltas;

    private Map<CDOID, CDOObject> detachedObjects;

    private List<CDOPackageUnit> newPackageUnits;

    public CDOCommitContextImpl()
    {
      CDOTransactionImpl transaction = getTransaction();
      newResources = transaction.getNewResources();
      newObjects = transaction.getNewObjects();
      dirtyObjects = transaction.getDirtyObjects();
      detachedObjects = transaction.getDetachedObjects();
      revisionDeltas = transaction.getRevisionDeltas();
      newPackageUnits = transaction.analyzeNewPackages();
    }

    public CDOTransactionImpl getTransaction()
    {
      return CDOTransactionImpl.this;
    }

    public Map<CDOID, CDOObject> getDirtyObjects()
    {
      return dirtyObjects;
    }

    public Map<CDOID, CDOObject> getNewObjects()
    {
      return newObjects;
    }

    public List<CDOPackageUnit> getNewPackageUnits()
    {
      return newPackageUnits;
    }

    public Map<CDOID, CDOResource> getNewResources()
    {
      return newResources;
    }

    public Map<CDOID, CDOObject> getDetachedObjects()
    {
      return detachedObjects;
    }

    public Map<CDOID, CDORevisionDelta> getRevisionDeltas()
    {
      return revisionDeltas;
    }

    public void preCommit()
    {
      if (isDirty())
      {
        if (TRACER.isEnabled())
        {
          TRACER.trace("commit()"); //$NON-NLS-1$
        }

        CDOTransactionHandler[] handlers = getTransactionHandlers();
        if (handlers != null)
        {
          for (int i = 0; i < handlers.length; i++)
          {
            CDOTransactionHandler handler = handlers[i];
            handler.committingTransaction(getTransaction(), this);
          }
        }

        try
        {
          preCommit(getNewResources());
          preCommit(getNewObjects());
          preCommit(getDirtyObjects());
        }
        catch (RuntimeException ex)
        {
          throw ex;
        }
        catch (Exception ex)
        {
          throw new TransactionException(ex);
        }
      }
    }

    public void postCommit(CommitTransactionResult result)
    {
      if (isDirty())
      {
        try
        {
          Collection<CDORevisionDelta> deltas = getRevisionDeltas().values();

          postCommit(getNewResources(), result);
          postCommit(getNewObjects(), result);
          postCommit(getDirtyObjects(), result);
          for (Entry<CDOID, CDOObject> entry : getDetachedObjects().entrySet())
          {
            removeObject(entry.getKey());
          }

          InternalCDOSession session = getSession();
          for (CDOPackageUnit newPackageUnit : newPackageUnits)
          {
            ((InternalCDOPackageUnit)newPackageUnit).setState(CDOPackageUnit.State.LOADED);
          }

          Map<CDOID, CDOObject> dirtyObjects = getDirtyObjects();
          Set<CDOIDAndVersion> dirtyIDs = new HashSet<CDOIDAndVersion>();
          for (CDOObject dirtyObject : dirtyObjects.values())
          {
            CDORevision revision = dirtyObject.cdoRevision();
            CDOIDAndVersion dirtyID = CDOIDUtil.createIDAndVersion(revision.getID(), revision.getVersion() - 1);
            dirtyIDs.add(dirtyID);
          }

          if (!dirtyIDs.isEmpty() || !getDetachedObjects().isEmpty())
          {
            Set<CDOID> detachedIDs = new HashSet<CDOID>(getDetachedObjects().keySet());
            Collection<CDORevisionDelta> deltasCopy = new ArrayList<CDORevisionDelta>(deltas);

            // Adjust references in the deltas. Could be used in
            // ChangeSubscription from others CDOView
            for (CDORevisionDelta dirtyObjectDelta : deltasCopy)
            {
              ((InternalCDORevisionDelta)dirtyObjectDelta).adjustReferences(result.getReferenceAdjuster());
            }

            session.handleCommitNotification(getBranch().getPoint(result.getTimeStamp()), newPackageUnits, dirtyIDs,
                detachedIDs, deltasCopy, getTransaction());
          }
          else
          {
            session.setLastUpdateTime(result.getTimeStamp());
          }

          CDOTransactionHandler[] handlers = getTransactionHandlers();
          if (handlers != null)
          {
            for (int i = 0; i < handlers.length; i++)
            {
              CDOTransactionHandler handler = handlers[i];
              handler.committedTransaction(getTransaction(), this);
            }
          }

          getChangeSubscriptionManager().committedTransaction(getTransaction(), this);
          getAdapterManager().committedTransaction(getTransaction(), this);

          cleanUp();
          Map<CDOIDTemp, CDOID> idMappings = result.getIDMappings();
          IListener[] listeners = getListeners();
          if (listeners != null)
          {
            fireEvent(new FinishedEvent(CDOTransactionFinishedEvent.Type.COMMITTED, idMappings), listeners);
          }
        }
        catch (RuntimeException ex)
        {
          throw ex;
        }
        catch (Exception ex)
        {
          throw new TransactionException(ex);
        }
      }
      else
      {
        // Removes locks even if no one touch the transaction
        if (options().isAutoReleaseLocksEnabled())
        {
          unlockObjects(null, null);
        }
      }
    }

    @SuppressWarnings("rawtypes")
    private void preCommit(Map objects)
    {
      if (!objects.isEmpty())
      {
        for (Object object : objects.values())
        {
          ((InternalCDOObject)object).cdoInternalPreCommit();
        }
      }
    }

    @SuppressWarnings("rawtypes")
    private void postCommit(Map objects, CommitTransactionResult result)
    {
      if (!objects.isEmpty())
      {
        for (Object object : objects.values())
        {
          CDOStateMachine.INSTANCE.commit((InternalCDOObject)object, result);
        }
      }
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class StartedEvent extends Event implements CDOTransactionStartedEvent
  {
    private static final long serialVersionUID = 1L;

    private StartedEvent()
    {
    }

    @Override
    public String toString()
    {
      return MessageFormat.format("CDOTransactionStartedEvent[source={0}]", getSource()); //$NON-NLS-1$
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class FinishedEvent extends Event implements CDOTransactionFinishedEvent
  {
    private static final long serialVersionUID = 1L;

    private Type type;

    private Map<CDOIDTemp, CDOID> idMappings;

    private FinishedEvent(Type type, Map<CDOIDTemp, CDOID> idMappings)
    {
      this.type = type;
      this.idMappings = idMappings;
    }

    public Type getType()
    {
      return type;
    }

    public Map<CDOIDTemp, CDOID> getIDMappings()
    {
      return idMappings;
    }

    @Override
    public String toString()
    {
      return MessageFormat.format("CDOTransactionFinishedEvent[source={0}, type={1}, idMappings={2}]", getSource(), //$NON-NLS-1$
          getType(), idMappings == null ? 0 : idMappings.size());
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class ConflictEvent extends Event implements CDOTransactionConflictEvent
  {
    private static final long serialVersionUID = 1L;

    private InternalCDOObject conflictingObject;

    private boolean firstConflict;

    public ConflictEvent(InternalCDOObject conflictingObject, boolean firstConflict)
    {
      this.conflictingObject = conflictingObject;
      this.firstConflict = firstConflict;
    }

    public InternalCDOObject getConflictingObject()
    {
      return conflictingObject;
    }

    public boolean isFirstConflict()
    {
      return firstConflict;
    }

    @Override
    public String toString()
    {
      return MessageFormat.format("CDOTransactionConflictEvent[source={0}, conflictingObject={1}, firstConflict={2}]", //$NON-NLS-1$
          getSource(), getConflictingObject(), isFirstConflict());
    }
  }

  /**
   * @author Eike Stepper
   */
  private final class ResourcesEvent extends Event implements CDOViewResourcesEvent
  {
    private static final long serialVersionUID = 1L;

    private String resourcePath;

    private Kind kind;

    public ResourcesEvent(String resourcePath, Kind kind)
    {
      this.resourcePath = resourcePath;
      this.kind = kind;
    }

    public String getResourcePath()
    {
      return resourcePath;
    }

    public Kind getKind()
    {
      return kind;
    }

    @Override
    public String toString()
    {
      return MessageFormat.format("CDOViewResourcesEvent[source={0}, {1}={2}]", getSource(), resourcePath, kind); //$NON-NLS-1$
    }
  }

  /**
   * @author Eike Stepper
   * @since 2.0
   */
  protected final class OptionsImpl extends CDOViewImpl.OptionsImpl implements CDOTransaction.Options
  {
    private List<CDOConflictResolver> conflictResolvers = new ArrayList<CDOConflictResolver>();

    private boolean autoReleaseLocksEnabled = true;

    public OptionsImpl()
    {
    }

    public CDOConflictResolver[] getConflictResolvers()
    {
      synchronized (conflictResolvers)
      {
        return conflictResolvers.toArray(new CDOConflictResolver[conflictResolvers.size()]);
      }
    }

    public void setConflictResolvers(CDOConflictResolver[] resolvers)
    {
      synchronized (conflictResolvers)
      {
        for (CDOConflictResolver resolver : conflictResolvers)
        {
          resolver.setTransaction(null);
        }

        conflictResolvers.clear();
        for (CDOConflictResolver resolver : resolvers)
        {
          validateResolver(resolver);
          conflictResolvers.add(resolver);
        }
      }

      IListener[] listeners = getListeners();
      if (listeners != null)
      {
        fireEvent(new ConflictResolversEventImpl(), listeners);
      }
    }

    public void addConflictResolver(CDOConflictResolver resolver)
    {
      boolean changed = false;
      synchronized (conflictResolvers)
      {
        if (!conflictResolvers.contains(resolver))
        {
          validateResolver(resolver);
          conflictResolvers.add(resolver);
          changed = true;
        }
      }

      if (changed)
      {
        IListener[] listeners = getListeners();
        if (listeners != null)
        {
          fireEvent(new ConflictResolversEventImpl(), listeners);
        }
      }
    }

    public void removeConflictResolver(CDOConflictResolver resolver)
    {
      boolean changed = false;
      synchronized (conflictResolvers)
      {
        changed = conflictResolvers.remove(resolver);
      }

      if (changed)
      {
        resolver.setTransaction(null);
        IListener[] listeners = getListeners();
        if (listeners != null)
        {
          fireEvent(new ConflictResolversEventImpl(), listeners);
        }
      }
    }

    public void disposeConflictResolvers()
    {
      try
      {
        for (CDOConflictResolver resolver : options().getConflictResolvers())
        {
          try
          {
            resolver.setTransaction(null);
          }
          catch (Exception ignore)
          {
          }
        }
      }
      catch (Exception ignore)
      {
      }
    }

    private void validateResolver(CDOConflictResolver resolver)
    {
      if (resolver.getTransaction() != null)
      {
        throw new IllegalArgumentException(Messages.getString("CDOTransactionImpl.17")); //$NON-NLS-1$
      }

      resolver.setTransaction(CDOTransactionImpl.this);
    }

    public boolean isAutoReleaseLocksEnabled()
    {
      return autoReleaseLocksEnabled;
    }

    public void setAutoReleaseLocksEnabled(boolean on)
    {
      if (autoReleaseLocksEnabled != on)
      {
        autoReleaseLocksEnabled = on;
        IListener[] listeners = getListeners();
        if (listeners != null)
        {
          fireEvent(new AutoReleaseLocksEventImpl(), listeners);
        }
      }
    }

    /**
     * @author Eike Stepper
     */
    private final class ConflictResolversEventImpl extends OptionsEvent implements ConflictResolversEvent
    {
      private static final long serialVersionUID = 1L;

      public ConflictResolversEventImpl()
      {
        super(OptionsImpl.this);
      }
    }

    /**
     * @author Eike Stepper
     */
    private final class AutoReleaseLocksEventImpl extends OptionsEvent implements AutoReleaseLocksEvent
    {
      private static final long serialVersionUID = 1L;

      public AutoReleaseLocksEventImpl()
      {
        super(OptionsImpl.this);
      }
    }
  }
}
