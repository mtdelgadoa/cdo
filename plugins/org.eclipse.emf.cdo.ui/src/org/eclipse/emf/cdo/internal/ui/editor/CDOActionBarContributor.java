/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.eclipse.emf.cdo.internal.ui.editor;

import org.eclipse.emf.cdo.eresource.CDOResource;
import org.eclipse.emf.cdo.internal.ui.actions.ExportResourceAction;
import org.eclipse.emf.cdo.internal.ui.actions.ImportRootsAction;
import org.eclipse.emf.cdo.internal.ui.actions.ReloadObjectsAction;

import org.eclipse.emf.common.ui.viewer.IViewerProvider;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.emf.edit.ui.action.ControlAction;
import org.eclipse.emf.edit.ui.action.CreateChildAction;
import org.eclipse.emf.edit.ui.action.CreateSiblingAction;
import org.eclipse.emf.edit.ui.action.EditingDomainActionBarContributor;
import org.eclipse.emf.edit.ui.action.LoadResourceAction;
import org.eclipse.emf.edit.ui.action.ValidateAction;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This is the action bar contributor for the CDO model editor. <!-- begin-user-doc --> <!-- end-user-doc -->
 * 
 * @generated
 */
public class CDOActionBarContributor extends EditingDomainActionBarContributor implements ISelectionChangedListener
{
  /**
   * @ADDED
   */
  public static final String LOAD_RESOURCE_ID = "load-resource";

  /**
   * @ADDED
   */
  public static final String REFRESH_VIEWER_ID = "refresh-viewer";

  /**
   * @ADDED
   */
  protected ImportRootsAction importResourceAction;

  /**
   * @ADDED
   */
  protected ExportResourceAction exportResourceAction;

  /**
   * @ADDED
   */
  protected ReloadObjectsAction reloadObjectsAction;

  /**
   * This keeps track of the active editor. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected IEditorPart activeEditorPart;

  /**
   * This keeps track of the current selection provider. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected ISelectionProvider selectionProvider;

  /**
   * This action opens the Properties view. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected IAction showPropertiesViewAction = new Action(PluginDelegator.INSTANCE
      .getString("_UI_ShowPropertiesView_menu_item"))
  {
    @Override
    public void run()
    {
      try
      {
        getPage().showView("org.eclipse.ui.views.PropertySheet");
      }
      catch (PartInitException exception)
      {
        PluginDelegator.INSTANCE.log(exception);
      }
    }
  };

  /**
   * This action refreshes the viewer of the current editor if the editor implements
   * {@link org.eclipse.emf.common.ui.viewer.IViewerProvider}. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected IAction refreshViewerAction = new Action(PluginDelegator.INSTANCE.getString("_UI_RefreshViewer_menu_item"))
  {
    @Override
    public boolean isEnabled()
    {
      return activeEditorPart instanceof IViewerProvider;
    }

    @Override
    public void run()
    {
      if (activeEditorPart instanceof IViewerProvider)
      {
        Viewer viewer = ((IViewerProvider)activeEditorPart).getViewer();
        if (viewer != null)
        {
          viewer.refresh();
        }
      }
    }
  };

  /**
   * This will contain one {@link org.eclipse.emf.edit.ui.action.CreateChildAction} corresponding to each descriptor
   * generated for the current selection by the item provider. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected Collection<IAction> createChildActions;

  /**
   * This is the menu manager into which menu contribution items should be added for CreateChild actions. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected IMenuManager createChildMenuManager;

  /**
   * This will contain one {@link org.eclipse.emf.edit.ui.action.CreateSiblingAction} corresponding to each descriptor
   * generated for the current selection by the item provider. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected Collection<IAction> createSiblingActions;

  /**
   * This is the menu manager into which menu contribution items should be added for CreateSibling actions. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected IMenuManager createSiblingMenuManager;

  /**
   * This creates an instance of the contributor. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated NOT
   */
  public CDOActionBarContributor()
  {
    super(ADDITIONS_LAST_STYLE);
    loadResourceAction = new LoadResourceAction();
    loadResourceAction.setId(LOAD_RESOURCE_ID);

    importResourceAction = new ImportRootsAction();
    importResourceAction.setId(ImportRootsAction.ID);

    // exportResourceAction = new ExportResourceAction(); // TODO
    // exportResourceAction.setId(EXPORT_RESOURCE_ID);

    reloadObjectsAction = new ReloadObjectsAction();

    validateAction = new ValidateAction();
    controlAction = new ControlAction();
  }

