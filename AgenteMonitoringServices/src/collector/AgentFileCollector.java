package collector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;

import monitoring.MonitoringConstants;
import client.MonitoringClient;

public class AgentFileCollector {

	private static String NET_ADDRESSES = "net_addresses";
	private static String PORT = "port";

	private static String TEMP_PATH = "temp_path";
	private static String SAVE_PATH = "file_save";

	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		InputStream inputStream = new FileInputStream(new File("AgentFileCollector.properties"));
		prop.load(inputStream);

		String[] addresses = getNetAddresses(prop.getProperty(NET_ADDRESSES)); 
		int port = Integer.parseInt(prop.getProperty(PORT));
		String tempPath = prop.getProperty(TEMP_PATH);		
		System.out.println();
		for (String string : addresses) {
			System.out.println(string);
		}

		/*for (int i = 0; i < addresses.length; i++) {
			new MonitoringClient(addresses[i], port, tempPath).getFiles();
			System.out.println((i+i)+"/"+addresses.length+" completed hosts");
		}

		saveFiles(prop.getProperty(TEMP_PATH), prop.getProperty(SAVE_PATH));*/

	}

	/**
	 * Returns an array with each individual IP in a comma separated list that may include ranges
	 * @param addresses: a comma separated list of IPv4 addresses. Any element may be a range of the form "IPaddress1-IPaddress2"
	 * Example: "200.168.25.3, 198.162.20.25-198.162.20.50, 200.168.25.56"
	 * @return An array with the contained addresses
	 * @throws UnknownHostException: If an element doesn't have a correct IP format 
	 */
	public static String[] getNetAddresses(String addresses) throws UnknownHostException {
		String[] tmp  = addresses.split("\\s*,\\s*");

		ArrayList<String> resp = new ArrayList<String>();
		for (int i = 0; i < tmp.length; i++) {
			if(tmp[i].matches(".*\\s*-\\s*.*")) {
				String[] range = tmp[i].split("\\s*-\\s*");
				byte[] current = InetAddress.getByName(range[0]).getAddress();

				resp.add(range[0]);
				String lastAdded = range[0];
				while(!lastAdded.equals(range[1])) {
					for (int j = 3; j >= 0; j--) {
						current[j]++;
						if(Byte.toUnsignedInt(current[j]) == 255) 
							current[j] = 1;
						else
							break;
					}
					lastAdded = InetAddress.getByAddress(current).getHostAddress();
					resp.add(lastAdded);
				}
			} else 
				resp.add(tmp[i]);
		}

		return resp.toArray(new String[1]);
	}

	/**
	 * Saves the temp files to the saved files directory using a punctual file structure
	 * @param tempPath
	 * @param savePath
	 */
	public static void saveFiles(String tempPath, String savePath) {

		File temp =  new File(tempPath);
		File[] tempFiles = temp.listFiles();
		for (int i = 0; i < tempFiles.length; i++) {
			File file = tempFiles[i];
			String workingDate = getFileStartDate(file);
			String sensor = getFileSensorName(file);
			String hostName = getFileHostName(file);
			String newFileName = file.getName().substring(8, file.getName().length());
			
			File savedFolder = new File(savePath+File.separator+ workingDate +File.separator+ sensor +File.separator+ hostName);
			if(!savedFolder.exists())
				savedFolder.mkdirs();
			
			File savedFile = new File(savePath +File.separator+ workingDate +File.separator+ sensor +File.separator+ hostName +File.separator+ newFileName);
			file.renameTo(savedFile);
		}
	}

	private static String getFileHostName(File file) {
		String fileName = file.getName();
		if(fileName.contains("__")) {
			String[] tmp = fileName.split("__")[0].split("_");
			return tmp[tmp.length-1];
		} else {
			String[] tmp = fileName.split("_-")[0].split("_");
			return tmp[tmp.length-1];
		}
	}

	private static String getFileStartDate(File file) {
		String fileName = file.getName();
		if(fileName.contains("__")) {
			return fileName.split("__")[1].substring(0, 10);
		} else {
			String[] tmp = fileName.split("_");
			return tmp[tmp.length-2].substring(1, tmp[tmp.length-2].length());
		}
	}

	private static String getFileSensorName(File file) {
		String fileName = file.getName();
		if(fileName.contains("power_gadget"))
			return "PowerGadget";
		else if(fileName.contains("sigar"))
			return "Sigar";
		else if(fileName.contains("perfmon"))
			return "Perfmon";
		else if(fileName.contains("open_hardware"))
			return "OpenHardwareMonitor";
		
		return "???";
	}
}
