/**
 */
package org.eclipse.emf.cdo.releng.setup.impl;

import org.eclipse.emf.cdo.releng.internal.setup.util.BasicProjectAnalyzer;
import org.eclipse.emf.cdo.releng.internal.setup.util.WorkspaceUtil;
import org.eclipse.emf.cdo.releng.setup.AutomaticSourceLocator;
import org.eclipse.emf.cdo.releng.setup.ProjectsImportTask;
import org.eclipse.emf.cdo.releng.setup.SetupPackage;
import org.eclipse.emf.cdo.releng.setup.SetupTask;
import org.eclipse.emf.cdo.releng.setup.SetupTaskContext;
import org.eclipse.emf.cdo.releng.setup.log.ProgressLogMonitor;
import org.eclipse.emf.cdo.releng.setup.util.ProjectProvider.Visitor;

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Import Project Task</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.releng.setup.impl.ProjectsImportTaskImpl#getSourceLocators <em>Source Locators</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ProjectsImportTaskImpl extends SetupTaskImpl implements ProjectsImportTask
{
  /**
   * The cached value of the '{@link #getSourceLocators() <em>Source Locators</em>}' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getSourceLocators()
   * @generated
   * @ordered
   */
  protected EList<AutomaticSourceLocator> sourceLocators;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected ProjectsImportTaskImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return SetupPackage.Literals.PROJECTS_IMPORT_TASK;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList<AutomaticSourceLocator> getSourceLocators()
  {
    if (sourceLocators == null)
    {
      sourceLocators = new EObjectContainmentEList.Resolving<AutomaticSourceLocator>(AutomaticSourceLocator.class,
          this, SetupPackage.PROJECTS_IMPORT_TASK__SOURCE_LOCATORS);
    }
    return sourceLocators;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs)
  {
    switch (featureID)
    {
    case SetupPackage.PROJECTS_IMPORT_TASK__SOURCE_LOCATORS:
      return ((InternalEList<?>)getSourceLocators()).basicRemove(otherEnd, msgs);
    }
    return super.eInverseRemove(otherEnd, featureID, msgs);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType)
  {
    switch (featureID)
    {
    case SetupPackage.PROJECTS_IMPORT_TASK__SOURCE_LOCATORS:
      return getSourceLocators();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @SuppressWarnings("unchecked")
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
    case SetupPackage.PROJECTS_IMPORT_TASK__SOURCE_LOCATORS:
      getSourceLocators().clear();
      getSourceLocators().addAll((Collection<? extends AutomaticSourceLocator>)newValue);
      return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eUnset(int featureID)
  {
    switch (featureID)
    {
    case SetupPackage.PROJECTS_IMPORT_TASK__SOURCE_LOCATORS:
      getSourceLocators().clear();
      return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public boolean eIsSet(int featureID)
  {
    switch (featureID)
    {
    case SetupPackage.PROJECTS_IMPORT_TASK__SOURCE_LOCATORS:
      return sourceLocators != null && !sourceLocators.isEmpty();
    }
    return super.eIsSet(featureID);
  }

  @Override
  public int getPriority()
  {
    return 0;
  }

  @Override
  public Object getOverrideToken()
  {
    return null;
  }

  @Override
  public void overrideFor(SetupTask overriddenTask)
  {
  }

  @Override
  public void consolidate()
  {
  }

  @Override
  public boolean needsBundlePool()
  {
    return false;
  }

  @Override
  public boolean needsBundlePoolTP()
  {
    return false;
  }

  public boolean isNeeded(SetupTaskContext context) throws Exception
  {
    EList<AutomaticSourceLocator> sourceLocators = getSourceLocators();
    if (sourceLocators.isEmpty())
    {
      return false;
    }

    return true;
  }

  public void perform(SetupTaskContext context) throws Exception
  {
    EList<AutomaticSourceLocator> locators = getSourceLocators();
    for (AutomaticSourceLocator source : locators)
    {
      context.log("Importing projects from " + source.getRootFolder());
      Map<IProject, File> projects = new BasicProjectAnalyzer<IProject>().collectProjects(
          new File(source.getRootFolder()), source.getPredicates(), source.isLocateNestedProjects(),
          new ProgressLogMonitor(context));
      WorkspaceUtil.importProjects(projects.values(), new ProgressLogMonitor(context));
    }
  }

  @Override
  public void dispose()
  {
  }

  @Override
  public MirrorRunnable mirror(MirrorContext context, File mirrorsDir, boolean includingLocals) throws Exception
  {
    return null;
  }

  @Override
  public void collectSniffers(List<Sniffer> sniffers)
  {
  }

  public <T> Map<T, File> accept(Visitor<T> visitor, IProgressMonitor monitor)
  {
    return null;
  }

} // ProjectsImportTaskImpl
