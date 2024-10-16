package io.apicurio.registry.operator.resource.app;

import io.apicurio.registry.operator.OperatorException;
import io.apicurio.registry.operator.api.v1.ApicurioRegistry3;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.apicurio.registry.operator.resource.LabelDiscriminators.AppDeploymentDiscriminator;
import static io.apicurio.registry.operator.resource.ResourceFactory.APP_CONTAINER_NAME;
import static io.apicurio.registry.operator.resource.ResourceFactory.COMPONENT_APP;
import static io.apicurio.registry.operator.resource.ResourceKey.APP_DEPLOYMENT_KEY;
import static io.apicurio.registry.operator.utils.Mapper.toYAML;
import static java.util.Objects.requireNonNull;

// spotless:off
@KubernetesDependent(
        labelSelector = "app.kubernetes.io/name=apicurio-registry,app.kubernetes.io/component=" + COMPONENT_APP,
        resourceDiscriminator = AppDeploymentDiscriminator.class
)
// spotless:on
public class AppDeploymentResource extends CRUDKubernetesDependentResource<Deployment, ApicurioRegistry3> {

    private static final Logger log = LoggerFactory.getLogger(AppDeploymentResource.class);

    public AppDeploymentResource() {
        super(Deployment.class);
    }

    @Override
    protected Deployment desired(ApicurioRegistry3 primary, Context<ApicurioRegistry3> context) {

        var d = APP_DEPLOYMENT_KEY.getFactory().apply(primary);

        var envVars = new LinkedHashMap<String, EnvVar>();
        primary.getSpec().getApp().getEnv().forEach(e -> {
            envVars.put(e.getName(), e);
        });

        // spotless:off
        addEnvVar(envVars, new EnvVarBuilder().withName("QUARKUS_PROFILE").withValue("prod").build());
        addEnvVar(envVars, new EnvVarBuilder().withName("APICURIO_CONFIG_CACHE_ENABLED").withValue("true").build());
        addEnvVar(envVars, new EnvVarBuilder().withName("QUARKUS_HTTP_ACCESS_LOG_ENABLED").withValue("true").build());
        addEnvVar(envVars, new EnvVarBuilder().withName("QUARKUS_HTTP_CORS_ORIGINS").withValue("*").build());
        addEnvVar(envVars, new EnvVarBuilder().withName("APICURIO_REST_DELETION_GROUP_ENABLED").withValue("true").build());
        addEnvVar(envVars, new EnvVarBuilder().withName("APICURIO_REST_DELETION_ARTIFACT_ENABLED").withValue("true").build());
        addEnvVar(envVars, new EnvVarBuilder().withName("APICURIO_REST_DELETION_ARTIFACTVERSION_ENABLED").withValue("true").build());
        addEnvVar(envVars, new EnvVarBuilder().withName("APICURIO_APIS_V2_DATE_FORMAT").withValue("yyyy-MM-dd''T''HH:mm:ssZ").build());
        // spotless:on

        var container = getContainer(d, APP_CONTAINER_NAME);
        container.setEnv(envVars.values().stream().toList());

        log.debug("Desired {} is {}", APP_DEPLOYMENT_KEY.getId(), toYAML(d));
        return d;
    }

    public static void addEnvVar(Map<String, EnvVar> map, EnvVar envVar) {
        if (!map.containsKey(envVar.getName())) {
            map.put(envVar.getName(), envVar);
        }
    }

    public static Container getContainer(Deployment d, String name) {
        requireNonNull(d);
        requireNonNull(name);
        for (var c : d.getSpec().getTemplate().getSpec().getContainers()) {
            if (name.equals(c.getName())) {
                return c;
            }
        }
        throw new OperatorException(
                "Container %s not found in Deployment %s".formatted(name, ResourceID.fromResource(d)));
    }
}
