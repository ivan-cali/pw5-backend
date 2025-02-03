package Its.incom.pw5.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

@Interceptor
@GlobalLog
public class GlobalLoggingInterceptor {

    private static final Logger LOG = Logger.getLogger(GlobalLoggingInterceptor.class);

    @AroundInvoke
    public Object logMethodCall(InvocationContext context) throws Exception {
        LOG.infof("Entering method: %s", context.getMethod().getName());
        Object result = context.proceed();
        LOG.infof("Exiting method: %s with result: %s", context.getMethod().getName(), result);
        return result;
    }
}
