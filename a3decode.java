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
import java.util.zip.ZipInputStream;

/**
 * @author CJF
 * 
 */
public class a3decode {
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

	private FileOutputStream output;
	private RandomAccessFile randomReader;
	private StringBuffer stringbuf;

	/**
	 * output buffer size
	 */
	private static final int OUT_BUFFER = 10000;
	/**
	 * size of bl
	 */
	private long sizeOfBL;
	/**
	 * length of bl
	 */
	private long lengthOfBL;
	private long l;

	private String outputFile;
	private String inputFile;
	/**
	 * data offset
	 */
	private String[] data;

	/**
	 * the base address of data
	 */
	private long dataBase;

	/**
	 * store all position of data element
	 */
	ArrayList<Integer> dataPos = new ArrayList<Integer>();

	String path;

	/**
	 * initialize all variables
	 * 
	 * @param inputfile
	 *            input file name
	 * @param outputfile
	 *            output file name
	 * @throws FileNotFoundException
	 */
	public void init(String inputfile, String outputfile, String path)
			throws FileNotFoundException {
		s_ahm = new HashMap<Integer, String>();
		c_array = new HashMap<Integer, Integer>();
		// s_a = new ArrayList<Integer>();
		tag = new Stack<Integer>();
		this.outputFile = outputfile;
		this.inputFile = inputfile;
		this.path = path;

		stringbuf = new StringBuffer();
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
		// for(int i=0;i<dataPos.size();i++){
		// System.out.print(dataPos.get(i)+" ");
		// }
		// randomReader.close();
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
		// String s = data[index];
		// randomReader.getFilePointer();
		// System.out.println("data[" + index + "] = " + s);
		index = index - 1;
		long dataOffset = dataPos.get(index);
		randomReader.seek(dataOffset + dataBase);
		byte[] buf;
		// System.out.println(dataOffset+dataBase);
		// System.out.println("data length = "+(dataPos.get(index+1)-dataOffset));
		buf = new byte[(int) (dataPos.get(index + 1) - dataOffset)];

		randomReader.read(buf);
		String result = new String(buf).substring(1);
		// System.out.println(result);
		return result;
	}

	/**
	 * generate final xml
	 * 
	 * @throws IOException
	 */
	public void generateXML() throws IOException {
		output = new FileOutputStream(outputFile);
		this.generateChild(s_a[0][0], 0);
		if (stringbuf.length() > 0) {
			output.write(stringbuf.toString().getBytes());
		}
	}

	/**
	 * get sub tree and output them
	 * 
	 * @param parent
	 *            the index of parent node
	 * @param pos position of parent
	 * @throws IOException
	 */
	public void generateChild(int parent, int pos) throws IOException {

		if (parent != 0) {
			int[] childrenRange = this.getRange(parent, pos + 1);
			tag.push(parent);
			String e = s_ahm.get(parent).substring(1);
			// output.write((e + ">").getBytes());
			stringbuf.append("<");
			stringbuf.append(e);
			stringbuf.append(">");

			int first = childrenRange[0];
			int last = childrenRange[1];
			for (int i = first; i <= last; i++) {
				int indexOfBL = (int) (i / sizeOfBL);
				int offset = (int) (i % sizeOfBL);
				int p = s_a[indexOfBL][offset];
				if (p >= 0) {
					generateChild(p, i);
				}

			}
			tag.pop();
			// e = "</" + e.substring(1) + ">";
			// output.write(e.getBytes());
			stringbuf.append("</");
			stringbuf.append(e);
			stringbuf.append(">");

		} else {
			// String data = this.getData(childrenRange[0], inputFile);
			// output.write(data.getBytes());
			int rank = rank(0, pos + 1);
			// System.out.println("data rank = "+rank);
			String d = this.getData(rank);
			// output.write(d.getBytes());
			stringbuf.append(d);
		}
		if (stringbuf.length() > OUT_BUFFER) {
			output.write(stringbuf.toString().getBytes());
			stringbuf = new StringBuffer();
		}

	}

