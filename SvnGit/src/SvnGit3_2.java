import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Formatter;
import java.util.Scanner;

public class SvnGit3_2 extends Thread{

	private Scanner input;
	private Scanner svndiff;
	private Formatter output;
	private String srcFile;
	private int fileCount, insertions, deletions;
	private int pos, neg, sum;
	private String spos, sneg;
	private String svnscript;
	String filePath;
	private File file;
	ProcessBuilder pb;
	long start = 0;
	long stop = 0;
	long diff = 0;
	private int instance;

	Commit commit = new Commit();

	public SvnGit3_2(String src_file, int inst) throws IOException {
		instance = inst;
		srcFile = src_file + "." + instance;
		input = new Scanner(new BufferedReader(new FileReader(srcFile)));
		output = new Formatter(srcFile + ".git" );

	}

	public void read() throws IOException, InterruptedException {
		
		file = new File(svnscript);
		filePath = file.getAbsolutePath();

		output = new Formatter(srcFile + ".git");
		System.out.println("Successfully opened " + srcFile + ".git");

		String line = "";
		String str = "";

		int i = 0;
		while (input.hasNext() && i < 100) {
			line = input.nextLine();

			if (line.contains("revision=")) {
				commit.ID = line.substring(line.indexOf("=") + 2,line.indexOf(">") - 1);
				line = input.nextLine();
				commit.author = line.substring(line.indexOf(">") + 1, line.indexOf("/") - 1);
				line = input.nextLine();
				commit.date = line.substring(line.indexOf(">") + 1,	line.indexOf("/") - 1);
				line = input.nextLine();
			}			
			if (line.startsWith("<paths")) {
				do {
					line = input.nextLine();
				} while (!line.startsWith("</paths"));
				line = input.nextLine();
			/*}
			if (line.startsWith("<msg")) {*/
				str = line.substring(line.indexOf(">") + 1);
				if (str.endsWith("</msg>")) {
					str = str.substring(0, str.indexOf("<"));
				} else {// it is multiline. while string does not contain
						// message, keep reading line and adding to string
					String str2 = str;
					while (!str.contains("</msg")) {
						str = input.nextLine();
						if (str.endsWith("</msg>")) {
							str = str.substring(0, str.indexOf("<"));
							str.trim();
							if (str.length() != 0) {
								str2 = str2 + "\n\t" + str;
							}
							break;
						} else
							str2 = str2 + "\n\t" + str;
					}
					str = str2;
				}
				commit.message = str;

				// print out the contents
				output.format("\n\n%s\n%s\n%s\n%s\n\n", "commit " + commit.ID,
						"Author: " + commit.author, "Date: " + commit.date,
						"\n\tMessage: " + commit.message);

				processFiles();

				output.format("%s", fileCount + " files changed, " + insertions
						+ " insertions(+), " + deletions + " deletions(-).");
				// reset file counters
				fileCount = 0;
				insertions = 0;
				deletions = 0;

			}
			i++;
		} // end while

		// close input file
		closeFile(input);

	} // end read()

