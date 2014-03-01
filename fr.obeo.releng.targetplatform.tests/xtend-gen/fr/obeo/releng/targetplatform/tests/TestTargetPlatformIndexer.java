package fr.obeo.releng.targetplatform.tests;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import fr.obeo.releng.targetplatform.TargetPlatformInjectorProvider;
import fr.obeo.releng.targetplatform.targetplatform.Location;
import fr.obeo.releng.targetplatform.targetplatform.TargetPlatform;
import fr.obeo.releng.targetplatform.util.LocationIndexBuilder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@InjectWith(TargetPlatformInjectorProvider.class)
@RunWith(XtextRunner.class)
@SuppressWarnings("all")
public class TestTargetPlatformIndexer {
  @Inject
  private ParseHelper<TargetPlatform> parser;
  
  @Inject
  private Provider<XtextResourceSet> resourceSetProvider;
  
  @Inject
  private LocationIndexBuilder indexBuilder;
  
  @Test
  public void testIncludeOverrideOrder() {
    try {
      final XtextResourceSet resourceSet = this.resourceSetProvider.get();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("target \"o\" include \"a.tpd\" include \"b.tpd\" include \"c.tpd\"");
      URI _createURI = URI.createURI("tmp:/o.tpd");
      final TargetPlatform o = this.parser.parse(_builder, _createURI, resourceSet);
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("target \"a\" include \"d.tpd\" include \"e.tpd\" include \"f.tpd\"");
      URI _createURI_1 = URI.createURI("tmp:/a.tpd");
      this.parser.parse(_builder_1, _createURI_1, resourceSet);
      StringConcatenation _builder_2 = new StringConcatenation();
      _builder_2.append("target \"b\" include \"g.tpd\" include \"h.tpd\" include \"i.tpd\"");
      URI _createURI_2 = URI.createURI("tmp:/b.tpd");
      this.parser.parse(_builder_2, _createURI_2, resourceSet);
      StringConcatenation _builder_3 = new StringConcatenation();
      _builder_3.append("target \"c\" include \"j.tpd\" include \"k.tpd\" include \"l.tpd\"");
      URI _createURI_3 = URI.createURI("tmp:/c.tpd");
      this.parser.parse(_builder_3, _createURI_3, resourceSet);
      for (final String tp : Collections.<String>unmodifiableList(Lists.<String>newArrayList("d", "e", "f", "g", "h", "i", "j", "k", "l"))) {
        StringConcatenation _builder_4 = new StringConcatenation();
        _builder_4.append("target \"");
        _builder_4.append(tp, "");
        _builder_4.append("\"");
        StringConcatenation _builder_5 = new StringConcatenation();
        _builder_5.append("tmp:/");
        _builder_5.append(tp, "");
        _builder_5.append(".tpd");
        URI _createURI_4 = URI.createURI(_builder_5.toString());
        this.parser.parse(_builder_4, _createURI_4, resourceSet);
      }
      LinkedList<TargetPlatform> _importedTargetPlatforms = this.indexBuilder.getImportedTargetPlatforms(o);
      final Function1<TargetPlatform,String> _function = new Function1<TargetPlatform,String>() {
        public String apply(final TargetPlatform it) {
          return it.getName();
        }
      };
      List<String> _map = ListExtensions.<TargetPlatform, String>map(_importedTargetPlatforms, _function);
      Assert.assertEquals(
        Collections.<String>unmodifiableList(Lists.<String>newArrayList("c", "b", "a", "l", "k", "j", "i", "h", "g", "f", "e", "d")), _map);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testRemoteInclude() {
    try {
      final XtextResourceSet resourceSet = this.resourceSetProvider.get();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("target \"o\" ");
      _builder.newLine();
      _builder.append("include \"http://git.eclipse.org/c/emfcompare/org.eclipse.emf.compare.git/plain/org.eclipse.emf.compare-parent/targetPlatforms/kepler-sdk.targetplatform?id=e9fbfd866f648083df83174d566ee1b9d61daeca\" ");
      _builder.newLine();
      URI _createURI = URI.createURI("tmp:/o.tpd");
      final TargetPlatform o = this.parser.parse(_builder, _createURI, resourceSet);
      ArrayListMultimap<String,Location> _locationIndex = this.indexBuilder.getLocationIndex(o);
      int _size = _locationIndex.size();
      Assert.assertEquals(4, _size);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
