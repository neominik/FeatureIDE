package de.ovgu.featureide.core.typecheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.typecheck.checks.Checks;
import de.ovgu.featureide.core.typecheck.parser.ClassTable;
import de.ovgu.featureide.core.typecheck.parser.Parser;
import de.ovgu.featureide.fm.core.Feature;

public class TypeChecker
{

	private IFeatureProject _project;
	private Parser _parser;
	private ClassTable _class_table;

	public TypeChecker(IFeatureProject project)
	{
		_project = project;
		_parser = new Parser();
	}

	public void run()
	{
		System.out.println("Starting parsing project " + _project.getProjectName());
		String sourcePath = _project.getSourcePath();
		Collection<Feature> features = _project.getFeatureModel().getConcreteFeatures();
		List<Feature> concrete_features = new ArrayList<Feature>();

		for (Feature feature : features)
		{
			concrete_features.add(feature);
		}

		// TODO: consider the userdefined feature order
		// if (_project.getFeatureModel().isFeatureOrderUserDefined()) {
		// _parser.parse(sourcePath, _project.getFeatureModel()
		// .getFeatureOrderList());
		// } else {

		_parser.parse(sourcePath, (concrete_features));

		_class_table = _parser.getClassTable();

		// }

//		System.out.println(_class_table.toString());

		// for (Feature feature : features)
		// {
		// System.out.println("Classes Introduced or Refined by Feature " +
		// feature.getName());
		// for (ClassTableEntry entry :
		// _class_table.getClassesByFeature(feature.getName()))
		// {
		// System.out.println("\t" + entry.getClassName());
		// }
		// }
		//
		// for (String class_name : _class_table.getClassNames())
		// {
		// System.out.println("Features introducing or refining class " +
		// class_name);
		// for (ClassTableEntry entry :
		// _class_table.getFeaturesByClass(class_name))
		// {
		// System.out.println("\t" + entry.getFeatureName());
		// }
		// }
		//
		// for (ClassTableEntry entry : _class_table.getClasses())
		// {
		//
		// }

		System.out.println("Parsing finished... (" + _parser.timer.getTime() + " ms)");
		System.out.println("Checking superclasses...");
		Checks checks = new Checks(_project, _class_table);
		checks.superClassCheck();
		System.out.println("Superclass check finished...");
	}
}
