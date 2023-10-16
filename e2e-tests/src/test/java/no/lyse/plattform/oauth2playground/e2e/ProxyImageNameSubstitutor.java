package no.lyse.plattform.oauth2playground.e2e;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;
import org.testcontainers.utility.TestcontainersConfiguration;

public class ProxyImageNameSubstitutor extends ImageNameSubstitutor {
    @Override
    public DockerImageName apply(DockerImageName original) {
        String repo = TestcontainersConfiguration.getInstance().getEnvVarOrProperty("testcontainers.image.registry", null);
        if (repo != null && !repo.isBlank()) {
            return original.withRegistry(repo);
        } else {
            return original;
        }
    }

    @Override
    protected String getDescription() {
        return "Image Registry prefix ";
    }
}
