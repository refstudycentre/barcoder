import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Scanner;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SettingsPanel extends JPanel
{	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6655154288676059513L;
	
	private JComponent self;
	
	private JLabel[] jLabels;
	private JComponent[] jFields;
	private JPanel jpFields;
	private JPanel jpLabels;
	private JPanel jpFiles;
	private JComboBox<String> jcbFiles;
	
	private int[] types;
	private int[] comps;
	private String folder;
	private FilenameFilter filter;
	private String ext;
	JFileChooser jfc = new JFileChooser(new File("."));
	
	// field types:
	static final int F_STRING = 0;
	static final int F_INTEGER = 1;
	static final int F_DOUBLE = 2;
	
	// component types:
	static final int C_TEXTFIELD = 0;
	static final int C_COMBOBOX = 1;
	
	// ActionListener for files buttons
	ActionListener alFiles = new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			String ac = e.getActionCommand();
			String selectedFile = (String)jcbFiles.getSelectedItem();
			
			//{"Load","Open...","Save...","Delete"}
			switch(ac) // YAY
			{
			case "Load":
				if (null != selectedFile)
				{
					try
					{
						loadSettings(new File("."+File.separatorChar+folder+File.separatorChar+selectedFile));
					}
					catch (IOException e1)
					{
						error("Cannot load file!\n"+e1.getMessage());
					}
				}
				else
				{
					error("No file selected!");
				}
				break;
			case "Open...":
				if (jfc.showOpenDialog(self) == 0)
				{
					try
					{
						loadSettings(jfc.getSelectedFile());
					}
					catch (IOException e1)
					{
						error("Cannot load file!\n"+e1.getMessage());
					}
				}
				break;
			case "Save...":
				if (checkSettings())
				{	
					String fname = JOptionPane.showInputDialog(self, "Please enter a name (without extention)\nfor the settings file:", "Save...", JOptionPane.QUESTION_MESSAGE);
					if (null != fname) // else operation has been cancelled
					{
						fname = fname.trim();
						if (!fname.equals(""))
						{
								File newFile = new File("."+File.separatorChar+folder+File.separatorChar+fname+"."+ext);
								if (!newFile.exists() || confirm("Overwrite existing file named:\n"+fname+"."+ext+"\n?"))
								{
									try
									{
										saveSettings(newFile);
										populateFileList();
									}
									catch (IOException e1)
									{
										error("Cannot save file!\n"+e1.getMessage());
									}
								}
						}
						else
						{
							error("No file name? WHYYYYYYY??? :'(");
						}
					}
				}
				else
				{
					error("Some fields contain invalid data!\nAlways check the number fields..."); // TODO: be more specific?
				}

				break;
			case "Delete":
				if (null != selectedFile)
				{
					if (confirm("Really delete settings file:\n"+selectedFile+"\n?"))
					{
						new File("."+File.separatorChar+folder+File.separatorChar+selectedFile).delete();
						populateFileList();
					}
				}
				else
				{
					error("No file selected!");
				}
				break;
			}
		}
	};
	
	/**
	 * 
	 * @param labels
	 * Label for each field
	 * @param cols
	 * Number of columns for text fields
	 * @param types
	 * Type of content for each field. Each index is one of:
	 * SettingsPanel.F_STRING,
	 * SettingsPanel.F_INTEGER or
	 * SettingsPanel.F_DOUBLE
	 * @param components
	 * Type of component to use for field. Each index is one of:
	 * SettingsPanel.C_TEXTFIELD or
	 * SettingsPanel.C_COMBOBOX
	 */
	public SettingsPanel(String[] labels, int cols, int[] types, int[] components, String folder, final String fileExtFilter, String[] defaults)
	{
		super(new BorderLayout(5,5));
		this.self = this;
		
		// check arguments
		if ((labels.length != types.length) || (labels.length != components.length))
		{
			throw new IllegalArgumentException("Length of argument arrays are not the same.");
		}
		
		// set some variables
		this.types = types;
		this.comps = components;
		this.folder = folder;
		this.ext = fileExtFilter;
		int n = labels.length;				// number of fields
		jLabels = new JLabel[n];
		jFields = new JComponent[n];
		
		// create panels
		GridLayout gl = new GridLayout(n,1,5,5);
		jpLabels = new JPanel(gl);
		jpFields = new JPanel(gl);
		jpFiles = new JPanel();
		
		// lay out panels
		setOpaque(false);
		jpLabels.setOpaque(false);
		this.add(jpLabels,BorderLayout.WEST);
		this.add(jpFields,BorderLayout.CENTER);
		this.add(jpFiles,BorderLayout.SOUTH);
		
		// create labels and fields
		for (int i=0;i<n;i++)
		{
			jLabels[i]=new JLabel(labels[i]);
			switch(components[i])
			{
			case SettingsPanel.C_TEXTFIELD:
				jFields[i] = new JTextField(cols);
				break;
			case SettingsPanel.C_COMBOBOX:
				switch(types[i])
				{
				case SettingsPanel.F_STRING:
					jFields[i] = new JComboBox<String>();
					break;
				case SettingsPanel.F_INTEGER:
					jFields[i] = new JComboBox<Integer>();
					break;
				case SettingsPanel.F_DOUBLE:
					jFields[i] = new JComboBox<Double>();
					break;
				}
				break;
			}
			
			jLabels[i].setOpaque(false);
			jpLabels.add(jLabels[i]);
			jpFields.add(jFields[i]);
		}
		
		// set filter for settings files
		filter = new FilenameFilter(){
			public boolean accept(File dir, String name)
			{
				return name.endsWith("."+ext);
			}
		};
		jcbFiles = new JComboBox<String>();
		jcbFiles.setPreferredSize(new Dimension(150, jcbFiles.getPreferredSize().height));
		
		populateFileList();
		setAll(defaults); // FIXME: Combo options cannot be set before constructing, so their defaults are never set!
		
		// lay out files panel
		jpFiles.add(new JLabel("Settings files:"));
		jpFiles.add(jcbFiles);
		jpFiles.add(new ButtonPanel(alFiles, new String[]{"Load","Open...","Save...","Delete"}, new String[]{"Load the selected settings file.","Open a settings file that is not in the quick-load list.","Save the current settings to a new file.","Delete the selected settings file."}));
	}
	
	@SuppressWarnings("unchecked")
	public String getCheckedField(int i)
	{
		String val = null;
		
		if (checkSetting(i))
		{
			switch(comps[i])
			{
			case SettingsPanel.C_COMBOBOX:
				val = (((JComboBox<String>) jFields[i]).getSelectedItem() + "").trim();
				break;
			case SettingsPanel.C_TEXTFIELD:
				val = ((JTextField) jFields[i]).getText().trim();
				break;
			}
		}
		else
		{
			throw new NumberFormatException("Invalid format for '"+val+"'");
		}
		
		return val;
	}
	
	@SuppressWarnings("unchecked")
	public String getUncheckedField(int i)
	{
		String val = null;
		
		switch(comps[i])
		{
		case SettingsPanel.C_COMBOBOX:
			val = (((JComboBox<String>) jFields[i]).getSelectedItem() + "").trim();
			break;
		case SettingsPanel.C_TEXTFIELD:
			val = ((JTextField) jFields[i]).getText().trim();
			break;
		}
		
		return val;
	}
	
	public String[] getAll()
	{
		String[] values = new String[types.length];
		for (int i=0;i<types.length;i++)
		{
			values[i] = getCheckedField(i);
		}
		return values;
	}
	
	public void setAll(String[] settings) throws IllegalArgumentException
	{
		if (settings.length == types.length)
		{
			for (int i=0;i<types.length;i++)
			{
				setField(settings[i],i);
			}
		}
		else
		{
			throw new IllegalArgumentException("Length of settings array does not match number of fields.");
		}
	}
	
	@SuppressWarnings("unchecked")
	public void setField(String val, int i)
	{
		if (null != val && !val.trim().equals("")) // else ignore
		{
			Object item = null;
			
			try // this is for conversion as well as type checking!
			{
				switch(types[i])
				{
				case SettingsPanel.F_STRING:
					item = val;
					break;
				case SettingsPanel.F_INTEGER:
					item = new Integer(val);
					break;
				case SettingsPanel.F_DOUBLE:
					item = new Double(val);
					break;
				}
			}
			catch (NumberFormatException e1)
			{
				error("Problem with setting '"+val+"':\n"+e1.getMessage());
				e1.printStackTrace();
			}
			
			switch(comps[i])
			{
			case SettingsPanel.C_COMBOBOX:
				((JComboBox<String>) jFields[i]).setSelectedItem(item); // TODO: what if the item is not in the combobox? nothing happens...
				break;
			case SettingsPanel.C_TEXTFIELD:
				((JTextField) jFields[i]).setText(val);
				break;
			}
		}
	}
	
	private void populateFileList()
	{
		// get valid settings files
		String[] list = new File("."+File.separatorChar+folder).list(filter);
		jcbFiles.removeAllItems();
		if (null != list)
		{
			for (String item : list)
			{
				jcbFiles.addItem(item);
			}
		}
	}

	/**
	 * Set the items in a JComboBox in the SettingsPanel
	 * @param options
	 * @param fieldIndex
	 */
	public void setComboOptions(String[] options, int fieldIndex)
	{
		if (comps[fieldIndex] == SettingsPanel.C_COMBOBOX)
		{
			if (types[fieldIndex] == SettingsPanel.F_STRING)
			{
				@SuppressWarnings("unchecked") // this is checked with if above
				JComboBox<String> jcb = (JComboBox<String>) jFields[fieldIndex];
				
				jcb.removeAllItems();
				for (int i=0;i<options.length;i++)
				{
					jcb.addItem(options[i]);
				}
			}
			else
			{
				throw new IllegalArgumentException("Incorrect type of data for this combo box.");
			}
		}
		else
		{
			throw new IllegalArgumentException("Argument fieldIndex is not the index of a combo box.");
		}
	}
	
	/**
	 * Set the items in a JComboBox in the SettingsPanel
	 * @param options
	 * @param fieldIndex
	 */
	public void setComboOptions(int[] options, int fieldIndex)
	{
		if (comps[fieldIndex] == SettingsPanel.C_COMBOBOX)
		{
			if (types[fieldIndex] == SettingsPanel.F_INTEGER)
			{
				@SuppressWarnings("unchecked") // this is checked with if above
				JComboBox<Integer> jcb = (JComboBox<Integer>) jFields[fieldIndex];
				
				jcb.removeAllItems();
				for (int i=0;i<options.length;i++)
				{
					jcb.addItem(options[i]);
				}
			}
			else
			{
				throw new IllegalArgumentException("Incorrect type of data for this combo box.");
			}
		}
		else
		{
			throw new IllegalArgumentException("Argument fieldIndex is not the index of a combo box.");
		}
	}
	
	/**
	 * Set the items in a JComboBox in the SettingsPanel
	 * @param options
	 * @param fieldIndex
	 */
	public void setComboOptions(double[] options, int fieldIndex)
	{
		if (comps[fieldIndex] == SettingsPanel.C_COMBOBOX)
		{
			if (types[fieldIndex] == SettingsPanel.F_DOUBLE)
			{
				@SuppressWarnings("unchecked") // this is checked with if above
				JComboBox<Double> jcb = (JComboBox<Double>) jFields[fieldIndex];
				
				jcb.removeAllItems();
				for (int i=0;i<options.length;i++)
				{
					jcb.addItem(options[i]);
				}
			}
			else
			{
				throw new IllegalArgumentException("Incorrect type of data for this combo box.");
			}
		}
		else
		{
			throw new IllegalArgumentException("Argument fieldIndex is not the index of a combo box.");
		}
	}

	/**
	 * Test client for SettingsPanel
	 * @param args
	 */
	/*
	public static void main(String[] args)
	{
		JFrame jf = new JFrame("SettingsPanel");
		
		String[] labels = {"Name","Surname","Telephone","Cellphone","Year of Birth","Month of birth","Day of birth","Random double"};
		int cols = 20;
		int[] types = {SettingsPanel.F_STRING,SettingsPanel.F_STRING,SettingsPanel.F_STRING,SettingsPanel.F_STRING,SettingsPanel.F_INTEGER,SettingsPanel.F_INTEGER,SettingsPanel.F_INTEGER,SettingsPanel.F_DOUBLE};
		int[] components = {SettingsPanel.C_TEXTFIELD,SettingsPanel.C_TEXTFIELD,SettingsPanel.C_TEXTFIELD,SettingsPanel.C_TEXTFIELD,SettingsPanel.C_COMBOBOX,SettingsPanel.C_COMBOBOX,SettingsPanel.C_COMBOBOX,SettingsPanel.C_COMBOBOX};
		
		String[] defaults = {"'n naam","'n van","021 123 4567","082 123 4567","1990","01","01","3.6"};
		SettingsPanel sp = new SettingsPanel(labels, cols, types, components, "sptest", "ext",defaults);
		
		int[] years = new int[100];
		int year = 1920;
		for (int i=0;i<100;i++)
		{
			years[i] = year++; 
		}
		sp.setComboOptions(years, 4);
		
		sp.setComboOptions(new int[]{1,2,3,4,5,6,7,8,9,10,11,12}, 5);
		sp.setComboOptions(new int[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31}, 6);
		sp.setComboOptions(new double[]{1,-12.25,30.0}, 7);
		
		jf.add(sp);
//		jf.setSize(600,480);
		jf.pack();

		jf.setVisible(true);
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}*/
	
	@SuppressWarnings("unchecked")
	public void loadSettings(File f) throws FileNotFoundException
	{
		Scanner sc = new Scanner(f);
		for (int i=0;i<types.length && sc.hasNext();i++)
		{
			String val = sc.nextLine();
			Object item = null;
			
			try // this is for conversion as well as type checking!
			{
				switch(types[i])
				{
				case SettingsPanel.F_STRING:
					item = val;
					break;
				case SettingsPanel.F_INTEGER:
					item = new Integer(val);
					break;
				case SettingsPanel.F_DOUBLE:
					item = new Double(val);
					break;
				}
			}
			catch (NumberFormatException e1)
			{
				error("Problem with file content! Not all settings have been loaded.\n"+e1.getMessage());
			}
			
			switch(comps[i])
			{
			case SettingsPanel.C_COMBOBOX:
				((JComboBox<String>) jFields[i]).setSelectedItem(item); // TODO: what if the item is not in the combobox? nothing happens...
				break;
			case SettingsPanel.C_TEXTFIELD:
				((JTextField) jFields[i]).setText(val);
				break;
			}
		}
		sc.close();
	}
	
	private boolean checkSettings()
	{
		boolean valid = true;
		for (int i=0;i<types.length;i++)
		{
			if (!checkSetting(i))
			{
				valid = false;
				i=types.length;
			}
		}
		return valid;
	}
	
	private boolean checkSetting(int i)
	{
		boolean valid = true;
		String val = getUncheckedField(i);
		
		if (null == val)
		{
			valid = false;
		}
		else
		{
			switch(types[i])
			{
//			case SettingsPanel.F_STRING:
				// should always be fine
//				break;
			case SettingsPanel.F_INTEGER:
				try
				{
					new Integer(val);
				}
				catch (NumberFormatException e)
				{
					valid = false;
				}
				break;
			case SettingsPanel.F_DOUBLE:
				try
				{
					new Double(val);
				}
				catch (NumberFormatException e)
				{
					valid = false;
				}
				break;
			}
		}
		
		return valid;
	}
	
	@SuppressWarnings("unchecked")
	private void saveSettings(File f) throws IOException
	{
		File parent = f.getParentFile();
		if (!parent.exists())
		{
			parent.mkdirs();
		}
		BufferedWriter bfw = new BufferedWriter(new FileWriter(f));
		for (int i=0;i<types.length;i++)
		{
			String val = "";
			
			switch(comps[i])
			{
			case SettingsPanel.C_COMBOBOX:
				val = (((JComboBox<String>) jFields[i]).getSelectedItem() + "").trim();
				if (val.equals(""))
				{
					val = "null";
				}
				break;
			case SettingsPanel.C_TEXTFIELD:
				val = ((JTextField) jFields[i]).getText().trim();
				break;
			}
			
			bfw.write(val);
			bfw.newLine();
		}
		bfw.close();
	}
	
	public JComponent getField(int i)
	{
		return jFields[i];
	}
	
	private void error(String msg)
	{
		JOptionPane.showMessageDialog(self,msg,"ERROR!",JOptionPane.ERROR_MESSAGE);
	}
	
	private boolean confirm(String msg)
	{
		return (JOptionPane.showConfirmDialog(self, msg, "Please confirm!", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) == 0;
	}
}