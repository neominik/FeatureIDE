/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2019  FeatureIDE team, University of Magdeburg, Germany
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
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.ui.editors.featuremodel.actions;

import static de.ovgu.featureide.fm.core.localization.StringTable.CREATE_CONSTRAINT;
import static de.ovgu.featureide.fm.core.localization.StringTable.STARTING_WITH;

import org.eclipse.jface.viewers.IStructuredSelection;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.io.manager.IFeatureModelManager;
import de.ovgu.featureide.fm.ui.editors.ConstraintDialog;
import de.ovgu.featureide.fm.ui.editors.featuremodel.editparts.FeatureEditPart;

/**
 * The CREATE_CONSTRAINT action for a selected feature inside the feature diagram. Calling this action the constraint dialog will automatically contains the
 * selected feature inside the input control.
 *
 * @author Marcus Pinnecke
 */
public class CreateConstraintWithAction extends CreateConstraintAction {

	public static final String ID = "de.ovgu.featureide.createconstraintwith";

	protected String selectedFeature;

	public CreateConstraintWithAction(Object viewer, IFeatureModelManager featureModelManager) {
		super(viewer, featureModelManager, ID);
	}

	protected void updateConstraintActionText(String featureName) {
		selectedFeature = featureName;
		setText(CREATE_CONSTRAINT + (featureName.isEmpty() ? "" : " " + STARTING_WITH + " \"" + featureName + "\""));
	}

	@Override
	public void run() {
		final ConstraintDialog dialog = new ConstraintDialog(featureModelManager, null);
		if (selectedFeature != null) {
			dialog.setInputText(selectedFeature);
		}
	}

	@Override
	protected boolean isValidSelection(IStructuredSelection selection) {
		if ((selection != null) && (selection.size() == 1)) {
			final Object editPart = selection.getFirstElement();

			final IFeature feature = editPart instanceof FeatureEditPart ? ((FeatureEditPart) editPart).getModel().getObject() : null;

			if (feature != null) {
				updateConstraintActionText(feature.getName());
				return true;
			}
		}
		return false;
	}

}
