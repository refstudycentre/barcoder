import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.itextpdf.text.FontFactory;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;

class TextPanel extends JPanel
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -322680882314714434L;
	
	private TextPanel self;
//	private JFrame window;
	
	private JFileChooser jfc = new JFileChooser(new File("."));
	JList<String> previewList;
	JTextField regexField;
	
	private File inputFile = null;
	private String regex = "[\\S]+"; // not whitespace
	private SettingsPanel sp;
	private Vector<String> currentListData;
	
	ActionListener al = new ActionListener()
	{
		public void actionPerformed(ActionEvent e)
		{
			String ac = e.getActionCommand();
			if (ac.equals("Choose file ..."))
			{
				if (jfc.showOpenDialog(self) == 0)
				{
					inputFile = jfc.getSelectedFile();
					updatePreview();
				}
			}
			else if (ac.equals("Update regex"))
			{
				regex = regexField.getText().trim();
				updatePreview();
			}
		}
	};
	
	TextPanel(JFrame window, int padding, int cols)
	{
		super(new BorderLayout(padding,padding));
		setBorder(new EmptyBorder(padding,padding,padding,padding));
		this.self = this;
//		this.window = window;
		
		JPanel jpNorth = new JPanel(new BorderLayout(padding,padding));
		JButton chooseBtn = new JButton("Choose file ...");
		chooseBtn.addActionListener(al);
		chooseBtn.setToolTipText("Open a dialog to choose an input file.");
		jpNorth.add(chooseBtn,BorderLayout.NORTH);
		
		String[] labels = new String[]{"Font","Font size (pt)","Max chars per line (0=no limit)"};
		int[] types = new int[]{SettingsPanel.F_STRING,SettingsPanel.F_DOUBLE,SettingsPanel.F_INTEGER};
		int[] components = new int[]{SettingsPanel.C_COMBOBOX,SettingsPanel.C_TEXTFIELD,SettingsPanel.C_TEXTFIELD};
		String[] defaults = new String[]{"courier-bold","12","4"};
		sp = new SettingsPanel(labels, cols, types, components, "settings", "txtInput", defaults);
		sp.setComboOptions(FontFactory.getRegisteredFonts().toArray(new String[0]), 0); // set font types
		sp.getField(2).setToolTipText("<html>Setting this to 0 will impose no limit.<br/>Even with a limit, normal character wrapping<br/>still applies and the text will not overflow<br/>the label boundaries.</html>");
		jpNorth.add(sp,BorderLayout.CENTER);
		
		add(jpNorth,BorderLayout.NORTH);
		
		previewList = new JList<String>();
		JScrollPane jspList = new JScrollPane(previewList);
		add(jspList,BorderLayout.CENTER);
		
		JPanel regexPanel = new JPanel(new FlowLayout());
		JLabel regexLabel = new JLabel("Regular expression to match data:");
		this.regexField = new JTextField(regex,10);
		JButton updateBtn = new JButton("Update regex");
		updateBtn.addActionListener(al);
		updateBtn.setToolTipText("Re-read the file, using the entered regular expression.");
		regexPanel.add(regexLabel);
		regexPanel.add(regexField);
		regexPanel.add(updateBtn);
		add(regexPanel,BorderLayout.SOUTH);
	}
	
	public String[] getSettings()
	{
		return sp.getAll();
	}
	
	public String[] getTxts()
	{
		if (null != currentListData && currentListData.size() > 0)
		{
			return currentListData.toArray(new String[0]);
		}
		return null;
	}
	
	private void updatePreview()
	{
		if (null != inputFile)
		{
			final ProgressMonitor mon = new ProgressMonitor(self,"Updating preview...","There is really no telling just how long this will take!",0,100);
			mon.setProgress(0);
			final Previewer pre = new Previewer(regex,inputFile,mon,previewList,self);
			pre.execute();
		}
	}
	
	private static class Previewer extends SwingWorker<Vector<String>, Void>
	{
		String regex;
		File inputFile;
		ProgressMonitor mon;
		JList<String> previewList;
		TextPanel self;
		
		Previewer(String regex, File inputFile, ProgressMonitor mon, JList<String> previewList, TextPanel self)
		{
			this.regex=regex;
			this.inputFile=inputFile;
			this.mon=mon;
			this.previewList=previewList;
			this.self = self;
		}
		
		@Override
		protected Vector<String> doInBackground()
		{
			int size = 2048; // safe to assume, no usable label will have a word of more characters than this
			char[] buffer = new char[size];
			RegularExpression re = new RegularExpression(regex);
			Match m = new Match();
			int start = 0;
			int unused = 0;
			Vector<String> matches = new Vector<String>();
			
			try
			{
				FileReader fr = new FileReader(inputFile);
				while (fr.ready() && !mon.isCanceled())
				{
					char[] newbuffer = new char[unused+size];
					System.arraycopy(buffer,size-unused,newbuffer,0,unused);
					fr.read(newbuffer,unused,size);				
					buffer = newbuffer;
					
					while (re.matches(buffer, start, size, m) && m.getEnd(0) != size && !mon.isCanceled())
					{
						start=m.getEnd(0);
						matches.add(m.getCapturedText(0));
					}
					unused = size-start;
					start=0;
				}
				fr.close();
				
				return matches;
			}
			catch (FileNotFoundException e)
			{
				error("Can't find file to open!\n"+e.getMessage());
			}
			catch (IOException e)
			{
				error("Can't read from file!\n"+e.getMessage());
			}
			return null;
		}

        @Override
        public void done()
        {
        	mon.close();
        	try
    		{
        		Vector<String> newListData = get();
    			previewList.setListData(newListData);
    			self.currentListData = newListData;
    		}
    		catch (CancellationException e)
    		{
    			// user cancelled
    		}
    		catch(InterruptedException e)
    		{
    			error("Could not update preview."+e.getMessage());
    		}
    		catch (ExecutionException e)
    		{
    			error("Could not update preview."+e.getMessage());
    		}
        }
	}
	
	private static void error(String msg) // TODO: move these to a common class instead of repeating
	{
		JOptionPane.showMessageDialog(null,msg,"ERROR!",JOptionPane.ERROR_MESSAGE);
	}
	
//	private boolean confirm(String msg)
//	{
//		return (JOptionPane.showConfirmDialog(this, msg, "Please confirm!", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) == 0;
//	}
}
