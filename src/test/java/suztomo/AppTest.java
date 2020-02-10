package suztomo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.truth.Truth;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
  private static RepositorySystem system = newRepositorySystem();
  private static DefaultRepositorySystemSession session = createDefaultRepositorySystemSession(system);

  private static RepositorySystem newRepositorySystem() {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

    return locator.getService(RepositorySystem.class);
  }

  private static DefaultRepositorySystemSession createDefaultRepositorySystemSession(
      RepositorySystem system) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    Path home = Paths.get(System.getProperty("user.home"));
    Path localRepo = home.resolve(".m2").resolve("repository");
    LocalRepository localRepository = new LocalRepository(localRepo.toFile());
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));
    return session;
  }

  @Test
  public void shouldIncludeProvidedScope() throws Exception{
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(new DefaultArtifact("jaxen:jaxen:1.1.6"), "compile"));
    CollectResult collectResult = system.collectDependencies(session, collectRequest);
    DependencyNode node = collectResult.getRoot();

    // Jaxen:1.1.6 declares following dependencies:
    // - dom4j (compile, optional)
    // - jdom (compile, optional)
    // - xml-apis (provided)
    // - xercesImpl (provided)
    // - xom (compile, optional)
    // - junit (test)
    // https://search.maven.org/artifact/jaxen/jaxen/1.1.6/bundle

    // This fails because the node does not have provided dependencies. It includes optional
    // dependencies:
    Truth.assertThat(node.getChildren()).hasSize(5);
  }
}
