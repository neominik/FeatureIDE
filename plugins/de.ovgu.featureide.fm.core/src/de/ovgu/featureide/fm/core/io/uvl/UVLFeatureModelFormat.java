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
package de.ovgu.featureide.fm.core.io.uvl;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.prop4j.Equals;
import org.prop4j.Implies;
import org.prop4j.Literal;
import org.prop4j.Node;

import de.neominik.uvl.UVLParser;
import de.neominik.uvl.ast.And;
import de.neominik.uvl.ast.Equiv;
import de.neominik.uvl.ast.Feature;
import de.neominik.uvl.ast.Group;
import de.neominik.uvl.ast.Impl;
import de.neominik.uvl.ast.Import;
import de.neominik.uvl.ast.Not;
import de.neominik.uvl.ast.Or;
import de.neominik.uvl.ast.ParseError;
import de.neominik.uvl.ast.UVLModel;
import de.ovgu.featureide.fm.core.PluginID;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.MultiFeatureModelFactory;
import de.ovgu.featureide.fm.core.io.AFeatureModelFormat;
import de.ovgu.featureide.fm.core.io.APersistentFormat;
import de.ovgu.featureide.fm.core.io.LazyReader;
import de.ovgu.featureide.fm.core.io.Problem;
import de.ovgu.featureide.fm.core.io.Problem.Severity;
import de.ovgu.featureide.fm.core.io.ProblemList;

/**
 * TODO description
 *
 * @author Dominik Engelhardt
 */
public class UVLFeatureModelFormat extends AFeatureModelFormat {

	public static final String ID = PluginID.PLUGIN_ID + ".format.fm." + UVLFeatureModelFormat.class.getSimpleName();
	public static final String FILE_EXTENSION = "uvl";

	private static final String NS_ATTRIBUTE_NAME = "namespace";
	private static final String NS_ATTRIBUTE_FEATURE = "_synthetic_ns_feature";

	private UVLModel rootModel;

	@Override
	public String getName() {
		return "UVL";
	}