	/**
	 * get the number of occurrence of value in s_a
	 * 
	 * @param valueInsa the index of value in sa
	 * @param pos position of value in sa 
	 * @return how many times it occurs in s_a
	 */
	public int rank(int valueInsa, int pos) {
		int rank = 0;
		int indexOfBL = (int) ((pos - 1) / sizeOfBL);
		int offset = (int) ((pos - 1) % sizeOfBL);

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
		// System.out.println("index  = " + index + " offset = " + offset);
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

		// System.out.println("rank1("+i+") =  "+result);
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
			// System.out.println("BLforLast[index - 1]"+BLforLast[index - 1]);
			// System.out.println("BLforLast[index]"+BLforLast[index]);
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
	public int[] getRange(int valueInsa, int pos) {
		int[] range = new int[2];
		// System.out.println("valueInsa = " + valueInsa + " index = " + index);
		int rank = this.rank(valueInsa, pos);
		int y = c_array.get(valueInsa);
		// System.out.println("y = " + y);
		int z = this.rankInLast(y - 1);
		// System.out.println("z = " + z);
		int first = this.selectInLast(z + rank - 1) + 1;
		int last = this.selectInLast(z + rank);

		range[0] = first - 1;
		range[1] = last - 1;
		// System.out.println("first = " + range[0] + " last = " + range[1]);
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
			HashMap<Byte, Integer> tmp = new HashMap<Byte, Integer>();
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
		// reader.
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

		///System.out.println("size Of BL = " + sizeOfBL);
		//System.out.println("length Of BL = " + lengthOfBL);

		for (int i = 0; i < (int) lengthOfBL; i++) {
			for (int j = 0; j < (int) sizeOfBL; j++) {
				s_a[i][j] = -1;
			}
		}

		int index = 0;
		while (index < lengthOfBL) {
			HashMap<Integer, Integer> tmpHM = new HashMap<Integer, Integer>();

			// System.out.println("buf = " + buf);
			// String[] s = buf.split(",");
			int end;
			if (dataLength < (index + 1) * sizeOfBL) {
				end = (int) dataLength;
			} else {
				end = (int) ((1 + index) * sizeOfBL);
			}
			// System.out.println("end = "+end);
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
			// System.out.println(e + " " + sNum);
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
		ZipFile zfile = new ZipFile(this.path + "/" + inputFile);
		Enumeration zList = zfile.entries();
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
			fo.flush();
			fo.close();
			is.close();
			os.close();
		}
		zfile.close();
	}
	/**
	 * uncompress the compressed file
	 * 
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
	 * delete temporary file
	 * @param temp
	 */
	public void deleteTempFile(String temp) {
		//System.out.println("Delete file " + temp);
		File file = new File(temp);
		if (file.exists()) {
			file.delete();
			//System.out.println("Temp file is deleted");
		} else {
			//System.out.println("Temp file is not existed");
		}
	}

	/**
	 * close file
	 * @throws IOException
	 */
	public void closeAllFile() throws IOException {
		randomReader.close();
	}

	/**
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		long startTime = System.currentTimeMillis();
		a3decode de = new a3decode();
		String path = args[0];
		String inputfile = "outputfile";
		String outputfile = args[1];
		String tempFile = path + "/" + inputfile + "1";

		de.init(inputfile, outputfile, path);
		//de.unzip();
		de.decompression(path + "/" + inputfile);
		de.readInSaHM(tempFile);
		de.readInC_array(tempFile);
		de.readInS_a(tempFile);
		de.readInS_Last(tempFile);
		de.loadData(path + "/" + inputfile + "data");

		de.generateXML();
		de.closeAllFile();
		de.deleteTempFile(tempFile);
		//System.out.println("Decode Complete");
		// de.generateChild(2, 3);
		// de.getData(4, inputfile);
//		long begin = Runtime.getRuntime().totalMemory();
//		System.out.println("Memory " + ((double) begin / (1024 * 1024)) + "MB");
//		long endTime = System.currentTimeMillis();
//		System.out.println((double) (endTime - startTime) / 1000 + "s");
	}

}
