import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.krysalis.barcode4j.impl.code39.Code39Bean;

import com.itextpdf.text.FontFactory;

class BarcodesPanel extends JPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4931889225195998228L;
	
	private JPanel fields = new JPanel();
	private ButtonPanel bottomBtns;
	private ButtonPanel sideBtns;
	private SettingsPanel sp;
	private JComboBox<String> field_types;
	
	private JFrame window;
	private int cols;
	private int padding;
	
	ActionListener al = new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			String ac = e.getActionCommand();
			if (ac.equals("Add field"))
			{
				String type = field_types.getSelectedItem().toString();
				if (type.equals("Static text"))
				{
					addField(new StaticField());
				}
				else if (type.equals("Counter"))
				{
					addField(new CounterField());
				}
			}
			else if (ac.equals("Remove checked fields"))
			{
				Component[] comps = fields.getComponents();
				for (Component comp : comps)
				{
					if (Field.class.isInstance(comp) && ((Field)comp).chk.isSelected())
					{
						removeField((Field) comp);
					}
				}
			}
			// "\u2191","\u2193"
			else if (ac.equals("\u2191")) // UP
			{
				// for now, move only first selected field
				Component[] comps = fields.getComponents();
				fields.removeAll();
				
				for (int i=1;i<comps.length;i++)
				{
					if (Field.class.isInstance(comps[i]) && ((Field)comps[i]).chk.isSelected())
					{	
						Component t = comps[i];
						comps[i] = comps[i-1];
						comps[i-1] = t;
						
						i=comps.length; // break out
					}
				}
				
				for (int i=0;i<comps.length;i++)
				{
					fields.add(comps[i]);
				}
				window.repaint();
			}
			else if (ac.equals("\u2193")) // DOWN
			{
				// for now, move only first selected field
				Component[] comps = fields.getComponents();
				fields.removeAll();
				
				for (int i=0;i<comps.length-1;i++)
				{
					if (Field.class.isInstance(comps[i]) && ((Field)comps[i]).chk.isSelected())
					{	
						Component t = comps[i];
						comps[i] = comps[i+1];
						comps[i+1] = t;
						
						i=comps.length; // break out
					}
				}
				
				for (int i=0;i<comps.length;i++)
				{
					fields.add(comps[i]);
				}
				window.repaint();
			}
		}
	};
	
	BarcodesPanel(JFrame window, int cols, int padding)
	{
		super(new BorderLayout(padding,padding));
		setBorder(new EmptyBorder(padding,padding,padding,padding));
		
		this.window = window;
		this.cols = cols;
		this.padding = padding;
		
		String[] labels = new String[]{
			"Font",
			"Font size (pt)",
			"DPI",
			"Bar height (mm)",
			"Full height (mm)",
			"Module width (mm)",
			"Text placement",
			"Text pattern",
			"'Quiet zone' width (mm)",
			"'Quiet zone' height (mm)"
		};
		Code39Bean bean = new Code39Bean();
		String[] defaults = new String[]{
			bean.getFontName(),
			""+bean.getFontSize(),
			""+300,
			""+bean.getBarHeight(),
			""+bean.getHeight(),
			""+bean.getModuleWidth(),
			"Bottom",
			bean.getPattern(),
			""+bean.getQuietZone(),
			""+bean.getVerticalQuietZone()
		};
		int[] types = new int[]{
			SettingsPanel.F_STRING,
			SettingsPanel.F_DOUBLE,
			SettingsPanel.F_INTEGER,
			SettingsPanel.F_DOUBLE,
			SettingsPanel.F_DOUBLE,
			SettingsPanel.F_DOUBLE,
			SettingsPanel.F_STRING,
			SettingsPanel.F_STRING,
			SettingsPanel.F_DOUBLE,
			SettingsPanel.F_DOUBLE
		};
		int[] components = new int[]{
			SettingsPanel.C_COMBOBOX,
			SettingsPanel.C_TEXTFIELD,
			SettingsPanel.C_TEXTFIELD,
			SettingsPanel.C_TEXTFIELD,
			SettingsPanel.C_TEXTFIELD,
			SettingsPanel.C_TEXTFIELD,
			SettingsPanel.C_COMBOBOX,
			SettingsPanel.C_TEXTFIELD,
			SettingsPanel.C_TEXTFIELD,
			SettingsPanel.C_TEXTFIELD
		};
		
		sp = new SettingsPanel(labels, cols, types, components, "settings", "barcode", defaults);
		sp.setComboOptions(FontFactory.getRegisteredFonts().toArray(new String[0]), 0); // set font types
		sp.setComboOptions(new String[]{"Bottom","None","Top"}, 6); // set positions
		sp.setBorder(new TitledBorder("Settings:"));
		add(sp,BorderLayout.NORTH);
		
		fields.setLayout(new BoxLayout(fields,BoxLayout.Y_AXIS));
		JScrollPane jspFields = new JScrollPane(fields,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jspFields.setBorder(new TitledBorder("Fields:"));
		add(jspFields,BorderLayout.CENTER);
		
		bottomBtns = new ButtonPanel(al,new String[]{"Add field","Remove checked fields"});
//		bottomBtns = new ButtonPanel(al,new String[]{"Add field","Remove checked fields","Generate!","Quit"});
		sideBtns = new ButtonPanel(al,new String[]{"\u2191","\u2193"},new String[]{"Move field up","Move field down"});
		
		field_types = new JComboBox<String>(new String[]{"Static text","Counter"});
		bottomBtns.add(field_types, 0);
		
		BoxLayout sideBtnsLayout = new BoxLayout(sideBtns, BoxLayout.Y_AXIS);
		sideBtns.setLayout(sideBtnsLayout);
		// TODO: center sideBtns vertically
		
		add(bottomBtns,BorderLayout.SOUTH);
		add(sideBtns,BorderLayout.EAST);
	}
	
	void addField(Field field)
	{
		fields.add(field);
		window.repaint();
	}
	
	void removeField(Field field)
	{
		fields.remove(field);
		window.repaint();
	}
	
	public String[] getSettings()
	{
		return sp.getAll();
	}
	
	static class BarcodeComputer extends SwingWorker<String[], Integer>
	{
		private ProgressMonitor mon;
		private JPanel fields;
		
		BarcodeComputer(ProgressMonitor mon, JPanel fields)
		{
			this.mon = mon;
			this.fields = fields;
		}
		
        public String[] doInBackground()
        {
        	int progress = 0;
            publish(progress);
            
         // compute barcodes		
    		ArrayList<String> barcodes = new ArrayList<String>();
    		Component[] comps = fields.getComponents();
    		for (Component field : comps)
    		{
    			if (isCancelled())
    			{
    				return null;
    			}
    			
    			if (StaticField.class.isInstance(field))
    			{
    				String text = ((StaticField)field).getText();
    				
    				if (barcodes.size()==0)
    				{
    					barcodes.add(text);
    					
    					publish(++progress);
    					if (isCancelled())
    	    			{
    	    				return null;
    	    			}
    				}
    				else
    				{
    					for (int i=0;i<barcodes.size();i++)
    					{
    						barcodes.set(i, barcodes.get(i)+text);
    					}
    				}
    			}
    			else if (CounterField.class.isInstance(field))
    			{
    				int[] nums = ((CounterField)field).getNums();
    				if (null == nums)
    				{
    					return null;
    				}
    				
    				int from = nums[0];
    				int to = nums[1];
    				int step = Math.abs(nums[2]);
    				int digits = nums[3];
    				
    				if (barcodes.size()==0)
    				{
    					if (from > to) // reverse
    					{
    						for (int i=from;i>=to;i-=step)
    						{
    							barcodes.add(String.format("%0"+digits+"d",i));
    							progress++;
    						}
    						
    						publish(progress);
        					if (isCancelled())
        	    			{
        	    				return null;
        	    			}
    					}
    					else
    					{
    						for (int i=from;i<=to;i+=step)
    						{
    							barcodes.add(String.format("%0"+digits+"d",i));
    							progress++;
    						}
    						
    						publish(progress);
        					if (isCancelled())
        	    			{
        	    				return null;
        	    			}
    					}
    				}
    				else
    				{
    					ArrayList<String> new_barcodes = new ArrayList<String>();
    					for (int i=0;i<barcodes.size();i++)
    					{
    						if (from > to) // reverse
    						{
    							for (int j=from;j>=to;j-=step)
    							{
    								new_barcodes.add(barcodes.get(i)+String.format("%0"+digits+"d",j));
    								progress++;
    							}
    							
    							publish(progress);
            					if (isCancelled())
            	    			{
            	    				return null;
            	    			}
    						}
    						else
    						{
    							for (int j=from;j<=to;j+=step)
    							{
    								new_barcodes.add(barcodes.get(i)+String.format("%0"+digits+"d",j));
    								progress++;
    							}
    							
    							publish(progress);
            					if (isCancelled())
            	    			{
            	    				return null;
            	    			}
    						}
    					}
    					barcodes = new_barcodes;
    				}
    			}
    		}
    		
			if (isCancelled())
			{
				return null;
			}
    		return barcodes.toArray(new String[0]);
        }

        @Override
		protected void process(List<Integer> chunks)
        {
			super.process(chunks);
			if (mon.isCanceled())
			{
				this.cancel(true);
				mon.close();
			}
			else
			{
				mon.setProgress(chunks.get(chunks.size()-1));
			}
		}
        
        @Override
        public void done()
        {
        	super.done();
        	mon.close();
        }
    }
	
	public String[] getBarcodes()
	{
		// estimate number of barcodes
		int n = 1;
		Component[] comps = fields.getComponents();
		for (Component field : comps)
		{
			if (CounterField.class.isInstance(field))
			{
				int[] nums = ((CounterField)field).getNums();
				if (null != nums)
				{
					int from = nums[0];
					int to = nums[1];
					int step = Math.abs(nums[2]);
					n *= Math.abs(from-to) / step;
				}
			}
		}
		
		final ProgressMonitor mon = new ProgressMonitor(this,"Computing barcode numbers...","",0,n);
		mon.setProgress(0);
		final BarcodeComputer bc = new BarcodeComputer(mon, fields);
		bc.execute();
		
		try
		{
			return bc.get();
		}
		catch (CancellationException e)
		{
			// user cancelled
		}
		catch (InterruptedException e)
		{
			error("Could not finish computing barcode numbers.\n"+e.getMessage());
		}
		catch (ExecutionException e)
		{
			error("Could not finish computing barcode numbers.\n"+e.getMessage());
		}
		return null;
	}

	class Field extends JPanel
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1171131479785821566L;
		
		JCheckBox chk;
		Field()
		{
			super(new BorderLayout(padding,padding));
			chk = new JCheckBox();
			add (chk,BorderLayout.EAST);
		}
	}

	class StaticField extends Field
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 7650856916083041178L;
		
		JLabel statictextLabel = new JLabel("Static text:");
		JTextField statictext = new JTextField(cols);
		StaticField()
		{
			super();
			add(statictextLabel,BorderLayout.WEST);
			add(statictext,BorderLayout.CENTER);
		}
		public String getText()
		{
			return statictext.getText();
		}
	}

	class CounterField extends Field
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -6648666224114220745L;
		
		JLabel counterLabel = new JLabel("Counter");
		LabelTextFieldPanel nums = new LabelTextFieldPanel(new String[]{"From:","To:","Step:","Digits:"},cols);
		CounterField()
		{
			super();
			add(counterLabel,BorderLayout.WEST);
			nums.setBorder(new LineBorder(Color.black));
			add(nums,BorderLayout.CENTER);
		}
		public int[] getNums()
		{
			try
			{
				String fromTxt = nums.jtf[0].getText();
				String toTxt = nums.jtf[1].getText();
				int from = Integer.parseInt(fromTxt);
				int to = Integer.parseInt(toTxt);
				int step = Integer.parseInt(nums.jtf[2].getText());
				int digits = Integer.parseInt(nums.jtf[3].getText());
				
				if (fromTxt.length() > digits || toTxt.length() > digits)
				{
					if (!confirm("One of your counters do not have enough digits to contain the largest possible number.\nThis will cause some barcodes to be longer than others.\nDo you really want to do this?"))
					{
						return null;
					}
				}
				return new int[]{from,to,step,digits};
			}
			catch (NumberFormatException e)
			{
				error("Counter fields should contain only integers!\n"+e.getMessage());
			}
			return null;
		}
	}
	
	private void error(String msg)
	{
		JOptionPane.showMessageDialog(this,msg,"ERROR!",JOptionPane.ERROR_MESSAGE);
	}
	
	private boolean confirm(String msg)
	{
		return (JOptionPane.showConfirmDialog(this, msg, "Please confirm!", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) == 0;
	}
}