	@Override
	public String getSuffix() {
		return FILE_EXTENSION;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public APersistentFormat<IFeatureModel> getInstance() {
		return new UVLFeatureModelFormat();
	}

	@Override
	public ProblemList read(IFeatureModel fm, CharSequence source) {
		if (fm.getSourceFile() != null) {
			return read(fm, source, fm.getSourceFile().toAbsolutePath());
		}
		System.err.println("No path set for model. Can't load imported models.");
		return read(fm, source, new File("./.").toPath());
	}

	@Override
	public ProblemList read(IFeatureModel fm, CharSequence source, Path path) {
		fm.setSourceFile(path);
		final ProblemList pl = new ProblemList();
		final Object result = UVLParser.parse(source.toString(), path.getParent().toString());
		if (result instanceof UVLModel) {
			rootModel = (UVLModel) result;
			constructFeatureModel((MultiFeatureModel) fm);
		} else if (result instanceof ParseError) {
			pl.add(toProblem((ParseError) result));
		}
		return pl;
	}

	private void constructFeatureModel(MultiFeatureModel fm) {
		fm.reset();
		IFeature root;
		if (rootModel.getRootFeatures().length == 1) {
			final Feature f = rootModel.getRootFeatures()[0];
			root = parseFeature(fm, null, f);
		} else {
			root = MultiFeatureModelFactory.getInstance().createFeature(fm, "Root");
			Arrays.asList(rootModel.getRootFeatures()).forEach(f -> parseFeature(fm, root, f));
		}
		fm.getStructure().setRoot(root.getStructure());
		Arrays.asList(rootModel.getConstraints()).forEach(c -> parseConstraint(fm, c));
		Arrays.asList(rootModel.getImports()).forEach(i -> parseImport(fm, i));
		fm.addAttribute(NS_ATTRIBUTE_FEATURE, NS_ATTRIBUTE_NAME, rootModel.getNamespace());
	}

	private IFeature parseFeature(MultiFeatureModel fm, IFeature root, Feature f) {
		final Feature resolved = UVLParser.resolve(f, rootModel);
		final IFeature feature = MultiFeatureModelFactory.getInstance().createFeature(fm, resolved.getName());
		if (root != null) {
			root.getStructure().addChild(feature.getStructure());
		}
		feature.getStructure().setAbstract(isAbstract(resolved));
		Arrays.asList(resolved.getGroups()).forEach(g -> parseGroup(fm, feature, g));
		parseAttributes(resolved, fm);
		return feature;
	}

	private void parseGroup(MultiFeatureModel fm, IFeature root, Group g) {
		final List<IFeature> children = Stream.of(g.getChildren()).map(f -> parseFeature(fm, root, (Feature) f)).collect(Collectors.toList());
		switch (g.getType()) {
		case "or":
			root.getStructure().setOr();
			break;
		case "alternative":
			root.getStructure().setAlternative();
			break;
		case "optional":
			break;
		case "mandatory":
			children.forEach(f -> f.getStructure().setMandatory(true));
			break;
		}
	}

	private boolean isAbstract(Feature f) {
		return Objects.equals(true, f.getAttributes().get("abstract"));
	}

	@SuppressWarnings("unchecked")
	private void parseAttributes(Feature f, MultiFeatureModel fm) {
		((Map<String, Object>) f.getAttributes()).entrySet().stream().filter(e -> e.getKey().equals("constraint") || e.getKey().equals("constraints"))
				.forEach(e -> parseAttribute(fm, e.getValue()));
	}

	private void parseAttribute(MultiFeatureModel fm, Object value) {
		if (value instanceof List<?>) {
			((List<?>) value).forEach(v -> parseConstraint(fm, v));
		} else {
			parseConstraint(fm, value);
		}
	}

	private void parseConstraint(IFeatureModel fm, Object c) {
		final Node constraint = parseConstraint(c);
		if (constraint != null) {
			fm.addConstraint(MultiFeatureModelFactory.getInstance().createConstraint(fm, constraint));
		}
	}

	private Node parseConstraint(Object c) {
		if (c instanceof String) {
			return new Literal((String) c);
		} else if (c instanceof Not) {
			return new org.prop4j.Not(parseConstraint(((Not) c).getChild()));
		} else if (c instanceof And) {
			return new org.prop4j.And(parseConstraint(((And) c).getLeft()), parseConstraint(((And) c).getRight()));
		} else if (c instanceof Or) {
			return new org.prop4j.Or(parseConstraint(((Or) c).getLeft()), parseConstraint(((Or) c).getRight()));
		} else if (c instanceof Impl) {
			return new Implies(parseConstraint(((Impl) c).getLeft()), parseConstraint(((Impl) c).getRight()));
		} else if (c instanceof Equiv) {
			return new Equals(parseConstraint(((Equiv) c).getLeft()), parseConstraint(((Equiv) c).getRight()));
		}
		return null;
	}

	private void parseImport(MultiFeatureModel fm, Import i) {
		fm.addInstance(i.getAlias(), i.getNamespace());
	}

	/**
	 * @param error a {@link ParseError}
	 * @return a {@link Problem}
	 */
	private Problem toProblem(ParseError error) {
		return new Problem(error.toString(), error.getLine(), Severity.ERROR);
	}

	@Override
	public String write(IFeatureModel object) {
		return deconstructFeatureModel((MultiFeatureModel) object).toString();
	}

	private UVLModel deconstructFeatureModel(MultiFeatureModel fm) {
		final UVLModel model = new UVLModel();
		model.setNamespace(fm.getStringAttributes().getAttribute(NS_ATTRIBUTE_FEATURE, NS_ATTRIBUTE_NAME).getValue());
		model.setImports(fm.getExternalModels().values().stream().map(um -> new Import(um.getVarName(), um.getModelName())).toArray(Import[]::new));
		model.setRootFeatures(new Feature[] { printFeature(fm.getStructure().getRoot().getFeature()) });
		model.setConstraints(fm.getConstraints().stream().map(this::printConstraint).toArray());
		return model;
	}

	private Feature printFeature(IFeature feature) {
		final Feature f = new Feature();
		f.setName(feature.getName());
		if (feature.getStructure().isAbstract()) {
			f.setAttributes(Collections.singletonMap("abstract", true));
		}
		f.setGroups(printGroups(feature));
		return f;
	}

	private Group constructGroup(IFeatureStructure fs, String type, Predicate<IFeatureStructure> pred) {
		return new Group(type, fs.getChildren().stream().filter(f -> !f.getFeature().getName().contains(".")).filter(pred)
				.map(f -> printFeature(f.getFeature())).toArray(Feature[]::new));
	}

	private Group[] printGroups(IFeature feature) {
		final IFeatureStructure fs = feature.getStructure();
		if (!fs.hasChildren()) {
			return new Group[] {};
		}
		if (fs.isAnd()) {
			final Group m = constructGroup(fs, "mandatory", c -> c.isMandatory());
			final Group o = constructGroup(fs, "optional", c -> !c.isMandatory());
			return Stream.of(m, o).filter(g -> g.getChildren().length > 0).toArray(Group[]::new);
		} else if (fs.isOr()) {
			return new Group[] { constructGroup(fs, "or", x -> true) };
		} else if (fs.isAlternative()) {
			return new Group[] { constructGroup(fs, "alternative", x -> true) };
		}
		return new Group[] {};
	}

	private Object printConstraint(IConstraint constraint) {
		return printConstraint(constraint.getNode());
	}

	private Object printConstraint(Node n) {
		if (n instanceof Literal) {
			return ((Literal) n).var;
		} else if (n instanceof org.prop4j.Not) {
			return new Not(printConstraint(n.getChildren()[0]));
		} else if (n instanceof org.prop4j.And) {
			return new And(printConstraint(n.getChildren()[0]), printConstraint(n.getChildren()[1]));
		} else if (n instanceof org.prop4j.Or) {
			return new Or(printConstraint(n.getChildren()[0]), printConstraint(n.getChildren()[1]));
		} else if (n instanceof Implies) {
			return new Impl(printConstraint(n.getChildren()[0]), printConstraint(n.getChildren()[1]));
		} else if (n instanceof Equals) {
			return new Equiv(printConstraint(n.getChildren()[0]), printConstraint(n.getChildren()[1]));
		}
		return null;
	}

	@Override
	public boolean supportsRead() {
		return true;
	}

	@Override
	public boolean supportsWrite() {
		return true;
	}

	@Override
	public boolean supportsContent(CharSequence content) {
		// TODO Auto-generated method stub
		return super.supportsContent(content);
	}

	@Override
	public boolean supportsContent(LazyReader reader) {
		// TODO Auto-generated method stub
		return super.supportsContent(reader);
	}

	@Override
	public boolean initExtension() {
		FMFactoryManager.getInstance().getDefaultFactoryWorkspace().assignID(getId(), MultiFeatureModelFactory.ID);
		return super.initExtension();
	}

}
