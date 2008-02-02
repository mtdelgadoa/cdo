/***************************************************************************
 * Copyright (c) 2004 - 2008 Eike Stepper, Germany.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Eike Stepper - initial API and implementation
 **************************************************************************/
package org.eclipse.emf.cdo.tests;

import org.eclipse.emf.cdo.CDOSession;
import org.eclipse.emf.cdo.CDOTransaction;
import org.eclipse.emf.cdo.server.CDOServerUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.cdo.server.IRepository.Props;
import org.eclipse.emf.cdo.server.internal.hibernate.HibernateStore;
import org.eclipse.emf.cdo.tests.model1.Category;
import org.eclipse.emf.cdo.tests.model1.Model1Factory;
import org.eclipse.emf.cdo.tests.model1.Model1Package;
import org.eclipse.emf.cdo.tests.model1.Product;
import org.eclipse.emf.cdo.util.CDOUtil;

import org.eclipse.net4j.Net4jUtil;
import org.eclipse.net4j.connector.IConnector;
import org.eclipse.net4j.internal.util.om.log.PrintLogHandler;
import org.eclipse.net4j.internal.util.om.trace.PrintTraceHandler;
import org.eclipse.net4j.jvm.JVMUtil;
import org.eclipse.net4j.util.container.ContainerUtil;
import org.eclipse.net4j.util.container.IManagedContainer;
import org.eclipse.net4j.util.om.OMPlatform;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MySQLDialect;

import java.io.PrintWriter;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Eike Stepper
 */
public class HibernateTest
{
  private static final String REPOSITORY_NAME = "repo1";

  public static void main(String[] args) throws Exception
  {
    // Turn on tracing
    OMPlatform.INSTANCE.setDebugging(true);
    OMPlatform.INSTANCE.addTraceHandler(PrintTraceHandler.CONSOLE);
    OMPlatform.INSTANCE.addLogHandler(PrintLogHandler.CONSOLE);

    // Prepare the standalone infra structure (not needed when running inside Eclipse)
    IManagedContainer container = ContainerUtil.createContainer(); // Create a wiring container
    Net4jUtil.prepareContainer(container); // Prepare the Net4j kernel
    JVMUtil.prepareContainer(container); // Prepare the JVM transport
    CDOServerUtil.prepareContainer(container); // Prepare the CDO server
    CDOUtil.prepareContainer(container, false); // Prepare the CDO client

    // Start the transport and create a repository
    JVMUtil.getAcceptor(container, "default"); // Start the JVM transport
    CDOServerUtil.addRepository(container, createRepository()); // Start a CDO respository

    // Establish a communications connection and open a session with the repository
    IConnector connector = JVMUtil.getConnector(container, "default"); // Open a JVM connection
    CDOSession session = CDOUtil.openSession(connector, REPOSITORY_NAME, true);// Open a CDO session
    session.getPackageRegistry().putEPackage(Model1Package.eINSTANCE);// Not needed after first commit!!!

    CDOTransaction transaction = session.openTransaction();// Open a CDO transaction
    Resource resource = transaction.createResource("/my/big/resource");// Create a new EMF resource

    // Work normally with the EMF resource
    EObject inputModel = getInputModel();
    resource.getContents().add(inputModel);
    transaction.commit();
    session.close();
    connector.disconnect();
  }

  private static IRepository createRepository() throws Exception
  {
    Map<String, String> props = new HashMap<String, String>();
    props.put(Props.PROP_SUPPORTING_AUDITS, "false");
    props.put(Props.PROP_SUPPORTING_REVISION_DELTAS, "true");
    props.put(Props.PROP_VERIFYING_REVISIONS, "false");
    props.put(Props.PROP_CURRENT_LRU_CAPACITY, "10000");
    props.put(Props.PROP_REVISED_LRU_CAPACITY, "10000");
    return CDOServerUtil.createRepository(REPOSITORY_NAME, createStore(), props);
  }

  private static IStore createStore() throws Exception
  {
    DriverManager.setLogWriter(new PrintWriter(System.out));
    Driver driver = new com.mysql.jdbc.Driver();
    DriverManager.registerDriver(driver);
    String driverName = driver.getClass().getName();
    String dialectName = MySQLDialect.class.getName();

    Configuration configuration = new Configuration();
    configuration.setProperty(Environment.DRIVER, driverName);
    configuration.setProperty(Environment.URL, "jdbc:mysql://localhost/cdohibernate");
    configuration.setProperty(Environment.USER, "root");
    configuration.setProperty(Environment.DIALECT, dialectName);
    configuration.setProperty(Environment.SHOW_SQL, "true");
    return new HibernateStore(configuration);
  }

  private static EObject getInputModel()
  {
    Category cat1 = Model1Factory.eINSTANCE.createCategory();
    cat1.setName("CAT1");
    Category cat2 = Model1Factory.eINSTANCE.createCategory();
    cat2.setName("CAT2");
    cat1.getCategories().add(cat2);
    Product p1 = Model1Factory.eINSTANCE.createProduct();
    p1.setName("P1");
    cat1.getProducts().add(p1);
    Product p2 = Model1Factory.eINSTANCE.createProduct();
    p2.setName("P2");
    cat1.getProducts().add(p2);
    Product p3 = Model1Factory.eINSTANCE.createProduct();
    p3.setName("P3");
    cat2.getProducts().add(p3);
    return cat1;
  }
}