	public void processFiles() throws IOException, InterruptedException {
		String diffLine = "";
		pos = 0;
		neg = 0;
		sum = 0;
		spos = "";
		sneg = "";
		String holdFile = "", nextFile = "";

		// create svn.diff file
		pb = new ProcessBuilder(filePath, commit.ID);
		Process p = pb.start();
		p.waitFor();
		p.destroy();

		// open svn.diff file
		svndiff = new Scanner(new BufferedReader(new FileReader(commit.ID)));
		// svndiff = new Scanner (file);
		System.out.println("Opened diff file for commit " + commit.ID);
		if (file.length() == 0) {
			System.out.println("Diff file is empty for " + commit.ID);
			// System.exit(1);
		}

		int i = 0;
		while (svndiff.hasNext()) {
			diffLine = svndiff.nextLine();
			if (diffLine.equals("h"))
				continue;
			if (diffLine.contains("Index: ")
					&& diffLine.substring(0, 7).equals("Index: ")) {
				// increment all counters
				// pos--; neg--;
				sum = pos + neg;
				fileCount++;
				insertions += pos;
				deletions += neg;

				changeRatio();

				String filePath = diffLine.substring(7);

				int n = 0;
				if (filePath.length() > 50) {
					do {
						n = filePath.indexOf("/");
						filePath = filePath.substring(n + 1);
					} while (filePath.length() > 50 && (n != -1));
				}
				if (filePath.length() < 50) {
					do {
						filePath = filePath + " ";
					} while (filePath.length() < 50 && (n != -1));
				}
				nextFile = filePath;
				if (!holdFile.equals("")) {
					output.format("...%s \t\t|  %5d %s%s \n", holdFile, sum,
							spos, sneg);
					System.out.printf("...%s \t\t|  %5d %s%s \n", holdFile,
							sum, spos, sneg);
				}
				// reset counters
				pos = 0;
				neg = 0;
				sum = 0;
				holdFile = nextFile;

			}

			// testing assignment
			if (diffLine.startsWith("+") && !diffLine.startsWith("+++ ")) {
				pos++;			
			}
			else if (diffLine.startsWith("-") && !diffLine.startsWith("--- ")) 
				neg++;
			
			i++;
		} // end while

		// print last file changed
		sum = pos + neg;
		insertions += pos;
		deletions += neg;
		changeRatio();
		output.format("...%s \t\t|  %5d %s%s \n", holdFile, sum, spos, sneg);
		System.out
				.printf("...%s \t\t|  %5d %s%s \n", holdFile, sum, spos, sneg);

		// close svndiff
		closeFile(svndiff);

	}

	public void changeRatio() {
		spos = "++++++++++++++++++++";
		sneg = "--------------------";
		
		if (sum > 20) {
			int percent = (pos * 20) / sum;
			spos = spos.substring(0, percent);
			sneg = sneg.substring(0, (20 - percent));
		}
		else {
			spos = spos.substring(0, pos);
			sneg = sneg.substring(0, neg);
		}
	}

	public void closeFile(Scanner input) {
		if (input != null) {
			input.close();
		}
	}

	public void cleanUp() {
		// DELETE ANY STORED FILES

		// run cleanup
		file = new File("clean_script");
		file.setExecutable(true);
		filePath = file.getAbsolutePath();
		pb = new ProcessBuilder(filePath);
		try {
			Process p = pb.start();
			p.waitFor();
			p.destroy();
			System.out.println("\ncleaned up files\n");
		} catch (IOException e) {
			System.err.println("error running cleanup");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void buildCleanupScript() throws IOException {
		String clean_script = "clean_script";
		output = new Formatter(clean_script);
		System.out.println("Successfully opened " + clean_script);

		output.format("rm 1* 5* 6* 7* 8* 9*");
		output.flush();
		output.close();
	}

	public void buildSvnScript(String url, int instance) throws FileNotFoundException {
		/** if file exits do not create it!!! */

		svnscript = "svnscript" + instance;
		output = new Formatter(svnscript);
		System.out.println("Successfully opened " + svnscript);

		output.format("%s \n\n%s \n%s \n\t%s \n%s ", "#!/bin/bash",
				"if [[ ! $# -eq 0 ]]", "then", "svn diff -c $1 " + url
						+ " > $1", "fi");

		output.close();

		file = new File("svnscript");

	}

	public void buildFragmenter() throws IOException {

		file = new File("avro.log");
		int filesize = 0;
		System.out.println("in fragmenter size is " + filesize);
	}

	public static void main(String args[])  {

		SvnGit3_2 git = null;
		int instance = 1;
		String source_file = "avro.log";
		try {
			git = new SvnGit3_2(source_file, instance);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		git.start();
		
		SvnGit3_2 git2 = null;
		instance++;
		try {
			git2 = new SvnGit3_2(source_file, instance);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		git2.start();
		
		/*try {
			git.buildCleanupScript();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
	}

	public void run() {
		
		try {
			buildSvnScript("http://svn.apache.org/repos/asf/avro/", instance);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		start = System.currentTimeMillis();
		try {
			read();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		stop = System.currentTimeMillis();
		diff = stop - start;
		output.flush();
		System.out.println("completed. Total time in read() is " + diff);
		
		//cleanUp();
	}
	class Commit {
		String ID;
		String author;
		String date;
		String message;
		String debug;
	}

}
