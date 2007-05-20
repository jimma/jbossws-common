/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.wsf.spi.deployment;

//$Id$

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jboss.wsf.spi.annotation.WebContext;
import org.jboss.wsf.spi.management.ServerConfig;
import org.jboss.wsf.spi.management.ServerConfigFactory;

/**
 * A deployer that generates a webapp for an EJB endpoint 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class WebAppGeneratorDeployer extends AbstractDeployer
{
   private SecurityRolesHandler securityRolesHandlerEJB21;
   private SecurityRolesHandler securityRolesHandlerEJB3;

   public void setSecurityRolesHandlerEJB21(SecurityRolesHandler securityRolesHandlerEJB21)
   {
      this.securityRolesHandlerEJB21 = securityRolesHandlerEJB21;
   }

   public void setSecurityRolesHandlerEJB3(SecurityRolesHandler securityRolesHandlerEJB3)
   {
      this.securityRolesHandlerEJB3 = securityRolesHandlerEJB3;
   }

   @Override
   public void create(Deployment dep)
   {
      UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
      if (udi == null)
         throw new IllegalStateException("Cannot obtain unified deployement info");

      if (dep.getType().toString().endsWith("EJB21"))
      {
         udi.webappURL = generatWebDeployment(dep, securityRolesHandlerEJB21);
      }
      else if (dep.getType().toString().endsWith("EJB3"))
      {
         udi.webappURL = generatWebDeployment(dep, securityRolesHandlerEJB3);
      }
   }

   private URL generatWebDeployment(Deployment dep, SecurityRolesHandler securityHandler)
   {
      Document webDoc = createWebAppDescriptor(dep, securityHandler);
      Document jbossDoc = createJBossWebAppDescriptor(dep);

      File tmpWar = null;
      try
      {
         ServerConfig config = ServerConfigFactory.getInstance().getServerConfig();
         File tmpdir = new File(config.getServerTempDir().getCanonicalPath() + "/deploy");

         UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);
         String deploymentName = udi.getCanonicalName().replace('/', '-');
         tmpWar = File.createTempFile(deploymentName, ".war", tmpdir);
         tmpWar.delete();

         File webInf = new File(tmpWar, "WEB-INF");
         webInf.mkdirs();

         File webXml = new File(webInf, "web.xml");
         FileWriter fw = new FileWriter(webXml);
         OutputFormat format = OutputFormat.createPrettyPrint();
         XMLWriter writer = new XMLWriter(fw, format);
         writer.write(webDoc);
         writer.close();

         File jbossWebXml = new File(webInf, "jboss-web.xml");
         fw = new FileWriter(jbossWebXml);
         writer = new XMLWriter(fw, format);
         writer.write(jbossDoc);
         writer.close();

         return tmpWar.toURL();
      }
      catch (IOException e)
      {
         throw new WSDeploymentException("Failed to create webservice.war", e);
      }
   }

   private Document createWebAppDescriptor(Deployment dep, SecurityRolesHandler securityHandler)
   {
      UnifiedDeploymentInfo udi = dep.getContext().getAttachment(UnifiedDeploymentInfo.class);

      Document document = DocumentHelper.createDocument();
      Element webApp = document.addElement("web-app");

      /*
       <servlet>
       <servlet-name>
       <servlet-class>
       </servlet>
       */
      for (Endpoint ep : dep.getService().getEndpoints())
      {
         Element servlet = webApp.addElement("servlet");
         servlet.addElement("servlet-name").addText(ep.getShortName());
         servlet.addElement("servlet-class").addText(ep.getTargetBean());
      }

      /*
       <servlet-mapping>
       <servlet-name>
       <url-pattern>
       </servlet-mapping>
       */
      ArrayList urlPatters = new ArrayList();
      for (Endpoint ep : dep.getService().getEndpoints())
      {
         Element servletMapping = webApp.addElement("servlet-mapping");
         servletMapping.addElement("servlet-name").addText(ep.getShortName());
         servletMapping.addElement("url-pattern").addText(ep.getURLPattern());
      }

      String authMethod = null;

      // Add web-app/security-constraint for each port component
      for (Endpoint ep : dep.getService().getEndpoints())
      {
         Class targetBean = ep.getTargetBeanClass();
         boolean secureWSDLAccess = false;
         String transportGuarantee = null;
         String beanAuthMethod = null;

         WebContext anWebContext = (WebContext)targetBean.getAnnotation(WebContext.class);
         if (anWebContext != null && anWebContext.authMethod().length() > 0)
            beanAuthMethod = anWebContext.authMethod();
         if (anWebContext != null && anWebContext.transportGuarantee().length() > 0)
            transportGuarantee = anWebContext.transportGuarantee();
         if (anWebContext != null && anWebContext.secureWSDLAccess())
            secureWSDLAccess = anWebContext.secureWSDLAccess();

         String ejbName = ep.getShortName();
         if (beanAuthMethod != null || transportGuarantee != null)
         {
            /*
             <security-constraint>
             <web-resource-collection>
             <web-resource-name>TestUnAuthPort</web-resource-name>
             <url-pattern>/HSTestRoot/TestUnAuth/*</url-pattern>
             </web-resource-collection>
             <auth-constraint>
             <role-name>*</role-name>
             </auth-constraint>
             <user-data-constraint>
             <transport-guarantee>NONE</transport-guarantee>
             </user-data-constraint>
             </security-constraint>
             */
            Element securityConstraint = webApp.addElement("security-constraint");
            Element wrc = securityConstraint.addElement("web-resource-collection");
            wrc.addElement("web-resource-name").addText(ejbName);
            wrc.addElement("url-pattern").addText(ep.getURLPattern());
            if (secureWSDLAccess)
            {
               wrc.addElement("http-method").addText("GET");
            }
            wrc.addElement("http-method").addText("POST");

            // Optional auth-constraint
            if (beanAuthMethod != null)
            {
               // Only the first auth-method gives the war login-config/auth-method
               if (authMethod == null)
                  authMethod = beanAuthMethod;

               Element authConstraint = securityConstraint.addElement("auth-constraint").addElement("role-name").addText("*");
            }
            // Optional user-data-constraint
            if (transportGuarantee != null)
            {
               Element userData = securityConstraint.addElement("user-data-constraint");
               userData.addElement("transport-guarantee").addText(transportGuarantee);
            }
         }
      }

      // Optional login-config/auth-method
      if (authMethod != null)
      {
         Element loginConfig = webApp.addElement("login-config");
         loginConfig.addElement("auth-method").addText(authMethod);
         loginConfig.addElement("realm-name").addText("EJBServiceEndpointServlet Realm");

         securityHandler.addSecurityRoles(webApp, udi);
      }

      return document;
   }

   private Document createJBossWebAppDescriptor(Deployment dep)
   {
      Document document = DocumentHelper.createDocument();

      /* Create a jboss-web
       <jboss-web>
       <security-domain>java:/jaas/cts</security-domain>
       <context-root>/ws/ejbN/</context-root>
       <virtual-host>some.domain.com</virtual-host>
       </jboss-web>
       */
      Element jbossWeb = document.addElement("jboss-web");

      String securityDomain = (String)dep.getContext().getProperty("security-domain");
      if (securityDomain != null)
         jbossWeb.addElement("security-domain").addText("java:/jaas/" + securityDomain);

      // Get the context root for this deployment
      String contextRoot = dep.getService().getContextRoot();
      if (contextRoot == null)
         throw new WSDeploymentException("Cannot obtain context root");

      jbossWeb.addElement("context-root").addText(contextRoot);

      return document;
   }
}