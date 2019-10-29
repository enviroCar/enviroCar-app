package org.envirocar.core.injection;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

/**
 * @author dewall
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectIOScheduler {
}
