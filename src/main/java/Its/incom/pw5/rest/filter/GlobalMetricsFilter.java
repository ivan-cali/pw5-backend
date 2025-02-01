package Its.incom.pw5.rest.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.logging.Logger;
import jakarta.inject.Inject;

@Provider
public class GlobalMetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(GlobalMetricsFilter.class);

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        metricRegistry.counter("global_api_calls_total").inc();
        LOG.infof("Incoming request: %s %s", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        LOG.infof("Outgoing response: %s, Status: %d", requestContext.getUriInfo().getRequestUri(), responseContext.getStatus());
    }
}
