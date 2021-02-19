package tsp.geneticAIModern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import tsp.Algorithm;
import tsp.utils.Util;
import tsp.utils.Metric;

/**
 * Artificial Intelligence A Modern Approach (3rd Edition): Figure 4.8, page
 * 129.<br>
 * <br>
 * 
 * <pre>
 * function GENETIC-ALGORITHM(population, FITNESS-FN) returns an individual
 *   inputs: population, a set of individuals
 *           FITNESS-FN, a function that measures the fitness of an individual
 *           
 *   repeat
 *     new_population &lt;- empty set
 *     for i = 1 to SIZE(population) do
 *       x &lt;- RANDOM-SELECTION(population, FITNESS-FN)
 *       y &lt;- RANDOM-SELECTION(population, FITNESS-FN)
 *       child &lt;- REPRODUCE(x, y)
 *       if (small random probability) then child &lt;- MUTATE(child)
 *       add child to new_population
 *     population &lt;- new_population
 *   until some individual is fit enough, or enough time has elapsed
 *   return the best individual in population, according to FITNESS-FN
 * --------------------------------------------------------------------------------
 * function REPRODUCE(x, y) returns an individual
 *   inputs: x, y, parent individuals
 *   
 *   n &lt;- LENGTH(x); c &lt;- random number from 1 to n
 *   return APPEND(SUBSTRING(x, 1, c), SUBSTRING(y, c+1, n))
 * </pre>
 * 
 * Figure 4.8 A genetic algorithm. The algorithm is the same as the one
 * diagrammed in Figure 4.6, with one variation: in this more popular version,
 * each mating of two parents produces only one offspring, not two.
 * 
 * @author Ciaran O'Reilly
 * @author Mike Stampone
 * @author Ruediger Lunde
 * 
 * @param <A> the type of the alphabet used in the representation of the
 *        individuals in the population (this is to provide flexibility in terms
 *        of how a problem can be encoded).
 */
public class GeneticAlgorithm<A> extends Algorithm<A> {

	protected int individualLength;
	protected List<A> finiteAlphabet;
	protected double mutationProbability;
	public Metric bestFitness = new Metric(0.0);
	protected Random random;

	public GeneticAlgorithm(int individualLength, Collection<A> finiteAlphabet, double mutationProbability) {
		this(individualLength, finiteAlphabet, mutationProbability, new Random());
	}

	public GeneticAlgorithm(int individualLength, Collection<A> finiteAlphabet, double mutationProbability,
			Random random) {
		this.individualLength = individualLength;
		this.finiteAlphabet = new ArrayList<A>(finiteAlphabet);
		this.mutationProbability = mutationProbability;
		this.random = random;

		assert (this.mutationProbability >= 0.0 && this.mutationProbability <= 1.0);
	}

	/**
	 * Starts the genetic algorithm and stops after a specified number of
	 * iterations.
	 */
	public Individual<A> geneticAlgorithm(Collection<Individual<A>> initPopulation, FitnessFunction<A> fitnessFn,
			final int maxIterations, long maxTimeMilliseconds) {
		Predicate<Individual<A>> goalTest = state -> getIterations() >= maxIterations;
		return geneticAlgorithm(initPopulation, fitnessFn, goalTest, maxTimeMilliseconds);
	}

	/**
	 * Template method controlling search. It returns the best individual in the
	 * specified population, according to the specified FITNESS-FN and goal test.
	 * 
	 * @param initPopulation      a set of individuals
	 * @param fitnessFn           a function that measures the fitness of an
	 *                            individual
	 * @param goalTest            test determines whether a given individual is fit
	 *                            enough to return. Can be used in subclasses to
	 *                            implement additional termination criteria, e.g.
	 *                            maximum number of iterations.
	 * @param maxTimeMilliseconds the maximum time in milliseconds that the
	 *                            algorithm is to run for (approximate). Only used
	 *                            if > 0L.
	 * @return the best individual in the specified population, according to the
	 *         specified FITNESS-FN and goal test.
	 */
	public Individual<A> geneticAlgorithm(Collection<Individual<A>> initPopulation, FitnessFunction<A> fitnessFn,
			Predicate<Individual<A>> goalTest, long maxTimeMilliseconds) {
		
		this.addProgressTracker(new ProgressTracker("bestFitness", this.bestFitness));
		
		Individual<A> bestIndividual = null;
		// Create a local copy of the population to work with
		List<Individual<A>> population = new ArrayList<>(initPopulation);
		validatePopulation(population);
		updateMetrics(population, 0, 0L);

		long startTime = System.currentTimeMillis();
		bestIndividual = retrieveBestIndividual(initPopulation, fitnessFn);
		int itCount = 0;
		do {
			population = nextGeneration(population, fitnessFn, bestIndividual);
			bestIndividual = retrieveBestIndividual(population, fitnessFn);

			this.bestFitness.setValue(fitnessFn.apply(bestIndividual));
			this.notifyProgressTrackers();
			
			// Monitor average and best fitness
			//System.out.println("\nGen: " + itCount + " Best f: " + fitnessFn.apply(bestIndividual) + " Average f:"
			//		+ averageFitness(population, fitnessFn));

			updateMetrics(population, ++itCount, System.currentTimeMillis() - startTime);

			// Until some individual is fit enough, or enough time has elapsed
			if (maxTimeMilliseconds > 0L && (System.currentTimeMillis() - startTime) > maxTimeMilliseconds)
				break;

		} while (!goalTest.test(bestIndividual));
		System.out.println(metrics.getMetricValues("bestFitness"));
		return bestIndividual;
	}

