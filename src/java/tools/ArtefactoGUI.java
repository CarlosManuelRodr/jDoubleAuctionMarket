package tools;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cartago.*;
import cartago.tools.*;
import env.DoubleAuctionMarketEnv;

public class ArtefactoGUI extends GUIArtifact
{
	private Window myWindow;
	
	public void setup()
	{
		myWindow = new Window();
		linkWindowClosingEventToOp(myWindow,"cerrar");
		linkActionEventToOp(myWindow.botonInit,"iniciar");
		defineObsProperty("zi", getZi());
		defineObsProperty("ma", getMa());
		defineObsProperty("useWl", getMa());
		myWindow.setVisible(true);
	}

	@INTERNAL_OPERATION void iniciar(ActionEvent ev)
	{
		getObsProperty("zi").updateValue(getZi());
		getObsProperty("ma").updateValue(getMa());
		getObsProperty("useWl").updateValue(getUseWl());
		signal("iniciar");
		myWindow.botonInit.setEnabled(false);
		
		if (myWindow.getUseWl())
			DoubleAuctionMarketEnv.getInstance().StartSimulation(getZi() + 1);
		else
			DoubleAuctionMarketEnv.getInstance().StartSimulation(getZi() + getMa());
	}
	
	@INTERNAL_OPERATION void cerrar(WindowEvent ev)
	{
		signal("cerrar");
	}
	
	@OPERATION void terminar()
	{
		myWindow.botonInit.setEnabled(true);
	}
	
	private int getZi()
	{
		return Integer.parseInt(myWindow.getZi());
	}
	
	private int getMa()
	{
		return Integer.parseInt(myWindow.getMa());
	}
	
	private int getUseWl()
	{
		if (myWindow.getUseWl())
			return 1;
		else
			return 0;
	}
	
	class Window extends JFrame implements ChangeListener
	{
		private static final long serialVersionUID = 1L;
		private JTextField nZi;
		private JTextField nMa;
		private JLabel lZi;
		private JLabel lMa;
		private JCheckBox useMathematica;
		private JButton botonInit; 
		
		public Window()
		{
			setTitle("Configuración");
			setSize(300,300);
			setResizable(false);
			JPanel panel = new JPanel();
			setContentPane(panel);
			botonInit = new JButton("Iniciar experimento");
			botonInit.setSize(100,50);
			lZi = new JLabel("Zero Intelligence:");
			lMa = new JLabel("Moving Average:");
			nZi = new JTextField(10);
			nZi.setText("75");
			nMa = new JTextField(10);
			nMa.setText("25");
			useMathematica = new JCheckBox("Agente Mathematica");
			useMathematica.addChangeListener(this);
			panel.add(lZi);
			panel.add(nZi);
			panel.add(lMa);
			panel.add(nMa);
			panel.add(useMathematica);
			panel.add(botonInit);
		}
		
		public void stateChanged(ChangeEvent e)
		{
			nMa.setEnabled(!useMathematica.isSelected());
	    }
		
		public String getZi()
		{
			return nZi.getText();
		}
		
		public String getMa()
		{
			return nMa.getText();
		}
		
		public Boolean getUseWl()
		{
			return useMathematica.isSelected();
		}
	}
}
