/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.impl.domain.solution.descriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Iterators;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningEntityProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.Solution;
import org.optaplanner.core.api.domain.solution.cloner.SolutionCloner;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.AbstractBendableScore;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.buildin.bendable.BendableScore;
import org.optaplanner.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import org.optaplanner.core.api.score.buildin.bendablelong.BendableLongScore;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore;
import org.optaplanner.core.api.score.buildin.hardsoftdouble.HardSoftDoubleScore;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import org.optaplanner.core.api.score.buildin.simpledouble.SimpleDoubleScore;
import org.optaplanner.core.api.score.buildin.simplelong.SimpleLongScore;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.domain.common.accessor.BeanPropertyMemberAccessor;
import org.optaplanner.core.impl.domain.common.accessor.MemberAccessor;
import org.optaplanner.core.impl.domain.entity.descriptor.EntityDescriptor;
import org.optaplanner.core.impl.domain.locator.LocationStrategyResolver;
import org.optaplanner.core.impl.domain.policy.DescriptorPolicy;
import org.optaplanner.core.impl.domain.solution.AbstractSolution;
import org.optaplanner.core.impl.domain.solution.cloner.FieldAccessingSolutionCloner;
import org.optaplanner.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import org.optaplanner.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import org.optaplanner.core.impl.domain.variable.descriptor.VariableDescriptor;
import org.optaplanner.core.impl.score.buildin.bendable.BendableScoreDefinition;
import org.optaplanner.core.impl.score.buildin.bendablebigdecimal.BendableBigDecimalScoreDefinition;
import org.optaplanner.core.impl.score.buildin.bendablelong.BendableLongScoreDefinition;
import org.optaplanner.core.impl.score.buildin.hardmediumsoft.HardMediumSoftScoreDefinition;
import org.optaplanner.core.impl.score.buildin.hardmediumsoftlong.HardMediumSoftLongScoreDefinition;
import org.optaplanner.core.impl.score.buildin.hardsoft.HardSoftScoreDefinition;
import org.optaplanner.core.impl.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScoreDefinition;
import org.optaplanner.core.impl.score.buildin.hardsoftdouble.HardSoftDoubleScoreDefinition;
import org.optaplanner.core.impl.score.buildin.hardsoftlong.HardSoftLongScoreDefinition;
import org.optaplanner.core.impl.score.buildin.simple.SimpleScoreDefinition;
import org.optaplanner.core.impl.score.buildin.simplebigdecimal.SimpleBigDecimalScoreDefinition;
import org.optaplanner.core.impl.score.buildin.simpledouble.SimpleDoubleScoreDefinition;
import org.optaplanner.core.impl.score.buildin.simplelong.SimpleLongScoreDefinition;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.optaplanner.core.config.util.ConfigUtils.MemberAccessorType.*;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SolutionDescriptor<Solution_> {

    public static <Solution_> SolutionDescriptor<Solution_> buildSolutionDescriptor(Class<Solution_> solutionClass,
            Class<?>... entityClasses) {
        return buildSolutionDescriptor(solutionClass, Arrays.asList(entityClasses), null);
    }

    public static <Solution_> SolutionDescriptor<Solution_> buildSolutionDescriptor(Class<Solution_> solutionClass,
            List<Class<?>> entityClassList, ScoreDefinition deprecatedScoreDefinition) {
        DescriptorPolicy descriptorPolicy = new DescriptorPolicy();
        SolutionDescriptor<Solution_> solutionDescriptor = new SolutionDescriptor<>(solutionClass);
        solutionDescriptor.processAnnotations(descriptorPolicy, deprecatedScoreDefinition);
        for (Class<?> entityClass : sortEntityClassList(entityClassList)) {
            EntityDescriptor<Solution_> entityDescriptor = new EntityDescriptor<>(solutionDescriptor, entityClass);
            solutionDescriptor.addEntityDescriptor(entityDescriptor);
            entityDescriptor.processAnnotations(descriptorPolicy);
        }
        solutionDescriptor.afterAnnotationsProcessed(descriptorPolicy);
        return solutionDescriptor;
    }

    private static List<Class<?>> sortEntityClassList(List<Class<?>> entityClassList) {
        List<Class<?>> sortedEntityClassList = new ArrayList<>(entityClassList.size());
        for (Class<?> entityClass : entityClassList) {
            boolean added = false;
            for (int i = 0; i < sortedEntityClassList.size(); i++) {
                Class<?> sortedEntityClass = sortedEntityClassList.get(i);
                if (entityClass.isAssignableFrom(sortedEntityClass)) {
                    sortedEntityClassList.add(i, entityClass);
                    added = true;
                    break;
                }
            }
            if (!added) {
                sortedEntityClassList.add(entityClass);
            }
        }
        return sortedEntityClassList;
    }

    // ************************************************************************
    // Non-static members
    // ************************************************************************

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private final Class<Solution_> solutionClass;
    private SolutionCloner<Solution_> solutionCloner;

    private final Map<String, MemberAccessor> problemFactMemberAccessorMap;
    private final Map<String, MemberAccessor> problemFactCollectionMemberAccessorMap;
    private final Map<String, MemberAccessor> entityMemberAccessorMap;
    private final Map<String, MemberAccessor> entityCollectionMemberAccessorMap;
    private MemberAccessor scoreMemberAccessor;
    private ScoreDefinition scoreDefinition;

    private final Map<Class<?>, EntityDescriptor<Solution_>> entityDescriptorMap;
    private final List<Class<?>> reversedEntityClassList;

    private final ConcurrentMap<Class<?>, EntityDescriptor<Solution_>> lowestEntityDescriptorCache = new ConcurrentHashMap<>();
    private LocationStrategyResolver locationStrategyResolver = null;

    public SolutionDescriptor(Class<Solution_> solutionClass) {
        this.solutionClass = solutionClass;
        problemFactMemberAccessorMap = new LinkedHashMap<>();
        problemFactCollectionMemberAccessorMap = new LinkedHashMap<>();
        entityMemberAccessorMap = new LinkedHashMap<>();
        entityCollectionMemberAccessorMap = new LinkedHashMap<>();
        entityDescriptorMap = new LinkedHashMap<>();
        reversedEntityClassList = new ArrayList<>();
    }

    public void addEntityDescriptor(EntityDescriptor<Solution_> entityDescriptor) {
        Class<?> entityClass = entityDescriptor.getEntityClass();
        for (Class<?> otherEntityClass : entityDescriptorMap.keySet()) {
            if (entityClass.isAssignableFrom(otherEntityClass)) {
                throw new IllegalArgumentException("An earlier entityClass (" + otherEntityClass
                        + ") should not be a subclass of a later entityClass (" + entityClass
                        + "). Switch their declaration so superclasses are defined earlier.");
            }
        }
        entityDescriptorMap.put(entityClass, entityDescriptor);
        reversedEntityClassList.add(0, entityClass);
        lowestEntityDescriptorCache.put(entityClass, entityDescriptor);
    }

    public void processAnnotations(DescriptorPolicy descriptorPolicy, ScoreDefinition deprecatedScoreDefinition) {
        processSolutionAnnotations(descriptorPolicy);
        // Iterate inherited members too (unlike for EntityDescriptor where each one is declared)
        // to make sure each one is registered
        for (Class<?> lineageClass : ConfigUtils.getAllAnnotatedLineageClasses(solutionClass, PlanningSolution.class)) {
            List<Member> memberList = ConfigUtils.getDeclaredMembers(lineageClass);
            for (Member member : memberList) {
                processValueRangeProviderAnnotation(descriptorPolicy, member);
                processProblemFactPropertyAnnotation(descriptorPolicy, member);
                processPlanningEntityPropertyAnnotation(descriptorPolicy, member);
                processScoreAnnotation(descriptorPolicy, member, deprecatedScoreDefinition);
            }
        }
        if (entityCollectionMemberAccessorMap.isEmpty() && entityMemberAccessorMap.isEmpty()) {
            throw new IllegalStateException("The solutionClass (" + solutionClass
                    + ") must have at least 1 member with a "
                    + PlanningEntityCollectionProperty.class.getSimpleName() + " annotation or a "
                    + PlanningEntityProperty.class.getSimpleName() + " annotation.");
        }
        if (Solution.class.isAssignableFrom(solutionClass)) {
            processLegacySolution(descriptorPolicy, deprecatedScoreDefinition);
            return;
        }
        // Do not check if problemFactCollectionMemberAccessorMap and problemFactMemberAccessorMap are empty
        // because they are only required for Drools score calculation.
        if (scoreMemberAccessor == null) {
            throw new IllegalStateException("The solutionClass (" + solutionClass
                    + ") must have 1 member with a " + PlanningScore.class.getSimpleName() + " annotation.\n"
                    + "Maybe add a getScore() method with a " + PlanningScore.class.getSimpleName() + " annotation.");
        }
    }

    private void processLegacySolution(DescriptorPolicy descriptorPolicy, ScoreDefinition deprecatedScoreDefinition) {
        if (!problemFactMemberAccessorMap.isEmpty()) {
            MemberAccessor memberAccessor = problemFactMemberAccessorMap.values().iterator().next();
            throw new IllegalStateException("The solutionClass (" + solutionClass
                    + ") which implements the legacy interface " + Solution.class.getSimpleName()
                    + ") must not have a member (" + memberAccessor.getName()
                    + ") with a " + ProblemFactProperty.class.getSimpleName() + " annotation.\n"
                    + "Maybe remove the use of the legacy interface.");
        }
        if (!problemFactCollectionMemberAccessorMap.isEmpty()) {
            MemberAccessor memberAccessor = problemFactCollectionMemberAccessorMap.values().iterator().next();
            throw new IllegalStateException("The solutionClass (" + solutionClass
                    + ") which implements the legacy interface " + Solution.class.getSimpleName()
                    + ") must not have a member (" + memberAccessor.getName()
                    + ") with a " + ProblemFactCollectionProperty.class.getSimpleName() + " annotation.\n"
                    + "Maybe remove the use of the legacy interface.");
        }
        try {
            Method getProblemFactsMethod = solutionClass.getMethod("getProblemFacts");
            MemberAccessor problemFactsMemberAccessor = new BeanPropertyMemberAccessor(getProblemFactsMethod);
            problemFactCollectionMemberAccessorMap.put(
                    problemFactsMemberAccessor.getName(), problemFactsMemberAccessor);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Impossible situation: the solutionClass (" + solutionClass
                    + ") which implements the legacy interface " + Solution.class.getSimpleName()
                    + ", lacks its getProblemFacts() method.", e);
        }
        if (scoreMemberAccessor != null) {
            throw new IllegalStateException("The solutionClass (" + solutionClass
                    + ") which implements the legacy interface " + Solution.class.getSimpleName()
                    + ") must not have a member (" + scoreMemberAccessor.getName()
                    + ") with a " + PlanningScore.class.getSimpleName() + " annotation.\n"
                    + "Maybe remove the use of the legacy interface.");
        }
        try {
            Method getScoreMethod = solutionClass.getMethod("getScore");
            scoreMemberAccessor = new BeanPropertyMemberAccessor(getScoreMethod);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Impossible situation: the solutionClass (" + solutionClass
                    + ") which implements the legacy interface " + Solution.class.getSimpleName()
                    + ", lacks its getScore() method.", e);
        }
        if (deprecatedScoreDefinition == null) {
            deprecatedScoreDefinition = new SimpleScoreDefinition();
        }
        scoreDefinition = deprecatedScoreDefinition;
        Class<? extends Score> scoreClass = extractScoreClass();
        if (!scoreClass.isAssignableFrom(scoreDefinition.getScoreClass())) {
            throw new IllegalArgumentException("The scoreClass (" + scoreClass
                    + ") of solutionClass (" + solutionClass
                    + ") is not the same or a superclass as the scoreDefinition's scoreClass ("
                    + scoreDefinition.getScoreClass() + ").");
        }
    }

    /**
     * Only called if Drools score calculation is used.
     */
    public void checkIfProblemFactsExist() {
        if (problemFactCollectionMemberAccessorMap.isEmpty() && problemFactMemberAccessorMap.isEmpty()) {
            throw new IllegalStateException("The solutionClass (" + solutionClass
                    + ") must have at least 1 member with a "
                    + ProblemFactCollectionProperty.class.getSimpleName() + " annotation or a "
                    + ProblemFactProperty.class.getSimpleName() + " annotation"
                    + " when used with Drools score calculation.");
        }
    }

    private void processSolutionAnnotations(DescriptorPolicy descriptorPolicy) {
        PlanningSolution solutionAnnotation = solutionClass.getAnnotation(PlanningSolution.class);
        if (solutionAnnotation == null) {
            throw new IllegalStateException("The solutionClass (" + solutionClass
                    + ") has been specified as a solution in the configuration," +
                    " but does not have a " + PlanningSolution.class.getSimpleName() + " annotation.");
        }
        processSolutionCloner(descriptorPolicy, solutionAnnotation);
        locationStrategyResolver = new LocationStrategyResolver(solutionAnnotation.locationStrategyType());
    }

    private void processSolutionCloner(DescriptorPolicy descriptorPolicy, PlanningSolution solutionAnnotation) {
        Class<? extends SolutionCloner> solutionClonerClass = solutionAnnotation.solutionCloner();
        if (solutionClonerClass == PlanningSolution.NullSolutionCloner.class) {
            solutionClonerClass = null;
        }
        if (solutionClonerClass != null) {
            solutionCloner = ConfigUtils.newInstance(this, "solutionClonerClass", solutionClonerClass);
        } else {
            solutionCloner = new FieldAccessingSolutionCloner<>(this);
        }
    }

    private void processValueRangeProviderAnnotation(DescriptorPolicy descriptorPolicy, Member member) {
        if (((AnnotatedElement) member).isAnnotationPresent(ValueRangeProvider.class)) {
            MemberAccessor memberAccessor = ConfigUtils.buildMemberAccessor(
                    member, FIELD_OR_READ_METHOD, ValueRangeProvider.class);
            descriptorPolicy.addFromSolutionValueRangeProvider(memberAccessor);
        }
    }

    private void processProblemFactPropertyAnnotation(DescriptorPolicy descriptorPolicy, Member member) {
        Class<? extends Annotation> annotationClass = ConfigUtils.extractAnnotationClass(member,
                ProblemFactProperty.class, ProblemFactCollectionProperty.class);
        if (annotationClass != null) {
            MemberAccessor memberAccessor = ConfigUtils.buildMemberAccessor(
                    member, FIELD_OR_READ_METHOD, annotationClass);
            assertUnexistingProblemFactOrPlanningEntityProperty(memberAccessor, annotationClass);
            if (annotationClass == ProblemFactProperty.class) {
                problemFactMemberAccessorMap.put(memberAccessor.getName(), memberAccessor);
            } else if (annotationClass == ProblemFactCollectionProperty.class) {
                if (!Collection.class.isAssignableFrom(memberAccessor.getType())) {
                    throw new IllegalStateException("The solutionClass (" + solutionClass
                            + ") has a " + ProblemFactCollectionProperty.class.getSimpleName()
                            + " annotated member (" + member + ") that does not return a "
                            + Collection.class.getSimpleName() + ".");
                }
                problemFactCollectionMemberAccessorMap.put(memberAccessor.getName(), memberAccessor);
            }
        }
    }

    private void processPlanningEntityPropertyAnnotation(DescriptorPolicy descriptorPolicy, Member member) {
        Class<? extends Annotation> annotationClass = ConfigUtils.extractAnnotationClass(member,
                PlanningEntityProperty.class, PlanningEntityCollectionProperty.class);
        if (annotationClass != null) {
            MemberAccessor memberAccessor = ConfigUtils.buildMemberAccessor(
                    member, FIELD_OR_GETTER_METHOD, annotationClass);
            assertUnexistingProblemFactOrPlanningEntityProperty(memberAccessor, annotationClass);
            if (annotationClass == PlanningEntityProperty.class) {
                entityMemberAccessorMap.put(memberAccessor.getName(), memberAccessor);
            } else if (annotationClass == PlanningEntityCollectionProperty.class) {
                if (!Collection.class.isAssignableFrom(memberAccessor.getType())) {
                    throw new IllegalStateException("The solutionClass (" + solutionClass
                            + ") has a " + PlanningEntityCollectionProperty.class.getSimpleName()
                            + " annotated member (" + member + ") that does not return a "
                            + Collection.class.getSimpleName() + ".");
                }
                entityCollectionMemberAccessorMap.put(memberAccessor.getName(), memberAccessor);
            }
        }
    }

    private void assertUnexistingProblemFactOrPlanningEntityProperty(
            MemberAccessor memberAccessor, Class<? extends Annotation> annotationClass) {
        MemberAccessor duplicate;
        Class<? extends Annotation> otherAnnotationClass;
        String memberName = memberAccessor.getName();
        if (problemFactMemberAccessorMap.containsKey(memberName)) {
            duplicate = problemFactMemberAccessorMap.get(memberName);
            otherAnnotationClass = ProblemFactProperty.class;
        } else if (problemFactCollectionMemberAccessorMap.containsKey(memberName)) {
            duplicate = problemFactCollectionMemberAccessorMap.get(memberName);
            otherAnnotationClass = ProblemFactCollectionProperty.class;
        } else if (entityMemberAccessorMap.containsKey(memberName)) {
            duplicate = entityMemberAccessorMap.get(memberName);
            otherAnnotationClass = PlanningEntityProperty.class;
        } else if (entityCollectionMemberAccessorMap.containsKey(memberName)) {
            duplicate = entityCollectionMemberAccessorMap.get(memberName);
            otherAnnotationClass = PlanningEntityCollectionProperty.class;
        } else {
            return;
        }
        throw new IllegalStateException("The solutionClass (" + solutionClass
                + ") has a " + annotationClass.getSimpleName()
                + " annotated member (" + memberAccessor
                + ") that is duplicated by a " + otherAnnotationClass.getSimpleName()
                + " annotated member (" + duplicate + ").\n"
                + (annotationClass.equals(otherAnnotationClass)
                ? "Maybe the annotation is defined on both the field and its getter."
                : "Maybe 2 mutually exclusive annotations are configured."));
    }

    private void processScoreAnnotation(DescriptorPolicy descriptorPolicy, Member member,
            ScoreDefinition deprecatedScoreDefinition) {
        if (((AnnotatedElement) member).isAnnotationPresent(PlanningScore.class)) {
            MemberAccessor memberAccessor = ConfigUtils.buildMemberAccessor(
                    member, FIELD_OR_GETTER_METHOD_WITH_SETTER, PlanningScore.class);
            if (deprecatedScoreDefinition != null) {
                throw new IllegalStateException("The solutionClass (" + solutionClass
                        + ") has a " + PlanningScore.class.getSimpleName()
                        + " annotated member (" + memberAccessor
                        + ") but the solver configuration still has a deprecated scoreDefinitionType"
                        + " or scoreDefinitionClass element.\n"
                        + "Maybe remove the <scoreDefinitionType>, <bendableHardLevelsSize>, <bendableSoftLevelsSize> and <scoreDefinitionClass> elements from the solver configuration.");
            }
            if (!Score.class.isAssignableFrom(memberAccessor.getType())) {
                throw new IllegalStateException("The solutionClass (" + solutionClass
                        + ") has a " + PlanningScore.class.getSimpleName()
                        + " annotated member (" + memberAccessor + ") that does not return a subtype of Score.");
            }
            if (scoreMemberAccessor != null) {
                if (!scoreMemberAccessor.getName().equals(memberAccessor.getName())
                        || !scoreMemberAccessor.getClass().equals(memberAccessor.getClass())) {
                    throw new IllegalStateException("The solutionClass (" + solutionClass
                            + ") has a " + PlanningScore.class.getSimpleName()
                            + " annotated member (" + memberAccessor
                            + ") that is duplicated by another member (" + scoreMemberAccessor + ").\n"
                            + "  Verify that the annotation is not defined on both the field and its getter.");
                }
                // Bottom class wins. Bottom classes are parsed first due to ConfigUtil.getAllAnnotatedLineageClasses()
                return;
            }
            scoreMemberAccessor = memberAccessor;
            Class<? extends Score> scoreType = (Class<? extends Score>) scoreMemberAccessor.getType();
            PlanningScore annotation = scoreMemberAccessor.getAnnotation(PlanningScore.class);
            scoreDefinition = buildScoreDefinition(scoreType, annotation);
        }
    }

    public ScoreDefinition buildScoreDefinition(Class<? extends Score> scoreType, PlanningScore annotation) {
        Class<? extends ScoreDefinition> scoreDefinitionClass = annotation.scoreDefinitionClass();
        if (scoreDefinitionClass != PlanningScore.NullScoreDefinition.class) {
            if (annotation.bendableHardLevelsSize() != PlanningScore.NO_LEVEL_SIZE
                    || annotation.bendableSoftLevelsSize() != PlanningScore.NO_LEVEL_SIZE) {
                throw new IllegalArgumentException("The solutionClass (" + solutionClass
                        + ") has a " + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that has a scoreDefinition (" + scoreDefinitionClass
                        + ") that must not have a bendableHardLevelsSize (" + annotation.bendableHardLevelsSize()
                        + ") or a bendableSoftLevelsSize (" + annotation.bendableSoftLevelsSize() + ").");
            }
            return ConfigUtils.newInstance(this, "scoreDefinitionClass", scoreDefinitionClass);
        }
        if (scoreType == Score.class) {
            if (!AbstractSolution.class.isAssignableFrom(solutionClass)) {
                throw new IllegalStateException("The solutionClass (" + solutionClass
                        + ") has a " + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that doesn't return a non-abstract " + Score.class.getSimpleName() + " class.\n"
                        + "Maybe make it return " + HardSoftScore.class.getSimpleName()
                        + " or another specific " + Score.class.getSimpleName() + " implementation.");
            } else {
                // Magic to support AbstractSolution
                if (solutionClass == AbstractSolution.class) {
                    throw new IllegalArgumentException(
                            "The solutionClass (" + solutionClass + ") cannot be directly a "
                            + AbstractSolution.class.getSimpleName() + ", but a subclass would be ok.");
                }
                Class baseClass = solutionClass;
                while (baseClass.getSuperclass() != AbstractSolution.class) {
                    baseClass = baseClass.getSuperclass();
                    if (baseClass == null) {
                        throw new IllegalStateException(
                                "Impossible situation because the solutionClass (" + solutionClass
                                + ") is assignable from " + AbstractSolution.class.getSimpleName() + ".");
                    }
                }
                Type genericAbstractSolution = solutionClass.getGenericSuperclass();
                if (!(genericAbstractSolution instanceof ParameterizedType)) {
                    throw new IllegalStateException(
                            "Impossible situation because the genericAbstractSolution (" + genericAbstractSolution
                                    + ") is a " + AbstractSolution.class.getSimpleName() + ".");
                }
                ParameterizedType parameterizedAbstractSolution = (ParameterizedType) genericAbstractSolution;
                Type[] typeArguments = parameterizedAbstractSolution.getActualTypeArguments();
                if (typeArguments.length != 1) {
                    throw new IllegalStateException(
                            "Impossible situation because the parameterizedAbstractSolution ("
                                    + parameterizedAbstractSolution
                                    + ") is a " + AbstractSolution.class.getSimpleName() + ".");
                }
                Type typeArgument = typeArguments[0];
                if (!(typeArgument instanceof Class)) {
                    throw new IllegalStateException(
                            "Impossible situation because a (" + AbstractSolution.class.getSimpleName()
                            + "'s typeArgument (" + typeArgument + ") must be a " + Score.class.getSimpleName() + ".");
                }
                scoreType = (Class<? extends Score>) typeArgument;
            }
        }
        if (!AbstractBendableScore.class.isAssignableFrom(scoreType)) {
            if (annotation.bendableHardLevelsSize() != PlanningScore.NO_LEVEL_SIZE
                    || annotation.bendableSoftLevelsSize() != PlanningScore.NO_LEVEL_SIZE) {
                throw new IllegalArgumentException("The solutionClass (" + solutionClass
                        + ") has a " + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that returns a scoreType (" + scoreType
                        + ") that must not have a bendableHardLevelsSize (" + annotation.bendableHardLevelsSize()
                        + ") or a bendableSoftLevelsSize (" + annotation.bendableSoftLevelsSize() + ").");
            }
            if (scoreType.equals(SimpleScore.class)) {
                return new SimpleScoreDefinition();
            } else if (scoreType.equals(SimpleLongScore.class)) {
                return new SimpleLongScoreDefinition();
            } else if (scoreType.equals(SimpleDoubleScore.class)) {
                return new SimpleDoubleScoreDefinition();
            } else if (scoreType.equals(SimpleBigDecimalScore.class)) {
                return new SimpleBigDecimalScoreDefinition();
            } else if (scoreType.equals(HardSoftScore.class)) {
                return new HardSoftScoreDefinition();
            } else if (scoreType.equals(HardSoftLongScore.class)) {
                return new HardSoftLongScoreDefinition();
            } else if (scoreType.equals(HardSoftDoubleScore.class)) {
                return new HardSoftDoubleScoreDefinition();
            } else if (scoreType.equals(HardSoftBigDecimalScore.class)) {
                return new HardSoftBigDecimalScoreDefinition();
            } else if (scoreType.equals(HardMediumSoftScore.class)) {
                return new HardMediumSoftScoreDefinition();
            } else if (scoreType.equals(HardMediumSoftLongScore.class)) {
                return new HardMediumSoftLongScoreDefinition();
            } else {
                throw new IllegalArgumentException("The solutionClass (" + solutionClass
                        + ") has a " + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that returns a scoreType (" + scoreType
                        + ") that is not recognized as a default " + Score.class.getSimpleName() + " implementation.\n"
                        + "  If you intend to use a custom implementation,"
                        + " maybe set a scoreDefinition in the " + PlanningScore.class.getSimpleName()
                        + " annotation.");
            }
        } else {
            int bendableHardLevelsSize = annotation.bendableHardLevelsSize();
            int bendableSoftLevelsSize = annotation.bendableSoftLevelsSize();
            if (bendableHardLevelsSize == PlanningScore.NO_LEVEL_SIZE
                    || bendableSoftLevelsSize == PlanningScore.NO_LEVEL_SIZE) {
                throw new IllegalArgumentException("The solutionClass (" + solutionClass
                        + ") has a " + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that returns a scoreType (" + scoreType
                        + ") that must have a bendableHardLevelsSize (" + annotation.bendableHardLevelsSize()
                        + ") and a bendableSoftLevelsSize (" + annotation.bendableSoftLevelsSize() + ").");
            }
            if (scoreType.equals(BendableScore.class)) {
                return new BendableScoreDefinition(bendableHardLevelsSize, bendableSoftLevelsSize);
            } else if (scoreType.equals(BendableLongScore.class)) {
                return new BendableLongScoreDefinition(bendableHardLevelsSize, bendableSoftLevelsSize);
            } else if (scoreType.equals(BendableBigDecimalScore.class)) {
                return new BendableBigDecimalScoreDefinition(bendableHardLevelsSize, bendableSoftLevelsSize);
            } else {
                throw new IllegalArgumentException("The solutionClass (" + solutionClass
                        + ") has a " + PlanningScore.class.getSimpleName()
                        + " annotated member (" + scoreMemberAccessor
                        + ") that returns a bendable scoreType (" + scoreType
                        + ") that is not recognized as a default " + Score.class.getSimpleName() + " implementation.\n"
                        + "  If you intend to use a custom implementation,"
                        + " maybe set a scoreDefinition in the annotation.");
            }
        }
    }

    public void afterAnnotationsProcessed(DescriptorPolicy descriptorPolicy) {
        for (EntityDescriptor<Solution_> entityDescriptor : entityDescriptorMap.values()) {
            entityDescriptor.linkInheritedEntityDescriptors(descriptorPolicy);
        }
        for (EntityDescriptor<Solution_> entityDescriptor : entityDescriptorMap.values()) {
            entityDescriptor.linkShadowSources(descriptorPolicy);
        }
        determineGlobalShadowOrder();
        if (logger.isTraceEnabled()) {
            logger.trace("    Model annotations parsed for Solution {}:", solutionClass.getSimpleName());
            for (Map.Entry<Class<?>, EntityDescriptor<Solution_>> entry : entityDescriptorMap.entrySet()) {
                EntityDescriptor<Solution_> entityDescriptor = entry.getValue();
                logger.trace("        Entity {}:", entityDescriptor.getEntityClass().getSimpleName());
                for (VariableDescriptor<Solution_> variableDescriptor : entityDescriptor.getDeclaredVariableDescriptors()) {
                    logger.trace("            Variable {} ({})", variableDescriptor.getVariableName(),
                            variableDescriptor instanceof GenuineVariableDescriptor ? "genuine" : "shadow");
                }
            }
        }
    }

    private void determineGlobalShadowOrder() {
        // Topological sorting with Kahn's algorithm
        List<Pair<ShadowVariableDescriptor, Integer>> pairList = new ArrayList<>();
        Map<ShadowVariableDescriptor, Pair<ShadowVariableDescriptor, Integer>> shadowToPairMap = new HashMap<>();
        for (EntityDescriptor<Solution_> entityDescriptor : entityDescriptorMap.values()) {
            for (ShadowVariableDescriptor<Solution_> shadow : entityDescriptor.getDeclaredShadowVariableDescriptors()) {
                int sourceSize = shadow.getSourceVariableDescriptorList().size();
                Pair<ShadowVariableDescriptor, Integer> pair = MutablePair.of(shadow, sourceSize);
                pairList.add(pair);
                shadowToPairMap.put(shadow, pair);
            }
        }
        for (EntityDescriptor<Solution_> entityDescriptor : entityDescriptorMap.values()) {
            for (GenuineVariableDescriptor<Solution_> genuine : entityDescriptor.getDeclaredGenuineVariableDescriptors()) {
                for (ShadowVariableDescriptor<Solution_> sink : genuine.getSinkVariableDescriptorList()) {
                    Pair<ShadowVariableDescriptor, Integer> sinkPair = shadowToPairMap.get(sink);
                    sinkPair.setValue(sinkPair.getValue() - 1);
                }
            }
        }
        int globalShadowOrder = 0;
        while (!pairList.isEmpty()) {
            Collections.sort(pairList, (a, b) -> Integer.compare(a.getValue(), b.getValue()));
            Pair<ShadowVariableDescriptor, Integer> pair = pairList.remove(0);
            ShadowVariableDescriptor<Solution_> shadow = pair.getKey();
            if (pair.getValue() != 0) {
                if (pair.getValue() < 0) {
                    throw new IllegalStateException("Impossible state because the shadowVariable ("
                            + shadow.getSimpleEntityAndVariableName()
                            + ") can not be used more as a sink than it has sources.");
                }
                throw new IllegalStateException("There is a cyclic shadow variable path"
                        + " that involves the shadowVariable (" + shadow.getSimpleEntityAndVariableName()
                        + ") because it must be later than its sources (" + shadow.getSourceVariableDescriptorList()
                        + ") and also earlier than its sinks (" + shadow.getSinkVariableDescriptorList() + ").");
            }
            for (ShadowVariableDescriptor<Solution_> sink : shadow.getSinkVariableDescriptorList()) {
                Pair<ShadowVariableDescriptor, Integer> sinkPair = shadowToPairMap.get(sink);
                sinkPair.setValue(sinkPair.getValue() - 1);
            }
            shadow.setGlobalShadowOrder(globalShadowOrder);
            globalShadowOrder++;
        }
    }

    public Class<Solution_> getSolutionClass() {
        return solutionClass;
    }

    /**
     * @return the {@link Class} of {@link PlanningScore}
     */
    public Class<? extends Score> extractScoreClass() {
        return (Class<? extends Score>) scoreMemberAccessor.getType();
    }

    public ScoreDefinition getScoreDefinition() {
        return scoreDefinition;
    }

    public SolutionCloner<Solution_> getSolutionCloner() {
        return solutionCloner;
    }

    public Map<String, MemberAccessor> getProblemFactMemberAccessorMap() {
        return problemFactMemberAccessorMap;
    }

    public Map<String, MemberAccessor> getProblemFactCollectionMemberAccessorMap() {
        return problemFactCollectionMemberAccessorMap;
    }

    public List<String> getProblemFactMemberAndProblemFactCollectionMemberNames() {
        List<String> memberNames = new ArrayList<>(problemFactMemberAccessorMap.size()
                + problemFactCollectionMemberAccessorMap.size());
        memberNames.addAll(problemFactMemberAccessorMap.keySet());
        memberNames.addAll(problemFactCollectionMemberAccessorMap.keySet());
        return memberNames;
    }

    public Map<String, MemberAccessor> getEntityMemberAccessorMap() {
        return entityMemberAccessorMap;
    }

    public Map<String, MemberAccessor> getEntityCollectionMemberAccessorMap() {
        return entityCollectionMemberAccessorMap;
    }

    public List<String> getEntityMemberAndEntityCollectionMemberNames() {
        List<String> memberNames = new ArrayList<>(entityMemberAccessorMap.size()
                + entityCollectionMemberAccessorMap.size());
        memberNames.addAll(entityMemberAccessorMap.keySet());
        memberNames.addAll(entityCollectionMemberAccessorMap.keySet());
        return memberNames;
    }

    // ************************************************************************
    // Model methods
    // ************************************************************************

    public Set<Class<?>> getEntityClassSet() {
        return entityDescriptorMap.keySet();
    }

    public Collection<EntityDescriptor<Solution_>> getEntityDescriptors() {
        return entityDescriptorMap.values();
    }

    public Collection<EntityDescriptor<Solution_>> getGenuineEntityDescriptors() {
        List<EntityDescriptor<Solution_>> genuineEntityDescriptorList = new ArrayList<>(entityDescriptorMap.size());
        for (EntityDescriptor<Solution_> entityDescriptor : entityDescriptorMap.values()) {
            if (entityDescriptor.hasAnyDeclaredGenuineVariableDescriptor()) {
                genuineEntityDescriptorList.add(entityDescriptor);
            }
        }
        return genuineEntityDescriptorList;
    }

    public boolean hasEntityDescriptorStrict(Class<?> entityClass) {
        return entityDescriptorMap.containsKey(entityClass);
    }

    public EntityDescriptor<Solution_> getEntityDescriptorStrict(Class<?> entityClass) {
        return entityDescriptorMap.get(entityClass);
    }

    public boolean hasEntityDescriptor(Class<?> entitySubclass) {
        EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptor(entitySubclass);
        return entityDescriptor != null;
    }

    public EntityDescriptor<Solution_> findEntityDescriptorOrFail(Class<?> entitySubclass) {
        EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptor(entitySubclass);
        if (entityDescriptor == null) {
            throw new IllegalArgumentException("A planning entity is an instance of an entitySubclass ("
                    + entitySubclass + ") that is not configured as a planning entity.\n" +
                    "If that class (" + entitySubclass.getSimpleName()
                    + ") (or superclass thereof) is not a entityClass (" + getEntityClassSet()
                    + "), check your Solution implementation's annotated methods.\n" +
                    "If it is, check your solver configuration.");
        }
        return entityDescriptor;
    }

    public EntityDescriptor<Solution_> findEntityDescriptor(Class<?> entitySubclass) {
        return lowestEntityDescriptorCache.computeIfAbsent(entitySubclass, key -> {
            // Reverse order to find the nearest ancestor
            for (Class<?> entityClass : reversedEntityClassList) {
                if (entityClass.isAssignableFrom(entitySubclass)) {
                    return entityDescriptorMap.get(entityClass);
                }
            }
            return null;
        });
    }

    public GenuineVariableDescriptor<Solution_> findGenuineVariableDescriptor(Object entity, String variableName) {
        EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
        return entityDescriptor.getGenuineVariableDescriptor(variableName);
    }

    public GenuineVariableDescriptor<Solution_> findGenuineVariableDescriptorOrFail(Object entity, String variableName) {
        EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
        GenuineVariableDescriptor<Solution_> variableDescriptor = entityDescriptor.getGenuineVariableDescriptor(variableName);
        if (variableDescriptor == null) {
            throw new IllegalArgumentException(entityDescriptor.buildInvalidVariableNameExceptionMessage(variableName));
        }
        return variableDescriptor;
    }

    public VariableDescriptor<Solution_> findVariableDescriptor(Object entity, String variableName) {
        EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
        return entityDescriptor.getVariableDescriptor(variableName);
    }

    public VariableDescriptor<Solution_> findVariableDescriptorOrFail(Object entity, String variableName) {
        EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
        VariableDescriptor<Solution_> variableDescriptor = entityDescriptor.getVariableDescriptor(variableName);
        if (variableDescriptor == null) {
            throw new IllegalArgumentException(entityDescriptor.buildInvalidVariableNameExceptionMessage(variableName));
        }
        return variableDescriptor;
    }

    // ************************************************************************
    // Locator methods
    // ************************************************************************

    public LocationStrategyResolver getLocationStrategyResolver() {
        return locationStrategyResolver;
    }

    // ************************************************************************
    // Extraction methods
    // ************************************************************************

    public Collection<Object> getAllFacts(Solution_ solution) {
        Collection<Object> facts = new ArrayList<>();
        // Adds both entities and facts
        Arrays.asList(entityMemberAccessorMap, problemFactMemberAccessorMap).forEach(map -> map.forEach((key, memberAccessor) -> {
            Object object = extractMemberObject(memberAccessor, solution);
            if (object != null) {
                facts.add(object);
            }
        }));
        Arrays.asList(entityCollectionMemberAccessorMap, problemFactCollectionMemberAccessorMap).forEach(map ->
                map.forEach((key, memberAccessor) -> facts.addAll(extractMemberCollection(memberAccessor, solution))));
        return facts;
    }

    /**
     * @param solution never null
     * @return {@code >= 0}
     */
    public int getEntityCount(Solution_ solution) {
        int entityCount = 0;
        for (MemberAccessor entityMemberAccessor : entityMemberAccessorMap.values()) {
            Object entity = extractMemberObject(entityMemberAccessor, solution);
            if (entity != null) {
                entityCount++;
            }
        }
        for (MemberAccessor entityCollectionMemberAccessor : entityCollectionMemberAccessorMap.values()) {
            Collection<Object> entityCollection = extractMemberCollection(entityCollectionMemberAccessor, solution);
            entityCount += entityCollection.size();
        }
        return entityCount;
    }

    public List<Object> getEntityList(Solution_ solution) {
        List<Object> entityList = new ArrayList<>();
        for (MemberAccessor entityMemberAccessor : entityMemberAccessorMap.values()) {
            Object entity = extractMemberObject(entityMemberAccessor, solution);
            if (entity != null) {
                entityList.add(entity);
            }
        }
        for (MemberAccessor entityCollectionMemberAccessor : entityCollectionMemberAccessorMap.values()) {
            Collection<Object> entityCollection = extractMemberCollection(entityCollectionMemberAccessor, solution);
            entityList.addAll(entityCollection);
        }
        return entityList;
    }

    public List<Object> getEntityListByEntityClass(Solution_ solution, Class<?> entityClass) {
        List<Object> entityList = new ArrayList<>();
        for (MemberAccessor entityMemberAccessor : entityMemberAccessorMap.values()) {
            if (entityMemberAccessor.getType().isAssignableFrom(entityClass)) {
                Object entity = extractMemberObject(entityMemberAccessor, solution);
                if (entity != null && entityClass.isInstance(entity)) {
                    entityList.add(entity);
                }
            }
        }
        for (MemberAccessor entityCollectionMemberAccessor : entityCollectionMemberAccessorMap.values()) {
            // TODO if (entityCollectionPropertyAccessor.getPropertyType().getElementType().isAssignableFrom(entityClass)) {
            Collection<Object> entityCollection = extractMemberCollection(entityCollectionMemberAccessor, solution);
            for (Object entity : entityCollection) {
                if (entityClass.isInstance(entity)) {
                    entityList.add(entity);
                }
            }
        }
        return entityList;
    }

    /**
     * @param solution never null
     * @return {@code >= 0}
     */
    public long getGenuineVariableCount(Solution_ solution) {
        long variableCount = 0L;
        for (Iterator<Object> it = extractAllEntitiesIterator(solution); it.hasNext(); ) {
            Object entity = it.next();
            EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
            variableCount += entityDescriptor.getGenuineVariableCount();
        }
        return variableCount;
    }

    public long getMaximumValueCount(Solution_ solution) {
        long maximumValueCount = 0L;
        for (Iterator<Object> it = extractAllEntitiesIterator(solution); it.hasNext(); ) {
            Object entity = it.next();
            EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
            maximumValueCount = Math.max(maximumValueCount, entityDescriptor.getMaximumValueCount(solution, entity));
        }
        return maximumValueCount;
    }

    /**
     * @param solution never null
     * @return {@code >= 0}
     */
    public int getValueCount(Solution_ solution) {
        int valueCount = 0;
        // TODO FIXME for ValueRatioTabuSizeStrategy (or reuse maximumValueCount() for that variable descriptor?)
        throw new UnsupportedOperationException(
                "getValueCount is not yet supported - this blocks ValueRatioTabuSizeStrategy");
        // return valueCount;
    }

    /**
     * Calculates an indication on how big this problem instance is.
     * This is intentionally very loosely defined for now.
     * @param solution never null
     * @return {@code >= 0}
     */
    public long getProblemScale(Solution_ solution) {
        long problemScale = 0L;
        for (Iterator<Object> it = extractAllEntitiesIterator(solution); it.hasNext(); ) {
            Object entity = it.next();
            EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
            problemScale += entityDescriptor.getProblemScale(solution, entity);
        }
        return problemScale;
    }

    public int countUninitializedVariables(Solution_ solution) {
        int count = 0;
        for (Iterator<Object> it = extractAllEntitiesIterator(solution); it.hasNext(); ) {
            Object entity = it.next();
            EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
            count += entityDescriptor.countUninitializedVariables(entity);
        }
        return count;
    }

    public int countReinitializableVariables(ScoreDirector scoreDirector, Solution_ solution) {
        int count = 0;
        for (Iterator<Object> it = extractAllEntitiesIterator(solution); it.hasNext(); ) {
            Object entity = it.next();
            EntityDescriptor<Solution_> entityDescriptor = findEntityDescriptorOrFail(entity.getClass());
            count += entityDescriptor.countReinitializableVariables(scoreDirector, entity);
        }
        return count;
    }

    public Iterator<Object> extractAllEntitiesIterator(Solution_ solution) {
        List<Iterator<Object>> iteratorList = new ArrayList<>(
                entityMemberAccessorMap.size() + entityCollectionMemberAccessorMap.size());
        for (MemberAccessor entityMemberAccessor : entityMemberAccessorMap.values()) {
            Object entity = extractMemberObject(entityMemberAccessor, solution);
            if (entity != null) {
                iteratorList.add(Collections.singletonList(entity).iterator());
            }
        }
        for (MemberAccessor entityCollectionMemberAccessor : entityCollectionMemberAccessorMap.values()) {
            Collection<Object> entityCollection = extractMemberCollection(entityCollectionMemberAccessor, solution);
            iteratorList.add(entityCollection.iterator());
        }
        return Iterators.concat(iteratorList.iterator());
    }

    private Object extractMemberObject(MemberAccessor memberAccessor, Solution_ solution) {
        return memberAccessor.executeGetter(solution);
    }

    private Collection<Object> extractMemberCollection(MemberAccessor collectionMemberAccessor, Solution_ solution,
            boolean isFact) {
        Collection<Object> collection = (Collection<Object>) collectionMemberAccessor.executeGetter(solution);
        if (collection == null) {
            throw new IllegalArgumentException("The solutionClass (" + solutionClass
                    + ")'s " + (isFact ? "factCollectionProperty" : "entityCollectionProperty") + " ("
                    + collectionMemberAccessor + ") should never return null.");
        }
        return collection;
    }

    private Collection<Object> extractMemberCollection(MemberAccessor collectionMemberAccessor, Solution_ solution) {
        return extractMemberCollection(collectionMemberAccessor, solution, false);
    }

    /**
     * @param solution never null
     * @return sometimes null, if the {@link Score} hasn't been calculated yet
     */
    public Score getScore(Solution_ solution) {
        return (Score) scoreMemberAccessor.executeGetter(solution);
    }

    /**
     * Called when the {@link Score} has been calculated or predicted.
     * @param solution never null
     * @param score sometimes null, in rare occasions to indicate that the old {@link Score} is stale,
     * but no new ones has been calculated
     */
    public void setScore(Solution_ solution, Score score) {
        scoreMemberAccessor.executeSetter(solution, score);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + solutionClass.getName() + ")";
    }

}
