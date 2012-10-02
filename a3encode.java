import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * This class is used for encoding xml file
 * In this class, we call the data part in s alpha as "data", the remaining part in s alpha as "node"
 * @author CJF
 *
 */
class a3encode extends DefaultHandler {
	/**
	 * ArrayList used for storing s_last
	 */
	ArrayList<Integer> s_last;
	/**
	 * ArrayList used for storing index of s alpha
	 */
	ArrayList<String> s_a;
	/**
	 * ArrayList used for storing index of s pi
	 */
	ArrayList<Integer> s_pi;

	/**
	 * HashMap used for storing matching relationship between index in s alpha
	 * and full information
	 */
	HashMap<String, Integer> s_ahm;
	/**
	 * no repeat node of s pi, contain no data
	 */
	HashMap<String, Integer> unique_spi_nodehm;
	/**
	 * no repeat data of s pi
	 */
	HashMap<String, Integer> unique_spi_datahm;
	/**
	 * HashMap used for storing c_array
	 */
	HashMap<String, Integer> c_array;

	/**
	 * stack used for storing new tag name when read in
	 */
	private Stack<String> tags = new Stack<String>();
	/**
	 * count how many times of continuous stack pop operation
	 */
	private int pop_count = 0;
	/**
	 * parent string
	 */
	private String parent = "";
	/**
	 * store the last tag name
	 */
	private String lastTag = "";

	private int indexOfNode = 0;
	private int indexOfSahm = 0;

	int indexOfLastBuf = 0;
	/**
	 * output buffer. Modify it to smaller when memory is not enough
	 */
	static final int BUFFER = 2000000;

	FileOutputStream output;
	ZipOutputStream zipout;
	FileOutputStream outputDataNum;
	SAXParser sp;
	/**
	 * if the data is existed
	 */
	boolean existData;

	/**
	 * input filename
	 */
	String inputFile;
	/**
	 * output filename
	 */
	String outputFile;

	String path;

	/**
	 * when a new parameter in, update C array
	 * 
	 * @param path
	 *            element of c array
	 * 
	 */
	public void setC_Array(String path) {
		if (c_array.containsKey(path)) {
			int i = c_array.get(path);
			i++;
			c_array.put(path, i);
		} else {
			c_array.put(path, 1);
		}
	}

	/**
	 * sort the s_last, s_a, and out put them
	 * 
	 * @throws IOException
	 */
	public void sort() throws IOException {
		//System.out.println("Begin to generate XBW node");

		// output c_array
		Object[] keysOfC = c_array.keySet().toArray();
		Arrays.sort(keysOfC);
		int current = 1;
		for (int i = 1; i < keysOfC.length; i++) {

			String s = (String) keysOfC[i];
			int sa = s_ahm.get(s);
			this.output("<" + sa + ">" + (current));
			int count = c_array.get(s);
			current += count;

		}
		this.output("\n");

		// output s_ahm
		s_ahm.remove("=");
		Iterator<String> it = s_ahm.keySet().iterator();
		while (it.hasNext()) {
			String entry = it.next();
			//System.out.println(entry + " & " + s_ahm.get(entry));
			this.output((entry + ">" + s_ahm.get(entry)));
		}
		this.output("\n");

		c_array.clear();
		s_ahm.clear();
		HashMap<String, Integer> unique_spi = new HashMap<String, Integer>();
		unique_spi.putAll(unique_spi_nodehm);
		unique_spi.putAll(unique_spi_datahm);
		int lengthOfNode = unique_spi_nodehm.size();
		unique_spi_nodehm.clear();
		unique_spi_datahm.clear();
		Object[] keys = unique_spi.keySet().toArray();
		Arrays.sort(keys);
		// Arrays.sort(keys2);
		//System.out.println("Size of spi_hm = " + keys.length);
		// System.out.println("Size of datahm = " + keys2.length);
		int[] valueOfsphm = new int[keys.length];
		for (int i = 0; i < keys.length; i++) {
			valueOfsphm[i] = unique_spi.get(keys[i]);
		}
		unique_spi.clear();

		//System.out.println("length of spi " + s_pi.size());

		sortS_last(valueOfsphm, lengthOfNode);
		this.output("\n");
		sortS_a(valueOfsphm, lengthOfNode);

	}

