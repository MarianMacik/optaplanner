/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.heuristic.selector.move.generic.chained;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import org.optaplanner.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.value.chained.SubChain;
import org.optaplanner.core.impl.score.director.ScoreDirector;

/**
 * This {@link Move} is not cacheable.
 */
public class SubChainSwapMove extends AbstractMove {

    protected final GenuineVariableDescriptor variableDescriptor;
    protected final SingletonInverseVariableSupply inverseVariableSupply;

    protected final SubChain leftSubChain;
    protected final SubChain rightSubChain;

    public SubChainSwapMove(GenuineVariableDescriptor variableDescriptor, SingletonInverseVariableSupply inverseVariableSupply,
            SubChain leftSubChain, SubChain rightSubChain) {
        this.variableDescriptor = variableDescriptor;
        this.inverseVariableSupply = inverseVariableSupply;
        this.leftSubChain = leftSubChain;
        this.rightSubChain = rightSubChain;
    }

    public String getVariableName() {
        return variableDescriptor.getVariableName();
    }

    public SubChain getLeftSubChain() {
        return leftSubChain;
    }

    public SubChain getRightSubChain() {
        return rightSubChain;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public boolean isMoveDoable(ScoreDirector scoreDirector) {
        for (Object leftEntity : leftSubChain.getEntityList()) {
            if (rightSubChain.getEntityList().contains(leftEntity)) {
                return false;
            }
        }
        // Because leftFirstEntity and rightFirstEntity are unequal, chained guarantees their values are unequal too.
        return true;
    }

    @Override
    public Move createUndoMove(ScoreDirector scoreDirector) {
        return new SubChainSwapMove(variableDescriptor, inverseVariableSupply,
                rightSubChain, leftSubChain);
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector scoreDirector) {
        Object leftFirstEntity = leftSubChain.getFirstEntity();
        Object leftFirstValue = variableDescriptor.getValue(leftFirstEntity);
        Object leftLastEntity = leftSubChain.getLastEntity();
        Object leftTrailingLastEntity = inverseVariableSupply.getInverseSingleton(leftLastEntity);
        Object rightFirstEntity = rightSubChain.getFirstEntity();
        Object rightFirstValue = variableDescriptor.getValue(rightFirstEntity);
        Object rightLastEntity = rightSubChain.getLastEntity();
        Object rightTrailingLastEntity = inverseVariableSupply.getInverseSingleton(rightLastEntity);
        // Change the entities
        if (leftLastEntity != rightFirstValue) {
            scoreDirector.changeVariableFacade(variableDescriptor, leftFirstEntity, rightFirstValue);
        }
        if (rightLastEntity != leftFirstValue) {
            scoreDirector.changeVariableFacade(variableDescriptor, rightFirstEntity, leftFirstValue);
        }
        // Reroute the new chains
        if (leftTrailingLastEntity != null) {
            if (leftTrailingLastEntity != rightFirstEntity) {
                scoreDirector.changeVariableFacade(variableDescriptor, leftTrailingLastEntity, rightLastEntity);
            } else {
                scoreDirector.changeVariableFacade(variableDescriptor, leftFirstEntity, rightLastEntity);
            }
        }
        if (rightTrailingLastEntity != null) {
            if (rightTrailingLastEntity != leftFirstEntity) {
                scoreDirector.changeVariableFacade(variableDescriptor, rightTrailingLastEntity, leftLastEntity);
            } else {
                scoreDirector.changeVariableFacade(variableDescriptor, rightFirstEntity, leftLastEntity);
            }
        }
    }

    // ************************************************************************
    // Introspection methods
    // ************************************************************************

    @Override
    public String getSimpleMoveTypeDescription() {
        return getClass().getSimpleName() + "(" + variableDescriptor.getSimpleEntityAndVariableName() + ")";
    }

    @Override
    public Collection<? extends Object> getPlanningEntities() {
        List<Object> entities = new ArrayList<>(
                leftSubChain.getSize() + rightSubChain.getSize());
        entities.addAll(leftSubChain.getEntityList());
        entities.addAll(rightSubChain.getEntityList());
        return entities;
    }

    @Override
    public Collection<? extends Object> getPlanningValues() {
        List<Object> values = new ArrayList<>(2);
        values.add(variableDescriptor.getValue(leftSubChain.getFirstEntity()));
        values.add(variableDescriptor.getValue(rightSubChain.getFirstEntity()));
        return values;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof SubChainSwapMove) {
            SubChainSwapMove other = (SubChainSwapMove) o;
            return new EqualsBuilder()
                    .append(variableDescriptor, other.variableDescriptor)
                    .append(leftSubChain, other.leftSubChain)
                    .append(rightSubChain, other.rightSubChain)
                    .isEquals();
        } else {
            return false;
        }
    }

    public int hashCode() {
        return new HashCodeBuilder()
                .append(variableDescriptor)
                .append(leftSubChain)
                .append(rightSubChain)
                .toHashCode();
    }

    public String toString() {
        Object oldLeftValue = variableDescriptor.getValue(leftSubChain.getFirstEntity());
        Object oldRightValue = variableDescriptor.getValue(rightSubChain.getFirstEntity());
        return leftSubChain.toDottedString() + " {" + oldLeftValue + "} <-> "
                + rightSubChain.toDottedString() + " {" + oldRightValue + "}";
    }

}
