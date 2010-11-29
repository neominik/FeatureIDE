/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2010  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.core.io.guidsl;

import java.util.LinkedList;

import org.prop4j.Node;
import org.prop4j.NodeWriter;

import de.ovgu.featureide.fm.core.Feature;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.io.AbstractFeatureModelWriter;


/**
 * Writes a feature model in the GUIDSL format (grammar).
 * 
 * @author Thomas Thuem
 *
 */
public class GuidslWriter extends AbstractFeatureModelWriter {

	/**
	 * Creates a new writer and sets the feature model to write out.
	 * 
	 * @param featureModel the structure to write
	 */
	public GuidslWriter(FeatureModel featureModel) {
		setFeatureModel(featureModel);
	}
	
	private boolean hasHiddenFeatures(){
		for(Feature feat : featureModel.getFeatures())
			if (feat.isHidden()) return true;
		return false;
	}
	
	public String writeToString() {
		//open a string buffer for writing
		StringBuffer out = new StringBuffer();// = new BufferedWriter(new FileWriter(file));

		//write generating information
//		DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.ENGLISH);
//		out.append("//This model was generated by the Eclipse Plugin at ");
//		out.append(formatter.format(new Date()));
//		out.append("\r\n\r\n");
		
		//write comments
		writeComments(out);

		//write featureModel
		writeGrammarDefinition(out);
		writePropositionalConstraints(out);
		
		// write hidden features
		if(hasHiddenFeatures()){
			out.append("##\r\n\r\n");
			for (Feature feat : featureModel.getFeatures())
				if (feat.isHidden()) out.append(feat.toString() +  " { hidden } \r\n");
		}

		return out.toString();
	}
	
	private void writeComments(StringBuffer out) {
		for (int i = 0; i<featureModel.getComments().size(); i++)
			out.append("//" + featureModel.getComments().get(i) + "\n");
	}

	private void writeGrammarDefinition(StringBuffer out) {
		Feature root = featureModel.getRoot();
		if (root != null) {
			if (root.isOr()) {
				out.append(root.getName());
				out.append("_ : ");
				out.append(root.getName());
				out.append("+ :: _" );
				out.append(root.getName());
				out.append(" ;\r\n\r\n");
			}
			writeRule(featureModel.getRoot(), out);
		}
		else
			out.append("\r\n");
	}

	private void writePropositionalConstraints(StringBuffer out) {
		if (featureModel.getPropositionalNodes().isEmpty())
			return;
		out.append("%%\r\n\r\n");
		for (Node node : featureModel.getPropositionalNodes())
			out.append(node.toString(NodeWriter.textualSymbols) + " ;\r\n");
		out.append("\r\n");
	}

//	private void writeAnnotations(StringBuffer out) {
//		if (featureModel.getAnnotations() != null)
//			out.append(featureModel.getAnnotations() + "\r\n\r\n");
//	}

	private void writeRule(Feature mainFeature, StringBuffer out) {
		
//		if (mainFeature.isAbstract())
//			this.hasAbstract = true;

		//check if there is a rule to write
		if (!mainFeature.hasChildren())
			return;
		LinkedList<Feature> mainChildren = mainFeature.getChildren();
		
		//left part of the rule
		out.append(mainFeature.getName());
		out.append(" :");
		
		//check if a output over more than one line is possible
		boolean moreThanOneLine = isMoreThanOneLinePossible(mainFeature, mainChildren);
		//moreThanOneLine = !mainFeature.isAND();
		
		//write out the line(s)
		for (int i = 0; i < mainChildren.size(); i++) {
			Feature feature = mainChildren.get(i);
			if (moreThanOneLine && i > 0) {
				out.append("\r\n\t|");
			}
			else if (!mainFeature.isAnd() && i > 0) {
				out.append("\r\n\t|");
			}
			if (!mainFeature.isAnd() && feature.hasInlineRule()) {
				LinkedList<Feature> children = feature.getChildren();
				for (int j = 0; j < children.size(); j++) {
					out.append(" ");
					out.append(getRightGrammarToken(children.get(j)));
				}
				out.append(" :: ");
				out.append(feature.getName());
			}
			else {
				out.append(" ");
				out.append(getRightGrammarToken(feature));
				if (!mainFeature.isAnd() && (!feature.isMandatory() || feature.isMultiple())) {
					out.append(" :: ");
					out.append(feature.getName() + "_");
				}
			}
		}
		if (mainFeature.isAnd()) {// && mainChildren.size() > 1) {
			out.append(" :: _");
			out.append(mainFeature.getName());
		}
		out.append(" ;\r\n\r\n");
		
		//write all left rules
		writeChildRules(mainFeature, mainChildren, out);
	}

	private boolean isMoreThanOneLinePossible(Feature feature, LinkedList<Feature> children) {
		if (!feature.isAnd()) {
			for (int i = 0; i < children.size(); i++) {
				Feature child = children.get(i);
				if (child.hasInlineRule()) {
					return true;
				}
			}
		}
		return false;
	}

	public static String getRightGrammarToken(Feature feature) {
		if (feature.isMultiple()) {
			return feature.getName() + (feature.isMandatory() ? "+" : "*");
		}
		return feature.isMandatory() ? feature.getName() : "[" + feature.getName() + "]";
	}

	private void writeChildRules(Feature mainFeature, LinkedList<Feature> mainChildren, StringBuffer out) {
		for (int i = 0; i < mainChildren.size(); i++) {
			Feature feature = mainChildren.get(i);
			if (!mainFeature.isAnd() && feature.hasInlineRule()) {
				LinkedList<Feature> children = feature.getChildren();
				for (int j = 0; j < children.size(); j++) {
					writeRule(children.get(j), out);
				}
			}
			else {
				writeRule(feature, out);
			}
		}
	}

	/**
	 * @return true, if the feature model has at least one abstract feature
	 */
	public boolean hasAbstractFeatures() {
		return hasAbstractFeaturesRec(featureModel.getRoot());
	}
	
	private boolean hasAbstractFeaturesRec(Feature mainFeature){
		LinkedList<Feature> mainChildren = mainFeature.getChildren();
		for (int i = 0; i < mainChildren.size(); i++) {
			Feature feature = mainChildren.get(i);
			if (feature.isAbstract())
				return true;
			else if(feature.hasChildren()){
				return hasAbstractFeaturesRec(feature);
			}
		}
		return false;
	}
	
	public boolean hasConcreteCompounds() {
		return hasConcreteCompoundsRec(featureModel.getRoot());
	}
	
	private boolean hasConcreteCompoundsRec(Feature mainFeature){
		if (!mainFeature.isAbstract() && mainFeature.hasChildren())
			return true;
		LinkedList<Feature> mainChildren = mainFeature.getChildren();
		for (int i = 0; i < mainChildren.size(); i++) {
			Feature feature = mainChildren.get(i);
			if (!feature.isAbstract() && feature.hasChildren())
				return true;
			else if(feature.hasChildren()){
				return hasConcreteCompoundsRec(feature);
			}
		}
		return false;
	}
}
