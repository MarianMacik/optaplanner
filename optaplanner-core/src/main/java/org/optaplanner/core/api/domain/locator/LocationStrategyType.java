/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.api.domain.locator;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.impl.score.director.ScoreDirector;

/**
 * Determines how {@link ScoreDirector#locateWorkingObject(Object)} to map
 * a {@link ProblemFactCollectionProperty problem fact} or a{@link PlanningEntity planning entity}
 * from an external copy to the internal one.
 */
public enum LocationStrategyType {
    /**
     * Map by the same {@link PlanningId} field or method.
     * If there is no such field or method,
     * there is no mapping and {@link ScoreDirector#locateWorkingObject(Object)} must not be used.
     * If there is such a field or method, but it returns null, it fails fast.
     * <p>
     * This is the default.
     */
    PLANNING_ID_OR_NONE,
    /**
     * Map by the same {@link PlanningId} field or method.
     * If there is no such field or method, it fails fast.
     */
    PLANNING_ID_OR_FAIL_FAST,
    /**
     * Map by {@link Object#equals(Object)} and {@link Object#hashCode()}.
     * If there is no such field or method,
     * there is no mapping and {@link ScoreDirector#locateWorkingObject(Object)} must not be used.
     * <p>
     * This is the default.
     */
    EQUALITY,
    /**
     * There is no mapping and {@link ScoreDirector#locateWorkingObject(Object)} must not be used.
     */
    NONE;

}
