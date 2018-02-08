/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package org.prop4j.analyses.impl.general;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.prop4j.Literal;
import org.prop4j.analyses.GeneralSolverAnalysis;
import org.prop4j.solver.ISolver;
import org.prop4j.solver.impl.SolverUtils;
import org.prop4j.solver.impl.sat4j.Sat4jSatSolver;
import org.prop4j.solverOld.ISatSolver;

import de.ovgu.featureide.fm.core.FMCorePlugin;
import de.ovgu.featureide.fm.core.base.util.RingList;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;

/**
 * Finds core and dead features.
 *
 * @author Sebastian Krieter
 * @author Joshua Sprey
 */
public class ImplicationAnalysis extends GeneralSolverAnalysis<List<int[]>> {

	private List<int[]> pairs;

	public ImplicationAnalysis(ISolver solver, List<int[]> pairs) {
		super(solver);
		this.pairs = pairs;
	}

	public ImplicationAnalysis(ISolver solver) {
		super(solver);
	}

	public void initParis(List<int[]> pairs) {
		this.pairs = pairs;
	}

	@Override
	public List<int[]> analyze(IMonitor monitor) {
		String compare = "New Start:\nCNF: " + solver.getProblem().getRoot() + "\nAssignedPairs: " + SolverUtils.getListArrayString(pairs);

		final List<int[]> resultList = new ArrayList<>();

		if (pairs == null) {
			return resultList;
		}

		final HashMap<String, Object> config = new HashMap<>();

		final RingList<int[]> solutionList = new RingList<>(Math.min(pairs.size(), ISatSolver.MAX_SOLUTION_BUFFER));

		config.put(Sat4jSatSolver.CONFIG_SELECTION_STRATEGY, Sat4jSatSolver.SelectionStrategy.POSITIVE);
		solver.setConfiguration(config);

		monitor.checkCancel();
		final int[] model1 = SolverUtils.getIntModel(solver.findSolution());

		if (model1 != null) {
			solutionList.add(model1);

			config.clear();
			config.put(Sat4jSatSolver.CONFIG_SELECTION_STRATEGY, Sat4jSatSolver.SelectionStrategy.NEGATIVE);
			solver.setConfiguration(config);

			monitor.checkCancel();
			final int[] model2 = SolverUtils.getIntModel(solver.findSolution());
			compare += "\nModel1: " + Arrays.toString(model1) + "\nModel2: " + Arrays.toString(model2);
			solutionList.add(model2);

			// if there are more negative than positive literals
			if ((model1.length - countNegative(model1)) < countNegative(model2)) {
				config.clear();
				config.put(Sat4jSatSolver.CONFIG_SELECTION_STRATEGY, Sat4jSatSolver.SelectionStrategy.POSITIVE);
				solver.setConfiguration(config);
			}

			pairLoop: for (final int[] pair : pairs) {
				monitor.checkCancel();
				solutionLoop: for (final int[] is : solutionList) {
					for (final int i : pair) {
						if (is[Math.abs(i) - 1] == i) {
							continue solutionLoop;
						}
					}
					continue pairLoop;
				}
				for (final int i : pair) {
					solver.push(getLiteralFromIndex(-i));
				}
				switch (solver.isSatisfiable()) {
				case FALSE:
					resultList.add(pair);
					break;
				case TIMEOUT:
					break;
				case TRUE:
					solutionList.add(SolverUtils.getIntModel(solver.getSoulution()));
					if (solver instanceof Sat4jSatSolver) {
						((Sat4jSatSolver) solver).shuffleOrder();
					}
					break;
				}
				for (int i = 0; i < pair.length; i++) {
					solver.pop();
				}
			}
		}
		compare += "\n\nResult: " + SolverUtils.getListArrayString(resultList);
		FMCorePlugin.getDefault().logInfo(compare);
		return resultList;
	}

	private static int countNegative(int[] model) {
		int count = 0;
		for (int i = 0; i < model.length; i++) {
			count += model[i] >>> (Integer.SIZE - 1);
		}
		return count;
	}

	public Literal getLiteralFromIndex(int index) {
		final Object variable = solver.getProblem().getVariableOfIndex(Math.abs(index));
		final Literal literal = new Literal(variable, index > 0);
		return literal;
	}

}
