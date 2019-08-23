/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 *
 * @author dewall
 */
class BaseUtils {
    /**
     * Private empty constructor.
     */
    private BaseUtils(){
    }


    /**
     *
     * @param clazz
     * @param annotation
     * @return
     */
    protected static Set<Field> findAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation){
        Set<Field> result = new HashSet<>();
        Field[] declaredFields = clazz.getDeclaredFields();

        for (Field field : declaredFields){
            if (field.isAnnotationPresent(annotation)){
                result.add(field);
            }
        }

        Class<?> parent = clazz.getSuperclass();
        if (parent != null){
            declaredFields = parent.getDeclaredFields();
            for (Field field : declaredFields){
                if (field.isAnnotationPresent(annotation)){
                    result.add(field);
                }
            }
        }
        return result;
    }
}
