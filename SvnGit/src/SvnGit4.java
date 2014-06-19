import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Formatter;
import java.util.Scanner;

public class SvnGit4 extends Thread{

	private Scanner svn_input;
	private Scanner diff_input;
	private Formatter output;
	private String srcFile;
	private String spos, sneg;
	private String svnscript;
	private String filePath;
	private File file;
	private ProcessBuilder pb;
	private int instance;
	private int fileCount, insertions, deletions;
	private int pos, neg, sum;

	Commit commit = new Commit();

	public SvnGit4(String src_file, int inst) throws IOException {
		instance = inst;
		srcFile = src_file + "." + instance;
		svn_input = new Scanner(new BufferedReader(new FileReader(srcFile)));
		output = new Formatter(srcFile + ".git" );
		svnscript = "svnscript" + instance;

	}

	public void read() throws IOException, InterruptedException {
		
		file = new File(svnscript);
		filePath = file.getAbsolutePath();

		output = new Formatter(srcFile + ".git");
		System.out.println("Successfully opened " + srcFile + ".git");

		String line = "";
		String str = "";

		int i = 0;
		while (svn_input.hasNext()) {
			line = svn_input.nextLine();

			if (line.contains("revision=")) {
				commit.ID = line.substring(line.indexOf("=") + 2,line.indexOf(">") - 1);
				line = svn_input.nextLine();
				commit.author = line.substring(line.indexOf(">") + 1, line.indexOf("/") - 1);
				line = svn_input.nextLine();
				commit.date = line.substring(line.indexOf(">") + 1,	line.indexOf("/") - 1);
				line = svn_input.nextLine();
			}			
			if (line.startsWith("<paths")) {
				do {
					line = svn_input.nextLine();
				} while (!line.startsWith("</paths"));
				line = svn_input.nextLine();
			/*}
			if (line.startsWith("<msg")) {*/
				str = line.substring(line.indexOf(">") + 1);
				if (str.endsWith("</msg>")) {
					str = str.substring(0, str.indexOf("<"));
				} else {// it is multiline. while string does not contain
						// message, keep reading line and adding to string
					String str2 = str;
					while (!str.contains("</msg")) {
						str = svn_input.nextLine();
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
			if( i == 10000) {
				System.out.println("current commit is " + commit.ID);
				i=0;
			}
			i++;
		} // end while

		// close svn_input file
		closeFile(svn_input);

	} // end read()

	public void processFiles() throws IOException, InterruptedException {
		String diffLine = "";
		pos = 0;
		neg = 0;
		sum = 0;
		spos = "";
		sneg = "";
		String holdFile = "", nextFile = "";
		
		// create new diff file
		pb = new ProcessBuilder(filePath, commit.ID);
		Process p = pb.start();
		p.waitFor();
		p.destroy();

		// open the diff file
		diff_input = new Scanner(new BufferedReader(new FileReader(commit.ID)));
		System.out.println("Opened diff file for commit " + commit.ID);
		if (file.length() == 0) {
			System.out.println("Diff file is empty for " + commit.ID);
		}

		
		while (diff_input.hasNext()) {
			diffLine = diff_input.nextLine();
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
					//System.out.printf("...%s \t\t|  %5d %s%s \n", holdFile,
							//sum, spos, sneg);
				}
				// reset counters
				pos = 0;
				neg = 0;
				sum = 0;
				holdFile = nextFile;

			}
			
			if (diffLine.startsWith("+") && !diffLine.startsWith("+++ ")) {
				pos++;			
			}
			else if (diffLine.startsWith("-") && !diffLine.startsWith("--- ")) 
				neg++;
			
			
		} // end while

		// print last file changed
		sum = pos + neg;
		insertions += pos;
		deletions += neg;
		changeRatio();
		output.format("...%s \t\t|  %5d %s%s \n", holdFile, sum, spos, sneg);
		//System.out.printf("...%s \t\t|  %5d %s%s \n", holdFile, sum, spos, sneg);

		// close diff_input
		closeFile(diff_input);

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

	public static void main(String args[])  {

		SvnGit4 git = null;
		
		String source_file = args[0];
		int instance = Integer.parseInt(args[1]);
		try {
			git = new SvnGit4(source_file, instance);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		git.start();
		
	}

	public void run() {		
		
		try {
			read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		output.flush();
		
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
