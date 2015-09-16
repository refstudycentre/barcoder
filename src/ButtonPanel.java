
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

public class ButtonPanel extends JPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -3395325800233237416L;
	
	/*
	 * Frequent access is needed to the buttons themselves,
	 * therefore a public variable is more suitable than a getter
	 * here.
	 */
	public JButton[] jb;

	/* 
	 * create a panel with buttons from the array of Strings, using
	 * the specified ActionListener for each button
	 */
	public ButtonPanel(ActionListener al, String[] buttons)
	{
		super();
		jb = new JButton[buttons.length];
		for (int i=0;i<buttons.length;i++)
		{
			jb[i]=new JButton(buttons[i]);
			jb[i].addActionListener(al);
			this.add(jb[i]);
		}
	}

	/* 
	 * create a panel with buttons from the array of Strings, using
	 * the specified ActionListener and tooltips for each button
	 */
	public ButtonPanel(ActionListener al, String[] buttons, String[] tooltips)
	{
		super();
		setOpaque(false);
		jb = new JButton[buttons.length];
		for (int i=0;i<buttons.length;i++)
		{
			jb[i]=new JButton(buttons[i]);
			jb[i].addActionListener(al);
			jb[i].setToolTipText(tooltips[i]);
			this.add(jb[i]);
		}
	}

	/* 
	 * create a panel with buttons from the array of Icons, using the
	 * specified ActionListener and tooltips for each button
	 */
	public ButtonPanel(ActionListener al, ImageIcon[] buttons, String[] tooltips)
	{
		super();
		setOpaque(false);
		jb = new JButton[buttons.length];
		for (int i=0;i<buttons.length;i++)
		{
			jb[i]=new JButton(buttons[i]);
			jb[i].addActionListener(al);
			jb[i].setToolTipText(tooltips[i]);
			this.add(jb[i]);
		}
	}
}