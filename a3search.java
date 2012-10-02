import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class a3search {
	/**
	 * HashMap used for storing matching relationship between index in s alpha
	 * and full information
	 */
	private HashMap<Integer, String> s_ahm;
	/**
	 * record the first occurrence position of element in s pi
	 */
	private HashMap<Integer, Integer> c_array;
	/**
	 * s alpha
	 */
	private int[][] s_a;
	/**
	 * record how many times of elements in s_a occur
	 */
	private HashMap<Integer, Integer>[] BL;
	/**
	 * s_last
	 */
	private char[][] s_last;
	/**
	 * record how many times of elements in s_last occur
	 */
	private int[] BLforLast;
	/**
	 * record tag name
	 */
	private Stack<Integer> tag;

	private RandomAccessFile randomReader;
	/**
	 * size of bl
	 */
	private long sizeOfBL;
	/**
	 * length of bl
	 */
	private long lengthOfBL;
	private long l;

	private String inputFile;
	/**
	 * data offset
	 */
	private String[] data;
	/**
	 * the base address of data
	 */
	private long dataBase;
	Stack<String> patterns = new Stack<String>();
	int First;
	int Last;
	String path;
	/**
	 * store all position of data element
	 */
	ArrayList<Integer> dataPos = new ArrayList<Integer>();

	
	/**
	 * search the pattern
	 * @param s XPATH
	 * @throws IOException
	 */
	public void search2(String s) throws IOException {
		String[] xpath = s.substring(1).split("\\/");
		for (int i = xpath.length - 1; i >= 0; i--) {
			patterns.push(xpath[i]);
			//System.out.println("=" + xpath[i]);
		}
		ArrayList<Integer> num = new ArrayList<Integer>();
		num.add(0);
		search_reversion(num);
	}

	/**
	 * search the pattern like this a[b~"c"]
	 * @param pattern the pattern
	 * @param node the input children of parent node
	 * @return nodes which satisfy the requirement
	 * @throws IOException
	 */
	public ArrayList<Integer> searchInside(String pattern, ArrayList<Integer> node)
			throws IOException {
		ArrayList<Integer> next = new ArrayList<Integer>();
		// System.out.println("element = "+element);
		String[] insideValue = pattern.split("\\[|\\]|\\~");
		String tag = insideValue[0];
		// System.out.println("tag = "+tag);
		// int iTag = this.getKeyOfSA(tag);
		String name = insideValue[1];
		// System.out.println("name = "+name);
		String value = "";
		if (insideValue[2].contains("\"")) {
			value = insideValue[2].substring(1, insideValue[2].length() - 1);
		} else {
			value = insideValue[2];
		}
		// System.out.println(tag + " " + name + " " + value);
		for (int i = 0; i < node.size(); i++) {
			int vInSA = this.getS_aElement(node.get(i));
			String se = s_ahm.get(vInSA).substring(1);
			// System.out.println(se+" "+node.get(i));
			if (tag.equals("*") || se.equals(tag)) {
				// int v = this.getS_aElement(node.get(i));
				int[] range = getRange(vInSA, node.get(i) + 1);
				int fr = range[0];
				int lr = range[1];
				//System.out.println(fr + " " + lr);
				for (int j = fr; j <= lr; j++) {

					int s = this.getS_aElement(j);
					String e = s_ahm.get(s).substring(1);
					// System.out.println("e = "+e);
					if (name.equals("*") || e.equals(name)) {
						// int rank = rank(0, j + 1);
						String data = this.generateChild(s, j);

						if (data.contains(value)) {
							//System.out.println(data);
							int m = node.get(i);
							if (!next.contains(m))
								next.add(m);
						}
					}
				}
			}
		}
		return next;
	}

	/**
	 * search nodes which satisfy requirements
	 * @param node nodes of parent
	 * @throws IOException
	 */
	public void search_reversion(ArrayList<Integer> node) throws IOException {
		ArrayList<Integer> next = new ArrayList<Integer>();
		String element = patterns.pop();

		if (element.contains("[")) {
			next = searchInside(element, node);
		} else {
			for (int i = 0; i < node.size(); i++) {
				int vInSA = this.getS_aElement(node.get(i));
				String e = s_ahm.get(vInSA).substring(1);
				// System.out.println(e);
				if (element.equals("*") || e.equals(element)) {
					next.add(node.get(i));
				}
			}
		}
		node = new ArrayList<Integer>();
		if (!patterns.isEmpty()) {
			for (int i = 0; i < next.size(); i++) {
				int v = this.getS_aElement(next.get(i));
				int[] range = getRange(v, next.get(i) + 1);
				int fr = range[0];
				int lr = range[1];
				for (int j = fr; j <= lr; j++) {
					node.add(j);
				}
			}
			search_reversion(node);

		} else {

			for (int i = 0; i < next.size(); i++) {
				int v = this.getS_aElement(next.get(i));
				int[] range = getRange(v, next.get(i) + 1);
				int fr = range[0];
				int lr = range[1];
				for (int j = fr; j <= lr; j++) {
					if (!node.contains(j))
						node.add(j);
				}
			}

			System.out.println(node.size());
		}
	}

	/**
	 * get the index of value in s alpha
	 * @param p position
	 * @return index of p
	 */
	public int getKeyOfSA(String p) {
		int iValue = 0;

		if (p.equals("=")) {
			return 0;
		}

		Iterator<Integer> ite = s_ahm.keySet().iterator();
		while (ite.hasNext()) {
			Integer key = ite.next();
			String value = s_ahm.get(key);
			if (value.equals("<" + p)) {
				iValue = key;
				break;
			}
		}

		return iValue;
	}

	/**
	 * split XPATH
	 * @param path XPATH
	 * @return pattern array
	 */
	public String[] getPartterns(String path) {
		String[] p = path.split("/");
		for (int i = 0; i < p.length; i++) {
			//System.out.println("=" + p[i]);
		}

		return p;
	}

	/**
	 * get index of value in s alpha via position 
	 * @param pos position
	 * @return index
	 */
	public int getS_aElement(int pos) {
		int base = (int) (pos / sizeOfBL);
		int offset = (int) (pos % sizeOfBL);
		return s_a[base][offset];
	}

	/**
	 * get index of value in s last via position 
	 * @param pos position
	 * @return index
	 */
	public char getS_lastElement(int pos) {
		int base = (int) (pos / sizeOfBL);
		int offset = (int) (pos % sizeOfBL);
		return s_last[base][offset];
	}

	/**
	 * get sub tree and output them
	 * 
	 * @param parent
	 *            the index of parent node
	 * @param pos position of parent
	 * @throws IOException
	 * return child
	 */
	public String generateChild(int parent, int index) throws IOException {
		String result = "";
		if (parent != 0) {
			int[] childrenRange = this.getRange(parent, index + 1);
			tag.push(parent);
			int first = childrenRange[0];
			int last = childrenRange[1];
			for (int i = first; i <= last; i++) {
				int indexOfBL = (int) (i / sizeOfBL);
				int offset = (int) (i % sizeOfBL);
				int p = s_a[indexOfBL][offset];
				if (p == 0) {
					result = generateChild(p, i);
				}

			}
			tag.pop();
			return result;
		} else {

			int rank = rank(0, index + 1);
			String d = this.getData(rank);
			return d;

		}

	}

	/**
	 * initialize all variables
	 * @param inputfile
	 * @param outputfile
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void init(String inputfile, String outputfile, String path)
			throws FileNotFoundException {
		s_ahm = new HashMap<Integer, String>();
		c_array = new HashMap<Integer, Integer>();
		// s_a = new ArrayList<Integer>();
		tag = new Stack<Integer>();
		inputFile = inputfile;
		this.path = path;
		new StringBuffer();
	}
	
	/**
	 * load every data offset
	 * @param dataName
	 * @throws IOException
	 */
	private void loadData(String dataName) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(dataName)));

		randomReader = new RandomAccessFile(path + "/" + inputFile + "1", "r");
		String temp;
		randomReader.readLine();
		randomReader.readLine();
		randomReader.readLine();
		randomReader.readLine();
		dataBase = randomReader.getFilePointer();
		// System.out.println("data offset = "+dataBase);
		temp = reader.readLine();
		data = temp.split(",");
		dataPos.add(0);
		int length = 0;
		for (int i = 0; i < data.length; i++) {
			length = Integer.parseInt(data[i]);
			int num = 0;

			num = dataPos.get(i);

			dataPos.add(num + length);

			// System.out.print(dataPos.get(i) + " ");
		}
	}
	/**
	 * return the data by given index
	 * 
	 * @param index
	 *            the index of data in file
	 * @return result data
	 * @throws IOException
	 */
	private String getData(int index) throws IOException {
		index = index - 1;
		long dataOffset = dataPos.get(index);
		randomReader.seek(dataOffset + dataBase);
		byte[] buf;

		buf = new byte[(int) (dataPos.get(index + 1) - dataOffset)];

		randomReader.read(buf);
		String result = new String(buf).substring(1);
		return result;
	}

	/**
	 * get the number of occurrence of value in s_a
	 * 
	 * @param valueInsa the index of value in sa
	 * @param pos position of value in sa 
	 * @return how many times it occurs in s_a
	 */
	public int rank(int valueInsa, int index) {
		int rank = 0;
		int indexOfBL = (int) ((index - 1) / sizeOfBL);
		int offset = (int) ((index - 1) % sizeOfBL);

		for (int i = 0; i <= offset; i++) {
			if (s_a[indexOfBL][i] == valueInsa) {
				rank++;
			}
		}
		if (indexOfBL != 0) {

			HashMap<Integer, Integer> tmp = BL[indexOfBL - 1];
			if (tmp.containsKey(valueInsa)) {
				rank = rank + BL[indexOfBL - 1].get(valueInsa);
			}

		}
		return rank;
	}
	/**
	 * how many times the '1' occurs in s last
	 * @param i position of this 1
	 * @return occurrence number
	 */
	public int rankInLast(int i) {
		int index = (int) ((i - 1) / sizeOfBL);
		int offset = (int) ((i - 1) % sizeOfBL);
		int c = 0;
		for (int j = 0; j <= offset; j++) {
			if (s_last[index][j] == '1') {
				c++;
			}
		}
		int result;

		if (index == 0) {
			result = c;
		} else {
			result = BLforLast[index - 1] + c;
		}

		return result;
	}

	/**
	 * ge the position of i'th 1 in last
	 * @param i i'th
	 * @return position
	 */
	public int selectInLast(int i) {
		int index = 0;
		int remain = 0;
		// System.out.println("select = "+i);
		if (BLforLast[(int) lengthOfBL - 1] >= i) {
			while (BLforLast[index] < i) {
				index++;
			}
			if (index > 0) {
				remain = i - BLforLast[index - 1];
			} else {
				remain = i;
			}
			int pos = 0;
			int j = 0;
			while (j < remain) {
				if (s_last[index][pos] == '1') {
					j++;
				}
				pos++;
			}
			int result = (int) (index * sizeOfBL + pos);
			// System.out.println("selectInLast = "+result);
			return result;
		} else {
			int result = i - BLforLast[(int) lengthOfBL - 1];
			// System.out.println("selectInLast = "+result);
			return result;
		}

	}
	/**
	 * get the range of children of this node
	 * @param valueInsa  the index of value in sa
	 * @param pos position of value in sa 
	 * @return position range
	 */
	public int[] getRange(int valueInsa, int index) {
		int[] range = new int[2];
		int rank = this.rank(valueInsa, index);
		int y = c_array.get(valueInsa);
		// System.out.println("y = " + y);
		int z = this.rankInLast(y - 1);
		// System.out.println("z = " + z);
		int first = this.selectInLast(z + rank - 1) + 1;
		int last = this.selectInLast(z + rank);

		range[0] = first - 1;
		range[1] = last - 1;
		return range;
	}

	/**
	 * add one hashmap to another
	 * @param h1 the first hashmap
	 * @param h2 the first hashmap
	 * @return result hashmap
	 */
	public HashMap<Integer, Integer> hashMapPlus(HashMap<Integer, Integer> h1,
			HashMap<Integer, Integer> h2) {

		if (h2 != null) {
			Object[] keys = h2.keySet().toArray();
			for (int j = 0; j < keys.length; j++) {
				if (h1.containsKey(keys[j])) {
					int value = h2.get(keys[j]);
					int origin = h1.get(keys[j]);
					h1.put((Integer) keys[j], value + origin);
				} else {
					int value = h2.get(keys[j]);
					h1.put((Integer) keys[j], value);
				}
			}
			return h1;
		} else {
			return h1;
		}
	}

	/**
	 * read in s last
	 * @param fileName input file name
	 * @throws IOException
	 */
	public void readInS_Last(String fileName) throws IOException {
		FileInputStream fi = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(fi));

		s_last = new char[(int) lengthOfBL][(int) sizeOfBL];
		BLforLast = new int[(int) lengthOfBL];

		for (int i = 0; i < lengthOfBL; i++) {
			for (int j = 0; j < sizeOfBL; j++) {
				s_last[i][j] = '3';
			}
		}

		reader.readLine();
		reader.readLine();

		String temp = reader.readLine();
		String buf;
		int index = 0;
		while (temp.length() > sizeOfBL) {
			buf = temp.substring(0, (int) sizeOfBL);
			temp = temp.substring((int) sizeOfBL);
			// HashMap <Byte, Integer> tmp = new HashMap<Byte,Integer>();
			int count = 0;
			for (int i = 0; i < buf.length(); i++) {
				char c = buf.charAt(i);
				s_last[index][i] = c;
				if (c == '1') {
					count++;
				}

			}
			if (index != 0) {
				BLforLast[index] = BLforLast[index - 1] + count;
			} else {
				BLforLast[index] = count;
			}

			index++;
		}
		if (temp.length() > 0) {
			int count = 0;
			for (int i = 0; i < temp.length(); i++) {
				char c = temp.charAt(i);
				s_last[index][i] = c;
				if (c == '1') {
					count++;
				}
			}
			BLforLast[index] = BLforLast[index - 1] + count;
		}
		fi.close();
		reader.close();
	}
	/**
	 * read in s alpha
	 * @param fileName input file name
	 * @throws IOException
	 */
	public void readInS_a(String fileName) throws IOException {
		FileInputStream fi = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(fi));
		String temp;
		reader.readLine();
		reader.readLine();
		reader.readLine();
		temp = reader.readLine();
		String[] datas = temp.split(",");

		long dataLength = datas.length;
		l = (long) (Math.log(dataLength) / Math.log(2));
		sizeOfBL = (int) (l * l);
		lengthOfBL = (dataLength - 1) / sizeOfBL + 1;
		s_a = new int[(int) lengthOfBL][(int) sizeOfBL];
		BL = new HashMap[(int) lengthOfBL];

		//System.out.println("size Of BL = " + sizeOfBL);
		//System.out.println("length Of BL = " + lengthOfBL);

		for (int i = 0; i < (int) lengthOfBL; i++) {
			for (int j = 0; j < (int) sizeOfBL; j++) {
				s_a[i][j] = -1;
			}
		}

		// long sizeOfBlockInsa = sizeOfBL * 2;

		int index = 0;
		while (index < lengthOfBL) {
			HashMap<Integer, Integer> tmpHM = new HashMap<Integer, Integer>();
			int end;
			if (dataLength < (index + 1) * sizeOfBL) {
				end = (int) dataLength;
			} else {
				end = (int) ((1 + index) * sizeOfBL);
			}
			for (int i = (int) (index * sizeOfBL); i < end; i++) {
				int data = Integer.parseInt(datas[i]);
				int offset = (int) (i % sizeOfBL);

				s_a[index][offset] = data;
				if (tmpHM.containsKey(data)) {
					int num = tmpHM.get(data);
					num++;
					tmpHM.put(data, num);
				} else {
					tmpHM.put(data, 1);
				}
			}
			if (index == 0) {
				BL[0] = tmpHM;
			} else {
				BL[index] = this.hashMapPlus(tmpHM, BL[index - 1]);
			}
			index++;
		}
		int count = 0;
		for (int i = 0; i < lengthOfBL; i++) {
			for (int j = 0; j < sizeOfBL; j++) {
				if (s_a[i][j] != -1) {
					count++;
				}
			}
		}
		//System.out.println("length of s_a = " + count);
		fi.close();
		reader.close();
	}
	/**
	 * read in c array
	 * @param fileName input file name
	 * @throws IOException
	 */
	public void readInC_array(String fileName) throws IOException {
		FileInputStream fi = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(fi));
		String temp = reader.readLine();
		int index = -1;

		while ((index = temp.indexOf(">")) != -1) {
			String e = temp.substring(1, index);
			temp = temp.substring(index + 1);
			int start = temp.indexOf("<");
			String sNum;
			if (start == -1) {
				sNum = temp.substring(0);
			} else {
				sNum = temp.substring(0, start);
			}

			c_array.put(Integer.parseInt(e), Integer.parseInt(sNum) + 1);
			if (start != -1) {
				temp = temp.substring(start);
			}
		}
		fi.close();
		reader.close();
	}
	/**
	 * read in s alpha hashmap
	 * @param fileName input file name
	 * @throws IOException
	 */
	public void readInSaHM(String fileName) throws IOException {
		FileInputStream fi = new FileInputStream(fileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(fi));

		String temp;
		reader.readLine();
		temp = reader.readLine();
		int index = -1;
		s_ahm.put(0, "=");
		while ((index = temp.indexOf(">")) != -1) {
			String e = temp.substring(0, index);
			temp = temp.substring(index + 1);
			int start = temp.indexOf("<");
			String sNum;
			if (start == -1) {
				sNum = temp.substring(0);
			} else {
				sNum = temp.substring(0, start);
			}

			s_ahm.put(Integer.parseInt(sNum), e);
			if (start != -1) {
				temp = temp.substring(start);
			}
			// System.out.println(sNum + " " + e);
		}
		fi.close();
		reader.close();
	}
	/**
	 * unzip the compressed file
	 * @throws IOException
	 */
	public void unzip() throws IOException {
		//System.out.println(this.path + "/" + inputFile);
		ZipFile zfile = new ZipFile(this.path + "/" + inputFile);
		
		Enumeration<? extends ZipEntry> zList = zfile.entries();
		ZipEntry ze = null;
		byte[] buf = new byte[1024];
		while (zList.hasMoreElements()) {
			ze = (ZipEntry) zList.nextElement();
			if (ze.isDirectory()) {
				File f = new File(ze.getName());
				f.mkdir();
				continue;
			}
			FileOutputStream fo = new FileOutputStream(new File(this.path + "/"
					+ inputFile + "1"));
			OutputStream os = new BufferedOutputStream(fo);
			InputStream is = new BufferedInputStream(zfile.getInputStream(ze));
			int readLen = 0;
			while ((readLen = is.read(buf, 0, 1024)) != -1) {
				os.write(buf, 0, readLen);
			}
			is.close();
			os.close();
		}
		zfile.close();
	}
	
	/**
	 * uncompress the compressed file
	 * @throws IOException
	 */
	void decompression(String zipFile) throws IOException {
        ZipFile zip=new ZipFile(zipFile);
        Enumeration en=zip.entries();
        ZipEntry entry=null;
        byte[] buffer=new byte[8192];
        int length=-1;
        InputStream input=null;
        BufferedOutputStream bos=null;
        File file=null;
        
        while(en.hasMoreElements()) {
            entry=(ZipEntry)en.nextElement();
            if(entry.isDirectory()) {
               // System.out.println("directory");
                continue;
            }
            
            input=zip.getInputStream(entry);
            file=new File(this.path+"/"+inputFile + "1");

            bos=new BufferedOutputStream(new FileOutputStream(file));
            
            while(true) {
                length=input.read(buffer);
                if(length==-1) break;
                bos.write(buffer,0,length);
            }
            bos.close();
            input.close();
        }
        zip.close();
    }
	
	/**
	 * close file
	 * @throws IOException
	 */
	public void closeAllFile() throws IOException {
		randomReader.close();
	}
	/**
	 * delete temporary file
	 * @param temp
	 */
	void deleteTempFile(String temp) {
		// System.out.println("Delete file "+temp);
		File file = new File(temp);
		if (file.exists()) {
			file.delete();
			// System.out.println("Temp file is deleted");
		} else {
			// System.out.println("Temp file is not existed");
		}
	}

	public static void main(String[] args) throws IOException {
		a3search s = new a3search();
		// s.getPartterns("/dblp/article[author~\"Codd\"]");
		long startTime = System.currentTimeMillis();
		String inputfile = "outputfile";
		String path = args[0];
		String seatchkey = args[1];

		String outputfile = "";
		String tempFile = path + "/" + inputfile + "1";

		s.init(inputfile, outputfile, path);
		//s.unzip();
		s.decompression(path+"/outputfile");
		s.readInSaHM(tempFile);
		s.readInC_array(tempFile);
		s.readInS_a(tempFile);
		s.readInS_Last(tempFile);
		s.loadData(path + "/" + inputfile + "data");
		// s.search("/biblio/book[*~\"J. Austin\"]");
		s.search2(seatchkey);
		s.closeAllFile();
		s.deleteTempFile(tempFile);
//		long begin = Runtime.getRuntime().totalMemory();
//		System.out.println("Memory " + ((double) begin / (1024 * 1024)) + "MB");
		long endTime = System.currentTimeMillis();
		//System.out.println((double) (endTime - startTime) / 1000 + "s");
	}
}
