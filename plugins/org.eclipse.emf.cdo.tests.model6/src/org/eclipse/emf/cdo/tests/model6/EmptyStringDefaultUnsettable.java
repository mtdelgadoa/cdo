/**
 */
package org.eclipse.emf.cdo.tests.model6;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Empty String Default Unsettable</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.eclipse.emf.cdo.tests.model6.EmptyStringDefaultUnsettable#getAttribute <em>Attribute</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.eclipse.emf.cdo.tests.model6.Model6Package#getEmptyStringDefaultUnsettable()
 * @model
 * @generated
 */
public interface EmptyStringDefaultUnsettable extends EObject
{
  /**
   * Returns the value of the '<em><b>Attribute</b></em>' attribute.
   * The default value is <code>""</code>.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Attribute</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Attribute</em>' attribute.
   * @see #isSetAttribute()
   * @see #unsetAttribute()
   * @see #setAttribute(String)
   * @see org.eclipse.emf.cdo.tests.model6.Model6Package#getEmptyStringDefaultUnsettable_Attribute()
   * @model default="" unsettable="true"
   * @generated
   */
  String getAttribute();

  /**
   * Sets the value of the '{@link org.eclipse.emf.cdo.tests.model6.EmptyStringDefaultUnsettable#getAttribute <em>Attribute</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Attribute</em>' attribute.
   * @see #isSetAttribute()
   * @see #unsetAttribute()
   * @see #getAttribute()
   * @generated
   */
  void setAttribute(String value);

  /**
   * Unsets the value of the '{@link org.eclipse.emf.cdo.tests.model6.EmptyStringDefaultUnsettable#getAttribute <em>Attribute</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #isSetAttribute()
   * @see #getAttribute()
   * @see #setAttribute(String)
   * @generated
   */
  void unsetAttribute();

  /**
   * Returns whether the value of the '{@link org.eclipse.emf.cdo.tests.model6.EmptyStringDefaultUnsettable#getAttribute <em>Attribute</em>}' attribute is set.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return whether the value of the '<em>Attribute</em>' attribute is set.
   * @see #unsetAttribute()
   * @see #getAttribute()
   * @see #setAttribute(String)
   * @generated
   */
  boolean isSetAttribute();

} // EmptyStringDefaultUnsettable
