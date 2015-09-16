import com.itextpdf.text.FontFactory;

public class PDFPanel extends SettingsPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6023951386143015185L;
	
	static String[] labels = new String[]{
		"Descriptive Text",
		"Font",
		"Font size (pt)",
		"Position",
		"Page width (mm)",
		"Page height (mm)",
		"Page left margin (mm)",
		"Page top margin (mm)",
		"Label width (mm)",
		"Label height (mm)",
		"Label left margin (mm)",
		"Label right margin (mm)",
		"Label top margin (mm)",
		"Label bottom margin (mm)",
		"Space between rows (mm)",
		"Space between columns (mm)",
		"Number of rows",
		"Number of columns",
	};
	static int[] types = new int[]{
		F_STRING,
		F_STRING,
		F_DOUBLE, // actually float, but should be fine
		F_STRING,
		F_DOUBLE,
		F_DOUBLE,
		F_DOUBLE,
		F_DOUBLE,
		F_DOUBLE,
		F_DOUBLE,
		F_DOUBLE,
		F_DOUBLE,
		F_DOUBLE,
		F_DOUBLE,
		F_DOUBLE,
		F_DOUBLE,
		F_INTEGER,
		F_INTEGER
	};
	static int[] components = new int[]{
		C_TEXTFIELD,
		C_COMBOBOX,
		C_TEXTFIELD,
		C_COMBOBOX,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD,
		C_TEXTFIELD
	};
	static String folder = "settings";
	static String fileExtFilter = "pdfOutput";
	static String[] defaults = new String[]{
		"",
		"",
		"12",
		"",
		"210",
		"297",
		"0",
		"0",
		"70",
		"37",
		"4",
		"4",
		"2",
		"3",
		"0",
		"0",
		"8",
		"3"
	};
		
	public PDFPanel(int cols)
	{
		/*
		 * Descriptive text:
		 * 0 "Text",						0
		 * 1 "Font",						1
		 * 2 "Font size",					2
		 * 3 "Position"						3
		 * 
		 * Layout:
		 * 4 "Page width",					0
		 * 5 "Page height",					1
		 * 6 "Page left margin",			2
		 * 7 "Page top margin",				3
		 * 8 "Label width"					4
		 * 9 "Label height"					5
		 * 10 "Label left margin",			6
		 * 11 "Label right margin",			7
		 * 12 "Label top margin",			8
		 * 13 "Label bottom margin",		9
		 * 14 "Space between rows",			10
		 * 15 "Space between columns",		11
		 * 16 "Number of rows",				12
		 * 17 "Number of columns"			13
		 */
		
		super(labels, cols, types, components, folder, fileExtFilter, defaults);
		
		setComboOptions(FontFactory.getRegisteredFonts().toArray(new String[0]), 1); // set font types
		setComboOptions(new String[]{"Above barcode","None","Below barcode"}, 3); // set positions
		
	}
}