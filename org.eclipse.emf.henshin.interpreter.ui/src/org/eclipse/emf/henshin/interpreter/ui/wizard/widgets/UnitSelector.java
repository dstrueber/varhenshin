/**
 * <copyright>
 * Copyright (c) 2010-2012 Henshin developers. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 */
package org.eclipse.emf.henshin.interpreter.ui.wizard.widgets;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.emf.henshin.interpreter.ui.HenshinInterpreterUIPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Button;

/**
 * @author Gregor Bonifer, Stefan Jurack, Christian Krause
 */
public class UnitSelector {
	
	protected int CONTROL_OFFSET = 5;
	
	protected Collection<UnitSelectionListener> listeners = new ArrayList<UnitSelector.UnitSelectionListener>();
	
	protected Group container;
	
	protected Combo unitSelector;
	
	protected Button unitFilter;
	
	//Save the outerunits in a separate list so you don't have to check every time you filter
	protected String[] selectableUnits;
	protected String[] outerUnits;
	
	//TODO This variable is always NULL. What is it supposed to do?
	//protected List<Unit> units;
	
	public UnitSelector(Composite parent) {
		container = new Group(parent, SWT.NONE);
		container.setText(HenshinInterpreterUIPlugin.LL("_UI_SelectTransformationUnit"));
		container.setLayout(new FormLayout());
		unitSelector = new Combo(container, SWT.NONE);
		{
			FormData data = new FormData();
			data.top = new FormAttachment(0, CONTROL_OFFSET);
			data.left = new FormAttachment(0, CONTROL_OFFSET);
			data.right = new FormAttachment(100, -CONTROL_OFFSET);
			unitSelector.setLayoutData(data);
		}
		
		unitSelector.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (UnitSelectionListener l : listeners)
					l.unitSelected(unitSelector.getSelectionIndex());
			}
		});
		
		unitFilter = new Button(container, SWT.CHECK);
		unitFilter.setAlignment(SWT.CENTER);
		unitFilter.setText(HenshinInterpreterUIPlugin.LL("_UI_ShowInnerUnits"));
		{
			FormData data = new FormData();
			data.top = new FormAttachment(unitSelector, CONTROL_OFFSET);
			data.right = new FormAttachment(100, -CONTROL_OFFSET);
			unitFilter.setLayoutData(data);
		}
		
		unitFilter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelection(unitFilter.getSelection());
			}
		});
		
		unitFilter.setSelection(false);
	}
	
	public Control getControl() {
		return container;
	}
	
	public void setSelection(int idx) {
		unitSelector.select(idx);
	}
	
	public void setSelectableUnits(String[] selectableUnits, String[] outerUnits) {
		this.selectableUnits = selectableUnits;
		this.outerUnits = outerUnits;
		updateSelection(false);
	}
	
	private void updateSelection(Boolean showInnerUnits) {
		unitSelector.removeAll();
		String[] selectionArray = showInnerUnits ? selectableUnits : outerUnits;
		for(String unit : selectionArray)
			unitSelector.add(unit);
		if(unitSelector.getItemCount() > 0)
			unitSelector.setText(unitSelector.getItem(0));
	}
	
	public void addUnitSelectionListener(UnitSelectionListener listener) {
		listeners.add(listener);
	}
	
	public static interface UnitSelectionListener {
		boolean unitSelected(int idx);
	}
	
	public void setEnabled(boolean enabled) {
		this.unitSelector.setEnabled(enabled);
	}
	
}
