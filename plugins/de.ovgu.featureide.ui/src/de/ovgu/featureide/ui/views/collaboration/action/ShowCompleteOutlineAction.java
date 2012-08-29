/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2012  FeatureIDE team, University of Magdeburg
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
package de.ovgu.featureide.ui.views.collaboration.action;

import org.eclipse.jface.action.Action;
import de.ovgu.featureide.ui.views.collaboration.CollaborationView;

/**
 * Shows unselected features at collaboration model
 * 
 * @author Jens Meinicke
 */
public class ShowCompleteOutlineAction extends Action {

	private CollaborationView collaborationView;
	public ShowCompleteOutlineAction(String text, CollaborationView collaborationView) {
		super(text);
		this.collaborationView = collaborationView;
	}

	public void setEnabled(boolean enabled) {
		super.setChecked(collaborationView.builder.showCompleteOutline);
		super.setEnabled(true);
	}
	
	public void run() {
		collaborationView.builder.showCompleteOutline(!collaborationView.builder.showCompleteOutline);
		collaborationView.refresh();
	}
}
