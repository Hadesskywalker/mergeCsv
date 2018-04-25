package at.hadesskywalker.mergeCsv;

public class Main {
	
	private static int POS_SEPARATOR = 0;
	private static int POS_FILE1 = 1;
	private static int POS_FILE2 = 2;

	public static void main(String[] args) {
		String arg_separator = args[POS_SEPARATOR];
		String arg_file1 = args[POS_FILE1];
		String arg_file2 = args[POS_FILE2];
		
		CsvFile file1 =	new CsvFile(arg_separator, arg_file1);
		CsvFile file2 = new CsvFile(arg_separator, arg_file2);
		
		CsvFile merged = file1.merge(file2, "DOI");
		System.out.println("Merged to: "+merged.getColumnContent().size());
		
		merged.writeFileAsLines("merged.csv");
	}
	
	private void displayHelp() {
		System.out.println("HELP... todo");
	}

}
