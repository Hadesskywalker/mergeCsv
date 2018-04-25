package at.hadesskywalker.mergeCsv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CsvFile {

	private String separator;
	private String pathToFile;

	//3d map -> line / column / value
	private Map<Integer, Map<String, String>> columnContent;
	private Map<Integer, Boolean> markedAsSingle; //default is all true
	private Map<Integer, String> columnHeaders; 

	public CsvFile() {
		separator = ";";
		pathToFile= "";
		columnContent = new HashMap<>();
		columnHeaders = new HashMap<>();
	}

	public CsvFile(String separator, String pathToFile) {
		this.separator = separator;
		this.pathToFile = pathToFile;

		if(pathToFile != null && !pathToFile.isEmpty()
				&& separator != null && !separator.isEmpty()) {
			columnContent = new HashMap<>();
			markedAsSingle = new HashMap<>();
			List<String> fileContent = loadFileAsLines(pathToFile);

			for(int i=0;i<fileContent.size();i++) {
				if(i == 0) {
					//map the column line (99% always the first line)
					columnHeaders = mapColumnHeaders(separator, fileContent.get(0));
				}else{
					Map<String, String> mappedLine = mapLine(separator, columnHeaders, fileContent.get(i));
					if(mappedLine != null) {
						columnContent.put(i, mappedLine);
						markedAsSingle.put(i, true);
					}
				}
			}
		}
	}

	public boolean isEmpty() {
		return columnContent.isEmpty();
	}

	public void setColumnHeaders(Map<Integer, String> columnHeaders) {
		this.columnHeaders = columnHeaders;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public void setPathToFile(String pathToFile) {
		this.pathToFile = pathToFile;
	}

	public void addColumnContent(Map<String, String> addLine) {
		if(addLine != null && !addLine.isEmpty()) {
			columnContent.put(columnContent.keySet().size()+1, addLine);
		}
	}

	public void setColumnContent(Map<Integer, Map<String, String>> columnContent) {
		this.columnContent = columnContent;
	}

	public void setMarkedAsSingle(int line, boolean isSingle) {
		markedAsSingle.put(line, isSingle);
	}

	public Map<Integer, Boolean> getMarkedAsSingle() {
		return markedAsSingle;
	}

	public Map<Integer, Map<String, String>> getColumnContent() {
		return columnContent;
	}

	public Map<Integer, String> getColumnHeaders() {
		return columnHeaders;
	}

	//prerequisite the headers are all named the same in all files
	public CsvFile merge(CsvFile toMergeWith, String columToMergeWith) {
		System.out.println("Merging "+getColumnContent().size()+" with "+toMergeWith.getColumnContent().size());

		CsvFile result = new CsvFile();
		if(toMergeWith != null && !toMergeWith.isEmpty()
				&& columToMergeWith != null && !columToMergeWith.isEmpty()) {

			Iterator<Integer> iteratorToMergeWith = toMergeWith.getColumnContent().keySet().iterator();
			while(iteratorToMergeWith.hasNext()) {
				Integer keyToMergeWith = iteratorToMergeWith.next();
				if(toMergeWith.getMarkedAsSingle().get(keyToMergeWith)) {
					String valueToMergeWith= toMergeWith.getColumnContent().get(keyToMergeWith).get(columToMergeWith);

					Iterator<Integer> iteratorInternal = getColumnContent().keySet().iterator();
					while(iteratorInternal.hasNext()) {
						Integer keyInternal = iteratorInternal.next();
						if(getMarkedAsSingle().get(keyInternal)) {
							String valueInternal = getColumnContent().get(keyInternal).get(columToMergeWith);


							if((valueToMergeWith == null && valueInternal == null)
									|| (valueToMergeWith.isEmpty() && valueInternal.isEmpty())
									|| valueToMergeWith.equals(valueInternal)) {
								//mark in both files the value as NOT single
								setMarkedAsSingle(keyInternal, false);
								toMergeWith.setMarkedAsSingle(keyToMergeWith, false);
							}
						}
					}
				}
			}

			//when done, copy this content to the new file and app all content from toMerge where single is still on true
			result.setColumnHeaders(getColumnHeaders());
			result.setSeparator(separator);
			result.setColumnContent(getColumnContent()); //not a real clone make it immutalbe??

			iteratorToMergeWith = toMergeWith.getMarkedAsSingle().keySet().iterator();
			while(iteratorToMergeWith.hasNext()) {
				Integer keyToMergeWith = iteratorToMergeWith.next();
				if(toMergeWith.getMarkedAsSingle().get(keyToMergeWith)) {
					result.addColumnContent(toMergeWith.getColumnContent().get(keyToMergeWith));
				}
			}
		}
		return result;
	}

	public Map<String, String> mapLine(String separator, Map<Integer, String> columnHeaders, String line){
		Map<String, String> result = new HashMap<>();
		if(separator != null && !separator.isEmpty()
				&& columnHeaders != null && !columnHeaders.isEmpty()
				&& line != null && !line.isEmpty()) {
			String[] values = line.split(separator+"(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
			for(int i = 0; i < values.length; i++) {
				result.put(columnHeaders.get(i), values[i]);
			}
		}
		//filter empty
		for(String content: result.values()) {
			if(content != null && !content.isEmpty())
				return result;
		}
		System.out.println("Found empty entry");
		//return null if all content is empty or null
		return null;
	}

	public Map<Integer, String> mapColumnHeaders(String separator, String line){
		Map<Integer, String> result = new HashMap<>();
		if(separator != null && !separator.isEmpty()
				&& line != null && !line.isEmpty()) {
			String[] values = line.split(separator+"(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
			for(int i = 0; i < values.length; i++) {
				result.put(i, values[i]);
			}
		}
		return result;
	}

	public void writeFileAsLines(String pathToFile) {
		File file = new File(pathToFile);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

			StringBuilder fileContent = new StringBuilder();
			
			//writer header
			String line = null;
			Iterator<Integer> iteratorHeader = getColumnHeaders().keySet().iterator();
			while(iteratorHeader.hasNext()) {
				Integer keyHeader = iteratorHeader.next();
				if(line != null) line+=separator;
				if(line != null) line+=getColumnHeaders().get(keyHeader);
				if(line == null) line=getColumnHeaders().get(keyHeader);
			}
			line+="\n";
			fileContent.append(line);
			
			Iterator<Integer> iteratorInternal = getColumnContent().keySet().iterator();
			while(iteratorInternal.hasNext()) {
				Integer keyInternal = iteratorInternal.next();
				iteratorHeader = getColumnHeaders().keySet().iterator();
				line = null;
				while(iteratorHeader.hasNext()) {
					Integer keyHeader = iteratorHeader.next();
					if(line != null) line+=separator;
					if(line != null) line+=getColumnContent().get(keyInternal).get(getColumnHeaders().get(keyHeader));
					if(line == null) line=getColumnContent().get(keyInternal).get(getColumnHeaders().get(keyHeader));
				}
				line+="\n";
				fileContent.append(line);
			}

			writer.write(fileContent.toString());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public List<String> loadFileAsLines(String pathToFile) {
		List<String> result = new ArrayList<String>();
		if (isExisting(pathToFile)) {
			BufferedReader reader = null;
			try {
				File file = new File(pathToFile);
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

				String line;
				while ((line = reader.readLine()) != null) {
					result.add(line);
				}

				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	public boolean isExisting(String path) {
		if (path == null || path.isEmpty()) {
			return false;
		}
		File file = new File(path);
		if (file == null || !file.exists()) {
			return false;
		}
		return true;
	}

}
