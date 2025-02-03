package Its.incom.pw5.service;

import Its.incom.pw5.interceptor.GlobalLog;
import io.vertx.ext.auth.impl.hash.SHA512;
import jakarta.enterprise.context.ApplicationScoped;

@GlobalLog
@ApplicationScoped
public class HashCalculator {
    public String calculateHash(String password) {
        SHA512 algoritmo = new SHA512();

        return algoritmo.hash(null, password);
    }


}
