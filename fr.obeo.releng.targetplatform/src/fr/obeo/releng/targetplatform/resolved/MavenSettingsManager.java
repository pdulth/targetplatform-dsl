/*******************************************************************************
 * Copyright (c) 2018, 2020 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Minh Tu TON THAT
 *     Sandu POSTARU
 *******************************************************************************/
package fr.obeo.releng.targetplatform.resolved;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.obeo.releng.targetplatform.TargetPlatformBundleActivator;

/**
 * 
 * This class reads a setting file (in Maven format) to replace a url location
 * by its mirror
 *
 */
public class MavenSettingsManager {

	public class Mirror {
		public final String url;
		public final String mirrorOf;

		public Mirror(String url, String mirrorOf) {
			this.url = url;
			this.mirrorOf = mirrorOf;
		}
	}

	public class Server {
		public final String id;
		public final String username;
		public final String password;

		public Server(String id, String username, String password) {
			this.id = id;
			this.username = username;
			this.password = password;
		}
	}

	private static MavenSettingsManager instance;
	protected String settingsFileLocation;

	protected List<Mirror> mirrors;
	protected List<Server> servers;

	private MavenSettingsManager() {
		extractSettingsFileLocation();
		extractDataFromSettingsFile();
	}

	public static MavenSettingsManager getInstance() {
		if (instance == null)
			instance = new MavenSettingsManager();
		return instance;
	}

	private void extractSettingsFileLocation() {
		String[] args = Platform.getApplicationArgs();
		if (settingsFileLocation == null) {
			for (int i = 0; i < args.length; i++) {
				String currentArg = args[i];
				if ("-settingsFileLocation".equalsIgnoreCase(currentArg) && (i + 1 < args.length)) {
					this.settingsFileLocation = args[i + 1];
					break;
				}
			}
		}
	}

	private void extractDataFromSettingsFile() {
		mirrors = new ArrayList<>();
		servers = new ArrayList<>();

		if (settingsFileLocation != null) {
			try {
				// Prepare the DOM document
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.parse(new File(settingsFileLocation));

				// Prepare the XPath variables
				XPathFactory xfact = XPathFactory.newInstance();
				XPath xpath = xfact.newXPath();

				// Extract mirrors
				NodeList mirrorNodeList = (NodeList) xpath.evaluate("/settings/mirrors/mirror", document,
						XPathConstants.NODESET);

				for (int i = 0; i < mirrorNodeList.getLength(); i++) {
					Node mirrorNode = mirrorNodeList.item(i);
					Node mirrorOfNode = (Node) xpath.evaluate("mirrorOf", mirrorNode, XPathConstants.NODE);
					Node urlNode = (Node) xpath.evaluate("url", mirrorNode, XPathConstants.NODE);
					if (mirrorOfNode != null && urlNode != null) {
						String mirrorOf = mirrorOfNode.getTextContent();
						String url = urlNode.getTextContent();

						mirrors.add(new Mirror(url, mirrorOf));
					}
				}

				// Extract servers
				NodeList serverNodeList = (NodeList) xpath.evaluate("/settings/servers/server", document,
						XPathConstants.NODESET);

				for (int i = 0; i < serverNodeList.getLength(); i++) {
					Node serverNode = serverNodeList.item(i);
					Node idNode = (Node) xpath.evaluate("id", serverNode, XPathConstants.NODE);
					Node usernameNode = (Node) xpath.evaluate("username", serverNode, XPathConstants.NODE);
					Node passwordNode = (Node) xpath.evaluate("password", serverNode, XPathConstants.NODE);

					if (idNode != null && usernameNode != null && passwordNode != null) {
						String id = idNode.getTextContent();

						try {
							String domainName = getDomainName(id);
							String username = usernameNode.getTextContent();
							String password = passwordNode.getTextContent();

							servers.add(new Server(domainName, username, password));
						} catch (URISyntaxException e) {
							TargetPlatformBundleActivator.logger.warn("Invalid server id " + id);
						}

					}
				}

			} catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
				TargetPlatformBundleActivator.logger
						.warn("The settings file does not exist or it cannot be read: " + settingsFileLocation);
			}
		}
	}

	public static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();
		if(domain != null) {
			return domain.startsWith("www.") ? domain.substring(4) : domain;	
		}
		
		return url;
		
	}

	public String getMirrorUrl(String url) {
		String originalUrl = url;

		for (Mirror mirror : mirrors) {
			originalUrl = originalUrl.replace(mirror.mirrorOf, mirror.url);
		}

		return originalUrl;
	}

	public List<Server> getServerSettings() {
		return servers;
	}

	public void overrideP2ServerSettings(List<Server> mavenServers) {

		if (!mavenServers.isEmpty()) {
			ISecurePreferences preferences = SecurePreferencesFactory.getDefault();
			ISecurePreferences p2Node = preferences.node("org.eclipse.equinox.p2.repository");

			for (Server server : mavenServers) {
				ISecurePreferences siteNode = p2Node.node(server.id);

				try {
					siteNode.put("username", server.username, true);
					siteNode.put("password", server.password, true);
				} catch (StorageException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