  /**
   * This adds Separators for editor additions to the tool bar. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public void contributeToToolBar(IToolBarManager toolBarManager)
  {
    toolBarManager.add(new Separator("cdo-settings"));
    toolBarManager.add(new Separator("cdo-additions"));
  }

  /**
   * This adds to the menu bar a menu and some separators for editor additions, as well as the sub-menus for object
   * creation items. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public void contributeToMenu(IMenuManager menuManager)
  {
    super.contributeToMenu(menuManager);

    IMenuManager submenuManager = new MenuManager(PluginDelegator.INSTANCE.getString("_UI_CDOEditor_menu"),
        "org.eclipse.emf.cdo.internal.ui.editorMenuID");
    menuManager.insertAfter("additions", submenuManager);
    submenuManager.add(new Separator("settings"));
    submenuManager.add(new Separator("actions"));
    submenuManager.add(new Separator("additions"));
    submenuManager.add(new Separator("additions-end"));

    // Prepare for CreateChild item addition or removal.
    //
    createChildMenuManager = new MenuManager(PluginDelegator.INSTANCE.getString("_UI_CreateChild_menu_item"));
    submenuManager.insertBefore("additions", createChildMenuManager);

    // Prepare for CreateSibling item addition or removal.
    //
    createSiblingMenuManager = new MenuManager(PluginDelegator.INSTANCE.getString("_UI_CreateSibling_menu_item"));
    submenuManager.insertBefore("additions", createSiblingMenuManager);

    // Force an update because Eclipse hides empty menus now.
    //
    submenuManager.addMenuListener(new IMenuListener()
    {
      public void menuAboutToShow(IMenuManager menuManager)
      {
        menuManager.updateAll(true);
      }
    });

    addGlobalActions(submenuManager);
  }

  /**
   * When the active editor changes, this remembers the change and registers with it as a selection provider. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void setActiveEditorGen(IEditorPart part)
  {
    super.setActiveEditor(part);
    activeEditorPart = part;

    // Switch to the new selection provider.
    //
    if (selectionProvider != null)
    {
      selectionProvider.removeSelectionChangedListener(this);
    }
    if (part == null)
    {
      selectionProvider = null;
    }
    else
    {
      selectionProvider = part.getSite().getSelectionProvider();
      selectionProvider.addSelectionChangedListener(this);

      // Fake a selection changed event to update the menus.
      //
      if (selectionProvider.getSelection() != null)
      {
        selectionChanged(new SelectionChangedEvent(selectionProvider, selectionProvider.getSelection()));
      }
    }
  }

  /**
   * @ADDED
   */
  @Override
  public void setActiveEditor(IEditorPart part)
  {
    super.setActiveEditor(part);
    activeEditorPart = part;
    if (reloadObjectsAction != null && activeEditor instanceof CDOEditor)
    {
      reloadObjectsAction.setEditor((CDOEditor)activeEditorPart);
    }

    // Switch to the new selection provider.
    //
    if (selectionProvider != null)
    {
      selectionProvider.removeSelectionChangedListener(this);
    }
    if (part == null)
    {
      selectionProvider = null;
    }
    else
    {
      selectionProvider = part.getSite().getSelectionProvider();
      selectionProvider.addSelectionChangedListener(this);

      // Fake a selection changed event to update the menus.
      //
      if (selectionProvider.getSelection() != null)
      {
        selectionChanged(new SelectionChangedEvent(selectionProvider, selectionProvider.getSelection()));
      }
    }
  }

