package com.kreative.bitsnpicas.edit.exporter;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class BitmapExportNFTRPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private final JComboBox<Integer> bitDepth;
	
	public BitmapExportNFTRPanel() {
		this.bitDepth = new JComboBox<Integer>(new Integer[] {
			1, 2, 3, 4, 5, 6, 7, 8
		});
		this.bitDepth.setSelectedIndex(2);
		JPanel bp = new JPanel(new GridLayout(1, 0, 4, 4));
		bp.add(new JLabel("Bit Depth"));
		bp.add(bitDepth);
		this.setLayout(new BorderLayout());
		this.add(bp, BorderLayout.PAGE_START);
	}
	
	public int getBitDepth() {
		return (int) bitDepth.getSelectedItem();
	}
}
