package fr.obeo.releng.targetplatform.util;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import fr.obeo.releng.targetplatform.Location;
import fr.obeo.releng.targetplatform.TargetPlatform;
import fr.obeo.releng.targetplatform.resolved.MavenSettingsManager;
import fr.obeo.releng.targetplatform.resolved.MavenSettingsManager.Server;

public class ResourceUtil {

  private static URI getResolvedImportUri(Resource context, URI uri) {
    URI contextURI = context.getURI();
    if (contextURI.isHierarchical() && !contextURI.isRelative() && (uri.isRelative() && !uri.isEmpty())) {
      uri = uri.resolve(contextURI);
    }
    return uri;
  }

  public static Resource getResource(Resource context, String uri) {

    System.out.println("Retrieve included location:" + uri);    
    
    MavenSettingsManager mavenSettingsManager = MavenSettingsManager.getInstance();
    final Server serverSettings = mavenSettingsManager.getServerSettings(uri);        
    
    if(serverSettings != null) {
    	System.out.println("Server settings identified for " + uri);
    	System.out.println("Using identified credentials");

    	Authenticator authenticator = new Authenticator() {
    		@Override
    		protected PasswordAuthentication getPasswordAuthentication() {
    			return new PasswordAuthentication(serverSettings.username, serverSettings.password.toCharArray());

    		}
    	};
    	Authenticator.setDefault(authenticator);
    }
    
    URI newURI = getResolvedImportUri(context, URI.createURI(uri));
    
    int maxRetries = 10;

    for (int i = 1; i <= maxRetries; i++) {
      try {
        Resource resource = context.getResourceSet().getResource(newURI, true);
        if (!resource.getErrors().isEmpty()) {
          context.getResourceSet().getResources().remove(resource);
          resource = context.getResourceSet().getResource(newURI, true);
        }
        Authenticator.setDefault(null);
        return resource;

      } catch (RuntimeException e) {
        System.out.println("Error while retrieving location:" + uri);
        e.printStackTrace();

        System.out.println("Retry:" + i + "/"+maxRetries);
        try {
          Thread.sleep(500);
        } catch (InterruptedException e2) {
          e2.printStackTrace();
        }
        
      }
    }
    
    Authenticator.setDefault(null);
    return null;

  }
  
  public static void replaceLocations(TargetPlatform targetPlatform) {
    List<Location> locations = targetPlatform.getLocations();
    for (Location location : locations) {
      String currentUri = location.getUri();
      String transformedUri = MavenSettingsManager.getInstance().getMirrorUrl(currentUri);
      
      if(!currentUri.equals(transformedUri)) {
        System.err.println("Replacing location: " + currentUri + " with " + transformedUri + " from settings.xml");
        location.setUri(transformedUri);
      }
    }
  }
}
