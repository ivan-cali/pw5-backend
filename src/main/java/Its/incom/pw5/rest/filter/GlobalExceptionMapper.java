package Its.incom.pw5.rest.filter;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("Unhandled exception occurred", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("An internal error occurred")
                .build();
    }
}