  /**
   * This implements {@link org.eclipse.jface.viewers.ISelectionChangedListener}, handling
   * {@link org.eclipse.jface.viewers.SelectionChangedEvent}s by querying for the children and siblings that can be
   * added to the selected object and updating the menus accordingly. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  public void selectionChangedGen(SelectionChangedEvent event)
  {
    // Remove any menu items for old selection.
    //
    if (createChildMenuManager != null)
    {
      depopulateManager(createChildMenuManager, createChildActions);
    }
    if (createSiblingMenuManager != null)
    {
      depopulateManager(createSiblingMenuManager, createSiblingActions);
    }

    // Query the new selection for appropriate new child/sibling descriptors
    //
    Collection<?> newChildDescriptors = null;
    Collection<?> newSiblingDescriptors = null;

    ISelection selection = event.getSelection();
    if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).size() == 1)
    {
      Object object = ((IStructuredSelection)selection).getFirstElement();

      EditingDomain domain = ((IEditingDomainProvider)activeEditorPart).getEditingDomain();

      newChildDescriptors = domain.getNewChildDescriptors(object, null);
      newSiblingDescriptors = domain.getNewChildDescriptors(null, object);
    }

    // Generate actions for selection; populate and redraw the menus.
    //
    createChildActions = generateCreateChildActions(newChildDescriptors, selection);
    createSiblingActions = generateCreateSiblingActions(newSiblingDescriptors, selection);

    if (createChildMenuManager != null)
    {
      populateManager(createChildMenuManager, createChildActions, null);
      createChildMenuManager.update(true);
    }
    if (createSiblingMenuManager != null)
    {
      populateManager(createSiblingMenuManager, createSiblingActions, null);
      createSiblingMenuManager.update(true);
    }
  }

  /**
   * @ADDED
   */
  public void selectionChanged(SelectionChangedEvent event)
  {
    // Remove any menu items for old selection.
    //
    if (createChildMenuManager != null)
    {
      depopulateManager(createChildMenuManager, createChildActions);
    }
    if (createSiblingMenuManager != null)
    {
      depopulateManager(createSiblingMenuManager, createSiblingActions);
    }

    // Query the new selection for appropriate new child/sibling descriptors
    //
    Collection<?> newChildDescriptors = null;
    Collection<?> newSiblingDescriptors = null;

    ISelection selection = event.getSelection();
    if (selection instanceof IStructuredSelection)
    {
      if (reloadObjectsAction != null)
      {
        reloadObjectsAction.selectionChanged((IStructuredSelection)selection);
      }

      if (((IStructuredSelection)selection).size() == 1)
      {
        Object object = ((IStructuredSelection)selection).getFirstElement();

        EditingDomain domain = ((IEditingDomainProvider)activeEditorPart).getEditingDomain();

        newChildDescriptors = domain.getNewChildDescriptors(object, null);
        newSiblingDescriptors = domain.getNewChildDescriptors(null, object);

        if (importResourceAction != null)
        {
          if (object instanceof CDOResource)
          {
            importResourceAction.setTargetResource((CDOResource)object);
          }
          else
          {
            importResourceAction.setTargetResource(null);
          }
        }
      }
    }

    // Generate actions for selection; populate and redraw the menus.
    //
    createChildActions = generateCreateChildActions(newChildDescriptors, selection);
    createSiblingActions = generateCreateSiblingActions(newSiblingDescriptors, selection);

    if (createChildMenuManager != null)
    {
      populateManager(createChildMenuManager, createChildActions, null);
      createChildMenuManager.update(true);
    }
    if (createSiblingMenuManager != null)
    {
      populateManager(createSiblingMenuManager, createSiblingActions, null);
      createSiblingMenuManager.update(true);
    }
  }

  /**
   * This generates a {@link org.eclipse.emf.edit.ui.action.CreateChildAction} for each object in
   * <code>descriptors</code>, and returns the collection of these actions. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   */
  protected Collection<IAction> generateCreateChildActions(Collection<?> descriptors, ISelection selection)
  {
    Collection<IAction> actions = new ArrayList<IAction>();
    if (descriptors != null)
    {
      for (Object descriptor : descriptors)
      {
        actions.add(new CreateChildAction(activeEditorPart, selection, descriptor));
      }
    }
    return actions;
  }

  /**
   * This generates a {@link org.eclipse.emf.edit.ui.action.CreateSiblingAction} for each object in
   * <code>descriptors</code>, and returns the collection of these actions. <!-- begin-user-doc --> <!-- end-user-doc
   * -->
   * 
   * @generated
   */
  protected Collection<IAction> generateCreateSiblingActions(Collection<?> descriptors, ISelection selection)
  {
    Collection<IAction> actions = new ArrayList<IAction>();
    if (descriptors != null)
    {
      for (Object descriptor : descriptors)
      {
        actions.add(new CreateSiblingAction(activeEditorPart, selection, descriptor));
      }
    }
    return actions;
  }

  /**
   * This populates the specified <code>manager</code> with {@link org.eclipse.jface.action.ActionContributionItem}s
   * based on the {@link org.eclipse.jface.action.IAction}s contained in the <code>actions</code> collection, by
   * inserting them before the specified contribution item <code>contributionID</code>. If
   * <code>contributionID</code> is <code>null</code>, they are simply added. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   */
  protected void populateManager(IContributionManager manager, Collection<? extends IAction> actions,
      String contributionID)
  {
    if (actions != null)
    {
      for (IAction action : actions)
      {
        if (contributionID != null)
        {
          manager.insertBefore(contributionID, action);
        }
        else
        {
          manager.add(action);
        }
      }
    }
  }

