/*
 * Copyright (c) 2013 Eike Stepper (Berlin, Germany) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Eike Stepper - initial API and implementation
 */
package org.eclipse.emf.cdo.releng.projectconfig.impl;

import org.eclipse.emf.cdo.releng.projectconfig.PreferenceFilter;
import org.eclipse.emf.cdo.releng.projectconfig.PreferenceProfile;
import org.eclipse.emf.cdo.releng.projectconfig.Project;
import org.eclipse.emf.cdo.releng.projectconfig.ProjectConfigFactory;
import org.eclipse.emf.cdo.releng.projectconfig.ProjectConfigPackage;
import org.eclipse.emf.cdo.releng.projectconfig.WorkspaceConfiguration;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import java.util.regex.Pattern;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class ProjectConfigFactoryImpl extends EFactoryImpl implements ProjectConfigFactory
{
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static ProjectConfigFactory init()
  {
    try
    {
      ProjectConfigFactory theProjectConfigFactory = (ProjectConfigFactory)EPackage.Registry.INSTANCE
          .getEFactory(ProjectConfigPackage.eNS_URI);
      if (theProjectConfigFactory != null)
      {
        return theProjectConfigFactory;
      }
    }
    catch (Exception exception)
    {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new ProjectConfigFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ProjectConfigFactoryImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public EObject create(EClass eClass)
  {
    switch (eClass.getClassifierID())
    {
    case ProjectConfigPackage.WORKSPACE_CONFIGURATION:
      return createWorkspaceConfiguration();
    case ProjectConfigPackage.PROJECT:
      return createProject();
    case ProjectConfigPackage.PREFERENCE_PROFILE:
      return createPreferenceProfile();
    case ProjectConfigPackage.PREFERENCE_FILTER:
      return createPreferenceFilter();
    default:
      throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Object createFromString(EDataType eDataType, String initialValue)
  {
    switch (eDataType.getClassifierID())
    {
    case ProjectConfigPackage.PATTERN:
      return createPatternFromString(eDataType, initialValue);
    default:
      throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public String convertToString(EDataType eDataType, Object instanceValue)
  {
    switch (eDataType.getClassifierID())
    {
    case ProjectConfigPackage.PATTERN:
      return convertPatternToString(eDataType, instanceValue);
    default:
      throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public WorkspaceConfiguration createWorkspaceConfiguration()
  {
    WorkspaceConfigurationImpl workspaceConfiguration = new WorkspaceConfigurationImpl();
    return workspaceConfiguration;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Project createProject()
  {
    ProjectImpl project = new ProjectImpl();
    return project;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public PreferenceProfile createPreferenceProfile()
  {
    PreferenceProfileImpl preferenceProfile = new PreferenceProfileImpl();
    return preferenceProfile;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public PreferenceFilter createPreferenceFilter()
  {
    PreferenceFilterImpl preferenceFilter = new PreferenceFilterImpl();
    return preferenceFilter;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated NOT
   */
  public Pattern createPatternFromString(EDataType eDataType, String initialValue)
  {
    return initialValue == null ? null : Pattern.compile(initialValue);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated NOT
   */
  public String convertPatternToString(EDataType eDataType, Object instanceValue)
  {
    return instanceValue == null ? null : instanceValue.toString();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ProjectConfigPackage getProjectConfigPackage()
  {
    return (ProjectConfigPackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  @Deprecated
  public static ProjectConfigPackage getPackage()
  {
    return ProjectConfigPackage.eINSTANCE;
  }

} // ProjectConfigFactoryImpl