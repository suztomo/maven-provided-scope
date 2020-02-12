package suztomo;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.project.ProjectModelResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.junit.Test;

/** Unit test for simple App. */
public class TestProjectDependencyResolver {
  private static RepositorySystem system = newRepositorySystem();
  private static DefaultRepositorySystemSession session =
      createDefaultRepositorySystemSession(system);

  private static final RemoteRepository CENTRAL =
      new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build();

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

  private static PlexusContainer createPlexusContainer() throws PlexusContainerException {
    // MavenCli's way to instantiate PlexusContainer
    ClassWorld classWorld =
        new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
    ContainerConfiguration containerConfiguration =
        new DefaultContainerConfiguration()
            .setClassWorld(classWorld)
            .setRealm(classWorld.getClassRealm("plexus.core"))
            .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
            .setAutoWiring(true)
            .setJSR250Lifecycle(true)
            .setName("TestProjectDependencyResolver");
    PlexusContainer container = new DefaultPlexusContainer(containerConfiguration);
    return container;
  }

  private static Model createModel(String coordinates)
      throws ModelBuildingException, ArtifactResolutionException {

    ArtifactRequest artifactRequest = new ArtifactRequest();
    artifactRequest.setArtifact(new DefaultArtifact(coordinates));
    ArtifactResult artifactResult = system.resolveArtifact(session, artifactRequest);

    ModelBuildingRequest modelRequest = new DefaultModelBuildingRequest();
    modelRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
    modelRequest.setProcessPlugins(false);
    modelRequest.setTwoPhaseBuilding(false);

    File file = artifactResult.getArtifact().getFile();
    modelRequest.setPomFile(file);
    modelRequest.setModelResolver(
        new ProjectModelResolver(
            session,
            null,
            system,
            new DefaultRemoteRepositoryManager(),
            ImmutableList.of(CENTRAL), // Needed when parent pom is not locally available
            null,
            null));
    modelRequest.setSystemProperties(System.getProperties());

    DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
    ModelBuildingResult modelBuildingResult = modelBuilder.build(modelRequest);
    Model model = modelBuildingResult.getEffectiveModel();
    return model;
  }

  @Test
  public void testWithProjectDependenciesResolver() throws Exception {
    PlexusContainer container = createPlexusContainer();
    ProjectDependenciesResolver resolver = container.lookup(ProjectDependenciesResolver.class);

    DependencyResolutionRequest resolutionRequest = new DefaultDependencyResolutionRequest();
    resolutionRequest.setRepositorySession(session);
    MavenProject mavenProject = new MavenProject();
    mavenProject.setModel(createModel("jaxen:jaxen:pom:1.1.6"));
    resolutionRequest.setMavenProject(mavenProject);

    DependencyResolutionResult resolutionResult = resolver.resolve(resolutionRequest);
    DependencyNode node = resolutionResult.getDependencyGraph();

    // This passes.
    // [dom4j:dom4j:jar:1.6.1 (compile?), jdom:jdom:jar:1.0 (compile?), xml-apis:xml-apis:jar:1.3.02 (provided), xerces:xercesImpl:jar:2.6.2 (provided), xom:xom:jar:1.0 (compile?), junit:junit:jar:3.8.2 (test)]
    Truth.assertThat(node.getChildren()).hasSize(6);
  }
}