	/**
	 * sort s last and output them to file
	 * @param index sorted index of s pi
	 * @param lenghOfnode number of node
	 * @throws IOException
	 */
	public void sortS_last(int[] index, int lenghOfnode) throws IOException {
		for (int in = 0; in < lenghOfnode; in++) {
			//System.out.println(in + "th ");
			StringBuffer tempBuf = new StringBuffer();
			// String p = (String) keys1[in];
			int indexOfpi = index[in];
			for (int i = 0; i < s_pi.size(); i++) {
				int valueOfspi = s_pi.get(i);
				if (valueOfspi == indexOfpi) {
					int valueOfslast = s_last.get(i);

					tempBuf.append(valueOfslast);
				}

			}
			this.output(tempBuf.toString());
		}
		s_last.clear();
		// this.output("\n");
	}

	/**
	 * sort s alpha and output them to file
	 * @param index sorted index of s pi
	 * @param lenghOfnode number of node
	 * @throws IOException
	 */
	public void sortS_a(int[] index, int lenghOfnode) throws IOException {
		for (int in = 0; in < index.length; in++) {
			//System.out.println(in + "th");
			StringBuffer tempBuf = new StringBuffer();

			int indexOfpi = index[in];
			if (in == lenghOfnode) {
				this.output("\n");
			}
			for (int i = 0; i < s_pi.size(); i++) {

				int valueOfspi = s_pi.get(i);// match to nodehm and datahm
				if (valueOfspi == indexOfpi) {
					String valueOfsa = s_a.get(i);

					if (in >= lenghOfnode) {
						if (tempBuf.length() > BUFFER) {
							this.output(tempBuf.toString());
							tempBuf = new StringBuffer();
							tempBuf.append(valueOfsa);
						} else {
							tempBuf.append(valueOfsa);
						}
						this.outputDataNum.write((valueOfsa.length() + ",")
								.getBytes());
					} else {
						if (tempBuf.length() > BUFFER) {
							this.output(tempBuf.toString());
							tempBuf = new StringBuffer();
							tempBuf.append(valueOfsa + ",");
						} else {
							tempBuf.append(valueOfsa + ",");
						}
					}

				}

			}

			//System.out.println("length of buffer = " + tempBuf.length());
			this.output(tempBuf.toString());

		}

		// this.output("\n");

		s_a.clear();
	}

	public a3encode() {
		super();
	}

