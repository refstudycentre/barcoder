import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class LabelTextFieldPanel extends JPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4916685351630201852L;
	
	private JLabel[] jl;
	private JPanel jtfields;
	private JPanel jlabels;
	
	/*
	 * Frequent access is needed to the textfield itself, therefore a
	 * public variable is more suitable than a getter here.
	 */
	public JTextField[] jtf;
	
	public LabelTextFieldPanel(String[] labels, int cols)
	{
		super(new BorderLayout(5,5));
		setOpaque(false);
		int ll = labels.length;
		GridLayout gl = new GridLayout(ll,1,5,5);
		jl = new JLabel[ll];
		jlabels = new JPanel(gl);
		jlabels.setOpaque(false);
		this.add(jlabels,BorderLayout.WEST);
		jtf = new JTextField[ll];
		jtfields = new JPanel(gl);
		this.add(jtfields,BorderLayout.CENTER);
		for (int i=0;i<ll;i++)
		{
			jl[i]=new JLabel(labels[i]);
			jl[i].setOpaque(false);
			jlabels.add(jl[i]);
			jtf[i]=new JTextField(cols);
			jtfields.add(jtf[i]);
		}
	}
	
	// replace a component in the panel with the specified component
	public void replace(int index, JComponent jc)
	{
		jtfields.remove(index);
		jtfields.add(jc,index);
	}
}