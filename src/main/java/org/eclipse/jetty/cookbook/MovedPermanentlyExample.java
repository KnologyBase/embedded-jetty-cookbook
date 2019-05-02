//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.cookbook;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.cookbook.servlets.DumpServlet;
import org.eclipse.jetty.rewrite.handler.RedirectUtil;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.Rule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.PathResource;

@SuppressWarnings("Duplicates")
public class MovedPermanentlyExample
{
    public static class MovedPermanentlyRule extends Rule
    {
        private Pattern regex;
        private String replacement;
        
        public MovedPermanentlyRule()
        {
            setTerminating(true);
        }

        public String getRegex()
        {
            return regex == null ? null : regex.pattern();
        }

        public String getReplacement()
        {
            return replacement;
        }

        @Override
        public String matchAndApply(String target, HttpServletRequest request, HttpServletResponse response) throws IOException
        {
            Matcher matcher = regex.matcher(request.getRequestURL());
            boolean matches = matcher.matches();
            if (matches)
            {
                String location = response.encodeRedirectURL(replacement);
                response.setHeader("Location",RedirectUtil.toRedirectURL(request,location));
                response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                response.getOutputStream().flush(); // no output / content
                response.getOutputStream().close();
                return location;
            }
            return null;
        }

        public void setRegex(String regex)
        {
            this.regex = Pattern.compile(regex);
        }

        public void setReplacement(String replacement)
        {
            this.replacement = replacement;
        }

        @Override
        public String toString()
        {
            return super.toString() + "[" + regex + "]";
        }
    }

    public static void main(String[] args) throws Exception
    {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);

        HandlerList handlers = new HandlerList();
        server.setHandler(handlers);

        // Add Rewrite / Redirect handlers + Rules
        RewriteHandler rewriteHandler = new RewriteHandler();
        MovedPermanentlyRule movedRule = new MovedPermanentlyRule();
        movedRule.setRegex("http://www.company.com/dump/.*");
        movedRule.setReplacement("https://api.company.com/dump/");
        rewriteHandler.addRule(movedRule);
        handlers.addHandler(rewriteHandler);

        Path webRootPath = new File("webapps/alt-root/").toPath().toRealPath();

        ServletContextHandler context = new ServletContextHandler();
        handlers.addHandler(context);
        context.setContextPath("/");
        context.setBaseResource(new PathResource(webRootPath));
        context.setWelcomeFiles(new String[] { "index.html" });
        context.addServlet(DumpServlet.class,"/dump/*");

        server.start();
        server.join();
    }
}
