/*******************************************************************************
 * Copyright (c) 2018 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Minh Tu TON THAT
 *******************************************************************************/
package fr.obeo.releng.targetplatform.resolved;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.runtime.Platform;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.obeo.releng.targetplatform.TargetPlatformBundleActivator;

/**
 * 
 * This class reads a setting file (in Maven format) to replace a url location by its mirror
 *
 */
public class MirrorManager {
	String settingsFileLocation;
	Map<String, String> mirrorMap;
	private static MirrorManager instance;

	private MirrorManager() {
		extractSettingsFileLocation();
		initializeMirrorMap();
	}
	
	public static MirrorManager getInstance() {
		if (instance == null)
			instance = new MirrorManager();
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

	private void initializeMirrorMap() {
		mirrorMap = new HashMap<>();

		if (settingsFileLocation != null) {
			try {
				// Prepare the DOM document
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.parse(new File(settingsFileLocation));
				// Prepare the XPath variables
				XPathFactory xfact = XPathFactory.newInstance();
				XPath xpath = xfact.newXPath();
				// Fill mirror map
				NodeList mirrorNodeList = (NodeList) xpath.evaluate("/settings/mirrors/mirror", document,
						XPathConstants.NODESET);
				for (int i = 0; i < mirrorNodeList.getLength(); i++) {
					Node mirrorNode = mirrorNodeList.item(i);
					Node mirrorOfNode = (Node) xpath.evaluate("mirrorOf", mirrorNode, XPathConstants.NODE);
					Node urlNode = (Node) xpath.evaluate("url", mirrorNode, XPathConstants.NODE);
					if (mirrorOfNode != null && urlNode != null) {
						mirrorMap.put(mirrorOfNode.getTextContent(), urlNode.getTextContent());
					}
				}
			} catch (XPathExpressionException | ParserConfigurationException | IOException | SAXException e) {
				TargetPlatformBundleActivator.logger.warn("The settings file does not exist or it cannot be read: " + settingsFileLocation);
			}
		}
	}
	
	public String getMirrorUrl(String originalUrl) {
		String mirrorUrl = originalUrl;
		for (Entry<String, String> entry : mirrorMap.entrySet()) {
			mirrorUrl = mirrorUrl.replace(entry.getKey(), entry.getValue());
		}
		return mirrorUrl;
	}
}