	/**
	 * initialize all variables
	 * @param inputfile
	 * @param outputfile
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void initial(String inputfile, String outputfile,String path)
			throws ParserConfigurationException, SAXException {
		s_last = new ArrayList<Integer>();
		s_a = new ArrayList<String>();
		s_pi = new ArrayList<Integer>();
		s_a.ensureCapacity(1000);
		unique_spi_nodehm = new HashMap<String, Integer>();
		unique_spi_datahm = new HashMap<String, Integer>();
		s_ahm = new HashMap<String, Integer>();
		c_array = new HashMap<String, Integer>();
		c_array.put("", 1);
		this.inputFile = inputfile;
		this.outputFile = outputfile;
		this.path = path;
		try {
			output = new FileOutputStream(outputfile);
			// zipout = new ZipOutputStream(new BufferedOutputStream(output));
			outputDataNum = new FileOutputStream(this.path+"/"+ outputFile+ "data");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SAXParserFactory sf = SAXParserFactory.newInstance();
		sp = sf.newSAXParser();

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	public void startDocument() throws SAXException {


		s_ahm.put("=", 0);
		indexOfSahm++;

	}

	public void endDocument() throws SAXException {

	}

	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) {

		existData = false;
		parent = "";
		for (int i = tags.size() - 1; i >= 0; i--) {
			parent += "<" + tags.get(i);
		}
		if (tags.size() > 0) {
			this.setC_Array("<" + tags.get(tags.size() - 1));
		}
		Integer tempIndex = s_ahm.get("<" + qName);
		if (tempIndex == null) {
			s_ahm.put("<" + qName, indexOfSahm);
			tempIndex = indexOfSahm;
			indexOfSahm++;
		}
		// System.out.println(tempIndex);
		// int t = tempIndex;
		s_a.add(tempIndex + "");
		tags.push(qName);
		// s_pi.add(parent);
		s_last.add(0);

		Integer index = unique_spi_nodehm.get(parent);
		if (index == null) {
			unique_spi_nodehm.put(parent, indexOfNode);
			s_pi.add(indexOfNode);
			indexOfNode++;
		} else {
			// int index = nodehm.get(parent);
			s_pi.add(index);
		}

		pop_count = 0;

	}

	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {

		String tagOfLast = "";
		pop_count++;
		// System.out.println(pop_count);

		if (existData) {
			if (pop_count > 1) {
				tagOfLast = lastTag;
				// System.out.println(tagOfLast);
				int j = 0;
				int t = s_ahm.get("<" + tagOfLast);
				j = s_a.lastIndexOf(t + "");
				s_last.set(j, 1);

			}
			lastTag = tags.pop();
		} else {
			parent = "";
			for (int i = tags.size() - 1; i >= 0; i--) {
				parent += "<" + tags.get(i);
			}

			// add "="
			if (tags.size() > 0) {
				this.setC_Array("<" + tags.get(tags.size() - 1));
			}
			s_a.add("0");

			Integer index = unique_spi_nodehm.get(parent);
			if (index == null) {
				unique_spi_nodehm.put(parent, indexOfNode);
				s_pi.add(indexOfNode);
				indexOfNode++;
			} else {
				s_pi.add(index);
			}

			s_last.add(1);

			// add data
			s_a.add("#");

			this.setC_Array("=");

			index = unique_spi_datahm.get("=" + parent);
			if (index == null) {
				unique_spi_datahm.put("=" + parent, indexOfNode);
				s_pi.add(indexOfNode);
				indexOfNode++;
			} else {
				s_pi.add(index);
			}
			s_last.add(1);

			if (pop_count > 1) {
				tagOfLast = lastTag;
				// System.out.println(tagOfLast);
				int j = 0;
				int t = s_ahm.get("<" + tagOfLast);
				j = s_a.lastIndexOf(t + "");
				s_last.set(j, 1);

			}
			lastTag = tags.pop();
			existData = true;

		}
	}

	public void characters(char[] chars, int start, int length)
			throws SAXException {
		
		existData = true;
		String b = new String(chars, start, length);
		if (length == 0) {
			//System.out.println(" empty found");
			b = "";
		}
		if (!b.equals("")) {
			parent = "";
			for (int i = tags.size() - 1; i >= 0; i--) {
				parent += "<" + tags.get(i);
			}

			// add "="
			if (tags.size() > 0) {
				this.setC_Array("<" + tags.get(tags.size() - 1));
			}
			s_a.add("0");

			Integer index = unique_spi_nodehm.get(parent);
			if (index == null) {
				unique_spi_nodehm.put(parent, indexOfNode);
				s_pi.add(indexOfNode);
				indexOfNode++;
			} else {
				s_pi.add(index);
			}

			s_last.add(1);

			// add data
			s_a.add("#" + b);

			this.setC_Array("=");

			index = unique_spi_datahm.get("=" + parent);
			if (index == null) {
				unique_spi_datahm.put("=" + parent, indexOfNode);
				s_pi.add(indexOfNode);
				indexOfNode++;
			} else {
				s_pi.add(index);
			}
			s_last.add(1);
		}
	}

	/**
	 * output given string to file
	 * @param slast the string want to be output
	 * @throws IOException
	 */
	public void output(String slast) throws IOException {
		// FileOutputStream output = new FileOutputStream("yahoo");
		output.write((slast).getBytes());

	}

	/**
	 * compress file use java zip
	 */
	public void zipFile() {
		File file = new File(this.outputFile);
		int length = (int) file.length();
		byte[] b = new byte[length];
		try {
			InputStream in = new FileInputStream(file);
			in.read(b);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(
					new File(this.path+"/"+this.outputFile)));
			zout.setLevel(9);
			ZipEntry zipEntry = new ZipEntry(file.getName());
			zout.putNextEntry(zipEntry);
			zout.write(b);
			zout.finish();
			zout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		long startTime = System.currentTimeMillis();
		try {
			String input = args[0];
			String output = "outputfile";
			
			String path = args[1];
			a3encode testsax = new a3encode();
			testsax.initial(input, output,path);
			testsax.sp.parse(new File(input), testsax);
			testsax.s_last.set(0, 1);

			//System.out.println();

			testsax.sort();
			testsax.zipFile();
			// testsax.compressLast();
			// testsax.output();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		System.out.println((double) (endTime - startTime) / 1000 + "s");
	}
}