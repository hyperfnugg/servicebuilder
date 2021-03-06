package jante.addon;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;
import jante.Injections;
import jante.exception.*;
import jante.model.Addon;
import jante.util.GuavaHelper;

import javax.ws.rs.NotFoundException;

/**
 * Legger til et sett med standard exceptionmappere for Jersey som mapper til problem response.
 * Logger stacktrace for de fleste exceptions, med unntak av exceptions og underexceptions satt til false i config.stacktraceConfig.
 * Config.logAllStackTraces er ment for debug-bruk.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExceptionMapperAddon implements Addon {

    @Wither(AccessLevel.PRIVATE)
    public final ImmutableMap<Class<?>, Boolean> stacktraceConfig;

    public static ExceptionMapperAddon exceptionMapperAddon = new ExceptionMapperAddon(
            ImmutableMap.<Class<?>, Boolean>builder()
                    .put(Throwable.class, true)
                    .put(NotFoundException.class, false)
                    .build()
    );


    @Override
    public Injections getInjections() {
        return Injections.injections
                .register(JsonProcessingExceptionMapper.class)
                .register(RuntimeExceptionMapper.class)
                .register(ValidationExceptionMapper.class)
                .register(WebApplicationExceptionMapper.class)
                .register(ConstraintViolationExceptionMapper.class)
                .register(ExternalResourceExceptionMapper.class)
                .register(UserMessageExceptionMapper.class)
                .register(HttpProblemExceptionMapper.class)
                .bind(this, ExceptionMapperAddon.class)
                .bind(GenericExceptionHandler.class)
                ;
    }

    public ExceptionMapperAddon stacktraceConfig(Class<?> key, boolean value) {
        return this.withStacktraceConfig(GuavaHelper.plus(stacktraceConfig, key, value));
    }
}