	public Individual<A> retrieveBestIndividual(Collection<Individual<A>> population, FitnessFunction<A> fitnessFn) {
		Individual<A> bestIndividual = null;
		double bestSoFarFValue = 0;

		for (Individual<A> individual : population) {
			double fValue = fitnessFn.apply(individual);
			if (fValue > bestSoFarFValue) {
				bestIndividual = individual;
				bestSoFarFValue = fValue;
			}
		}

		return bestIndividual;
	}

	//
	// PROTECTED METHODS
	//
	// Note: Override these protected methods to create your own desired
	// behavior.
	//
	/**
	 * Primitive operation which is responsible for creating the next generation.
	 * Override to get progress information!
	 */
	protected List<Individual<A>> nextGeneration(List<Individual<A>> population, FitnessFunction<A> fitnessFn,
			Individual<A> bestBefore) {
		List<Individual<A>> newPopulation = new ArrayList<>(population.size());
		for (int i = 0; i < population.size() - 1; i++) { // -1 para elitismo
			Individual<A> x = randomSelection(population, fitnessFn);
			Individual<A> y = randomSelection(population, fitnessFn);
			Individual<A> child = reproduce(x, y);

			if (random.nextDouble() <= mutationProbability) {
				child = mutate(child);
			}
			newPopulation.add(child);
		}
		newPopulation.add(bestBefore);
		return newPopulation;
	}

	protected Individual<A> randomSelection(List<Individual<A>> population, FitnessFunction<A> fitnessFn) {
		// Default result is last individual to avoid problems with rounding errors
		Individual<A> selected = population.get(population.size() - 1);

		// Determine all of the fitness values
		double[] fValues = new double[population.size()];
		double minFitness = fitnessFn.apply(population.get(0));
		for (int i = 0; i < population.size(); i++) {
			fValues[i] = fitnessFn.apply(population.get(i));
			if (minFitness > fValues[i])
				minFitness = fValues[i];
		}

		// Fitness scalation: Every individual is sustracted lowest fitness
		for (int i = 0; i < population.size(); i++) {
			fValues[i] -= minFitness;
		}
		fValues = Util.normalize(fValues);

		double prob = random.nextDouble();
		double totalSoFar = 0.0;
		for (int i = 0; i < fValues.length; i++) {
			totalSoFar += fValues[i];
			if (prob <= totalSoFar) {
				selected = population.get(i);
				break;
			}
		}

		selected.incDescendants();
		return selected;
	}

	protected Individual<A> reproduce(Individual<A> x, Individual<A> y) {

		int workingIndividualLength = individualLength-1;
		
		List<A> childRepresentation = new ArrayList<A>(x.getRepresentation());
		int p1 = randomOffset(workingIndividualLength);
		int p2 = randomOffset(workingIndividualLength);
		List<A> inheritedFromFirstParent = new ArrayList<A>();
		
		//Inheriting from first parent
		int i = p1;
		while (i != p2) {
			inheritedFromFirstParent.add(x.getRepresentation().get(i));
			i++;
			if (i == workingIndividualLength)
				i = 0;
		}

		// Inheriting from second parent
		int secondParentInheritsAt = p2;
		for (i = 0; i < workingIndividualLength; i++) {
			if (!inheritedFromFirstParent.contains(y.getRepresentation().get(i))) {
				childRepresentation.set(secondParentInheritsAt, y.getRepresentation().get(i));
				secondParentInheritsAt++;
				if (secondParentInheritsAt == workingIndividualLength)
					secondParentInheritsAt = 0;
			}
		}
		
		// Last city must be initial one
		childRepresentation.set(individualLength-1, childRepresentation.get(0));
		return new Individual<A>(childRepresentation);
	}

	protected double averageFitness(List<Individual<A>> population, FitnessFunction<A> fitnessFn) {
		double totalFitness = 0.0;
		for (int i = 0; i < population.size(); i++) {
			totalFitness += fitnessFn.apply(population.get(i));
		}
		return totalFitness / population.size();
	}

	protected Individual<A> mutate(Individual<A> child) {
		// Select two random cities that are not the initial one and exchange them
		List<A> mutatedRepresentation = new ArrayList<A>(child.getRepresentation());
		int mutateOffsetPos1 = randomOffset(individualLength-1);
		int mutateOffsetPos2 = randomOffset(individualLength-1);
		A mutateOffsetValue1 = mutatedRepresentation.get(mutateOffsetPos1);
		A mutateOffsetValue2 = mutatedRepresentation.get(mutateOffsetPos2);

		mutatedRepresentation.set(mutateOffsetPos1, mutateOffsetValue2);
		mutatedRepresentation.set(mutateOffsetPos2, mutateOffsetValue1);
		
		// Last city must be initial one
		mutatedRepresentation.set(individualLength-1, mutatedRepresentation.get(0));
		
		return new Individual<A>(mutatedRepresentation);
	}

	protected int randomOffset(int length) {
		return random.nextInt(length);
	}

	protected void validatePopulation(Collection<Individual<A>> population) {
		if (population.size() < 1) {
			throw new IllegalArgumentException("Must start with at least a population of size 1");
		}
		// String lengths are assumed to be of fixed size,
		// therefore ensure initial populations lengths correspond to this
		for (Individual<A> individual : population) {
			if (individual.length() != this.individualLength) {
				throw new IllegalArgumentException("Individual [" + individual
						+ "] in population is not the required length of " + this.individualLength);
			}
		}
	}
	
}