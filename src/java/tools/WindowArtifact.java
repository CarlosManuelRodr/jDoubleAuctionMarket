package tools;

import java.awt.event.*;
import javax.swing.*;
import cartago.*;
import cartago.tools.*;

public class WindowArtifact extends GUIArtifact {
	private Ventana inicio;
	
	public void setup() {
		inicio = new Ventana();
		linkWindowClosingEventToOp(inicio,"cerrar");
		linkActionEventToOp(inicio.botonInit,"iniciar");
		defineObsProperty("opBetaMin", ParseOpBetaMin());
		defineObsProperty("opBetaMax", ParseOpBetaMax());
		defineObsProperty("sellBetaMin", ParseSellBetaMin());
		defineObsProperty("sellBetaMax", ParseSellBetaMax());
		defineObsProperty("cashBetaMin", ParseCashBetaMin());
		defineObsProperty("cashBetaMax", ParseCashBetaMax());
		inicio.setVisible(true);
	}

	@INTERNAL_OPERATION void iniciar(ActionEvent ev) {
		getObsProperty("opBetaMin").updateValue(ParseOpBetaMin());
		getObsProperty("opBetaMax").updateValue(ParseOpBetaMin());
		getObsProperty("sellBetaMin").updateValue(ParseSellBetaMin());
		getObsProperty("sellBetaMax").updateValue(ParseSellBetaMax());
		getObsProperty("cashBetaMax").updateValue(ParseCashBetaMin());
		getObsProperty("cashBetaMax").updateValue(ParseCashBetaMax());
		signal("iniciar");
		inicio.botonInit.setEnabled(false);
	}
	
	@INTERNAL_OPERATION void cerrar(WindowEvent ev) {
		signal("cerrar");
	}
	
	@OPERATION void terminar() {
		inicio.botonInit.setEnabled(true);
	}
	
	private double ParseOpBetaMin() {
		return Double.parseDouble(inicio.GetOpBetaMin());
	}
	
	private double ParseOpBetaMax() {
		return Double.parseDouble(inicio.GetOpBetaMax());
	}
	
	private double ParseSellBetaMin() {
		return Double.parseDouble(inicio.GetSellBetaMin());
	}
	
	private double ParseSellBetaMax() {
		return Double.parseDouble(inicio.GetSellBetaMax());
	}
	
	private double ParseCashBetaMin() {
		return Double.parseDouble(inicio.GetCashBetaMin());
	}
	
	private double ParseCashBetaMax() {
		return Double.parseDouble(inicio.GetCashBetaMax());
	}
	
	class Ventana extends JFrame {
		private static final long serialVersionUID = 1L;
		private JTextField noInd;
		private JTextField paramF;
		private JTextField paramCR;
		private JTextField ruta;
		private JTextField gen;
		private JTextField precision;
		private JLabel pInd;
		private JLabel pF;
		private JLabel pCR;
		private JLabel lRuta;
		private JLabel lGen;
		private JLabel lPrecision;
		private JButton botonInit; 
		
		public Ventana(){
			setTitle("Configuración");
			setSize(300,300);
			setResizable(false);
			JPanel panel = new JPanel();
			setContentPane(panel);
			botonInit = new JButton("Iniciar experimento");
			botonInit.setSize(100,50);
			pInd = new JLabel("opBetaMin:");
			pF = new JLabel("opBetaMax:");
			pCR = new JLabel("sellBetaMin:");
			lRuta = new JLabel("sellBetaMax:");
			lGen = new JLabel("cashBetaMin:");
			lPrecision = new JLabel("cashBetaMax:");
			noInd = new JTextField(10);
			noInd.setText("100");
			paramF = new JTextField(10);
			paramF.setText("0.75");
			paramCR = new JTextField(10);
			paramCR.setText("0.5");
			ruta = new JTextField(10);
			ruta.setText("");
			gen = new JTextField(10);
			gen.setText("100");
			precision = new JTextField(10);
			precision.setText("90");
			panel.add(pInd);
			panel.add(noInd);
			panel.add(pF);
			panel.add(paramF);
			panel.add(pCR);
			panel.add(paramCR);
			panel.add(lGen);
			panel.add(gen);
			panel.add(lPrecision);
			panel.add(precision);
			panel.add(lRuta);
			panel.add(ruta);
			panel.add(botonInit);
		}
		
		public String GetOpBetaMin() {
			return noInd.getText();
		}
		
		public String GetOpBetaMax() {
			return paramF.getText();
		}
		
		public String GetSellBetaMin() {
			return paramCR.getText();
		}
		
		public String GetSellBetaMax() {
			return ruta.getText();
		}
		
		public String GetCashBetaMin() {
			return gen.getText();
		}
		
		public String GetCashBetaMax() {
			return precision.getText();
		}
	}
}