  /**
   * This removes from the specified <code>manager</code> all {@link org.eclipse.jface.action.ActionContributionItem}s
   * based on the {@link org.eclipse.jface.action.IAction}s contained in the <code>actions</code> collection. <!--
   * begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected void depopulateManager(IContributionManager manager, Collection<? extends IAction> actions)
  {
    if (actions != null)
    {
      IContributionItem[] items = manager.getItems();
      for (int i = 0; i < items.length; i++)
      {
        // Look into SubContributionItems
        //
        IContributionItem contributionItem = items[i];
        while (contributionItem instanceof SubContributionItem)
        {
          contributionItem = ((SubContributionItem)contributionItem).getInnerItem();
        }

        // Delete the ActionContributionItems with matching action.
        //
        if (contributionItem instanceof ActionContributionItem)
        {
          IAction action = ((ActionContributionItem)contributionItem).getAction();
          if (actions.contains(action))
          {
            manager.remove(contributionItem);
          }
        }
      }
    }
  }

  /**
   * This populates the pop-up menu before it appears. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  @Override
  public void menuAboutToShow(IMenuManager menuManager)
  {
    super.menuAboutToShow(menuManager);
    MenuManager submenuManager = null;

    submenuManager = new MenuManager(PluginDelegator.INSTANCE.getString("_UI_CreateChild_menu_item"));
    populateManager(submenuManager, createChildActions, null);
    menuManager.insertBefore("edit", submenuManager);

    submenuManager = new MenuManager(PluginDelegator.INSTANCE.getString("_UI_CreateSibling_menu_item"));
    populateManager(submenuManager, createSiblingActions, null);
    menuManager.insertBefore("edit", submenuManager);
  }

  /**
   * This inserts global actions before the "additions-end" separator. <!-- begin-user-doc --> <!-- end-user-doc -->
   * 
   * @generated
   */
  protected void addGlobalActionsGen(IMenuManager menuManager)
  {
    menuManager.insertAfter("additions-end", new Separator("ui-actions"));
    menuManager.insertAfter("ui-actions", showPropertiesViewAction);

    refreshViewerAction.setEnabled(refreshViewerAction.isEnabled());
    menuManager.insertAfter("ui-actions", refreshViewerAction);

    super.addGlobalActions(menuManager);
  }

  /**
   * @ADDED
   */
  @Override
  protected void addGlobalActions(IMenuManager menuManager)
  {
    menuManager.insertAfter("additions-end", new Separator("ui-actions"));
    menuManager.insertAfter("ui-actions", showPropertiesViewAction);

    refreshViewerAction.setEnabled(refreshViewerAction.isEnabled());
    refreshViewerAction.setId(REFRESH_VIEWER_ID);
    menuManager.insertAfter("ui-actions", refreshViewerAction);
    menuManager.insertBefore(refreshViewerAction.getId(), reloadObjectsAction);
    super.addGlobalActions(menuManager);

    if (loadResourceAction != null)
    {
      if (exportResourceAction != null)
      {
        menuManager.insertAfter(loadResourceAction.getId(), exportResourceAction);
      }

      if (importResourceAction != null)
      {
        menuManager.insertAfter(loadResourceAction.getId(), importResourceAction);
      }
    }
    else
    {
      if (importResourceAction != null)
      {
        menuManager.insertBefore("additions-end", importResourceAction);
      }

      if (exportResourceAction != null)
      {
        menuManager.insertAfter("additions-end", exportResourceAction);
      }

      menuManager.insertBefore("additions-end", new Separator());
    }
  }

  /**
   * This ensures that a delete action will clean up all references to deleted objects. <!-- begin-user-doc --> <!--
   * end-user-doc -->
   * 
   * @generated
   */
  @Override
  protected boolean removeAllReferencesOnDelete()
  {
    return true;
  }

  /**
   * @ADDED
   */
  @Override
  public void activate()
  {
    if (importResourceAction != null)
    {
      importResourceAction.setActiveWorkbenchPart(activeEditor);
      Object input = ((CDOEditor)getActiveEditor()).getViewer().getInput();
      if (input instanceof CDOResource)
      {
        importResourceAction.setTargetResource((CDOResource)input);
      }
      else
      {
        importResourceAction.setTargetResource(null);
      }
    }

    if (exportResourceAction != null)
    {
      exportResourceAction.setActiveWorkbenchPart(activeEditor);
    }

    super.activate();
  }

  /**
   * @ADDED
   */
  @Override
  public void deactivate()
  {
    if (importResourceAction != null)
    {
      importResourceAction.setActiveWorkbenchPart(null);
      importResourceAction.setTargetResource(null);
    }

    if (exportResourceAction != null)
    {
      exportResourceAction.setActiveWorkbenchPart(null);
    }

    super.deactivate();
  }

  /**
   * @ADDED
   */
  @Override
  public void update()
  {
    super.update();
    if (importResourceAction != null)
    {
      importResourceAction.update();
    }

    if (exportResourceAction != null)
    {
      exportResourceAction.update();
    }

    if (reloadObjectsAction != null)
    {
      reloadObjectsAction.update();
    }
  }
}