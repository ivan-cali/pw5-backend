package Its.incom.pw5.rest.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;

@Provider
public class GlobalLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(GlobalLoggingFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOG.infof("Incoming request: %s %s", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri());
        LOG.debugf("Headers: %s", requestContext.getHeaders());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        LOG.infof("Outgoing response: %s, Status: %d", requestContext.getUriInfo().getRequestUri(), responseContext.getStatus());
        LOG.debugf("Response headers: %s", responseContext.getHeaders());
    }
}
