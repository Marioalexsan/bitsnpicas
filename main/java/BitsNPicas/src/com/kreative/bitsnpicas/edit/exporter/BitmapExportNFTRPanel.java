package com.kreative.bitsnpicas.edit.exporter;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.kreative.bitsnpicas.nftr.FINFChunk;

public class BitmapExportNFTRPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private final JComboBox<Integer> bitDepth;
	private final JComboBox<String> charEncoding;
	
	public BitmapExportNFTRPanel() {
		this.bitDepth = new JComboBox<Integer>(new Integer[] {
			1, 2, 3, 4, 5, 6, 7, 8
		});
		this.bitDepth.setSelectedIndex(2);
		this.charEncoding = new JComboBox<String>(new String[] {
			"UTF16",
			"Shift-JIS",
			"Windows-1252"
		});
		this.bitDepth.setSelectedIndex(0);
		JPanel bp = new JPanel(new GridLayout(2, 2, 4, 4));
		bp.add(new JLabel("Bit Depth"));
		bp.add(bitDepth);
		bp.add(new JLabel("Character Encoding"));
		bp.add(bitDepth);
		this.setLayout(new BorderLayout());
		this.add(bp, BorderLayout.PAGE_START);
	}
	
	public int getBitDepth() {
		return (int) bitDepth.getSelectedItem();
	}
	
	public int getCharEncoding() {
		switch ((String) charEncoding.getSelectedItem()) {
		case "UTF16":
			return FINFChunk.ENCODING_UTF16;
		case "Shift-JIS":
			return FINFChunk.ENCODING_SHIFTJIS;
		case "Windows-1252":
			return FINFChunk.ENCODING_WINDOWS1252;
		default:
			return FINFChunk.ENCODING_UTF16;
		}
	}
}
