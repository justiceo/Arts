import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Formatter;
import java.util.NoSuchElementException;
import java.util.Scanner;



public class SvnGit3 {
	
	private Scanner input;
	private Scanner svndiff;
	private Formatter output;
	//private Formatter error;
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
	
	Commit commit = new Commit();
	
	public SvnGit3() //throws IOException - use me to cut out the crap
	{
		//open svn log file *input
		srcFile = "avro.log";
		try	{
			input = new Scanner( new BufferedReader( new FileReader(srcFile)));	//open file for read
			System.out.println("Successfully opened " + srcFile );
		} 
		catch(NoSuchElementException | IOException exception) {
			System.err.println("Error opening file " + srcFile );
			System.exit(1);
		}
		//open output file		
		try
		{
			output = new Formatter(srcFile+".git"); 
			System.out.println("Successfully opened " + srcFile + ".git");
		}
		catch( SecurityException | FileNotFoundException exception )
		{
			System.err.println("Error opening file " + srcFile + ".git" );
			System.exit(1);
		}
		
		/*open error log
		try
		{
			error = new Formatter("error.log"); 
			System.out.println("Successfully opened error.log");
		}
		catch( SecurityException | FileNotFoundException exception )
		{
			System.err.println("Error opening file error.log" );
			System.exit(1);
		}*/
		
	}
	
	public void read()
	{
		file = new File(svnscript);
		filePath = file.getAbsolutePath();
		//open output file		
				try
				{
					output = new Formatter(srcFile+".git"); 
					System.out.println("Successfully opened " + srcFile + ".git");
				}
				catch( SecurityException | FileNotFoundException exception )
				{
					System.err.println("Error opening file " + srcFile + ".git" );
					System.exit(1);
				}
				
		String line = "";
		String str = "";
		
		int i=0;
		while (input.hasNext() && i<50)
		{
			line = input.nextLine();
			
			if (line.contains("revision=")) {
				str = line.substring(line.indexOf("=") + 2, line.indexOf(">")-1);
				commit.ID = str;
				line = input.nextLine();
			}
			if(line.startsWith("<author>")) {
				str = line.substring(line.indexOf(">") + 1, line.indexOf("/")-1);
				commit.author = str;
				line = input.nextLine();
			}
			if(line.startsWith("<date>")){
				str = line.substring(line.indexOf(">") + 1, line.indexOf("/")-1);
				commit.date = str;
				line = input.nextLine();
				
			}
			if(line.startsWith("<paths")) {
				do {
					line = input.nextLine();
				}while ( !line.startsWith("</paths"));
				//line = input.nextLine();
			}
			if(line.startsWith("<msg")) {
				str = line.substring(line.indexOf(">") + 1);
				if (str.endsWith("</msg>")) {
					str = str.substring(0, str.indexOf("<"));
				}
				else {//it is multiline. while string does not contain message, keep reading line and adding to string
					String str2 = str;
					while (!str.contains("</msg")) {						
						str = input.nextLine();
						if (str.endsWith("</msg>")) {
							str = str.substring(0, str.indexOf("<"));
							str.trim();
							if(str.length() != 0) {
								str2 = str2 + "\n\t" + str;
							}
							break;
						}
						else
							str2 = str2 + "\n\t" + str;
					}
					str = str2;
				}
				commit.message = str;

				//print out the contents
				output.format("\n\n%s\n%s\n%s\n%s\n\n",
						"commit " + commit.ID,
						"Author: " + commit.author,
						"Date: " + commit.date,
						"\n\tMessage: " + commit.message);
				
				processFiles();
					
				
				output.format("%s",
						fileCount + " files changed, " + insertions + " insertions(+), " + deletions + " deletions(-).");
				//reset file counters
				fileCount=0; insertions=0; deletions=0;
				
				
			}		
			i++;
		} // end while	
		
		//close input file
		closeFile(input);
		
	} // end read()
	
	public void processFiles() /*throws IOException, InterruptedException */{
		System.out.println("Processing files in commit " + commit.ID);
		String diffLine="";
		pos = 0; neg=0; sum=0;
		spos=""; sneg="";
		String holdFile = "", nextFile = "";
				
		//create svn.diff file
		pb = new ProcessBuilder(filePath, commit.ID);
		try {
			Process p = pb.start();
			p.waitFor();
			p.destroy();
			System.out.println("Created diff file for " + commit.ID);
		} catch (IOException e) {
			System.err.println("error running svnGet");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		//open svn.diff file
		try	{
			//File file = new File(commit.ID);
			svndiff = new Scanner (new BufferedReader(new FileReader(commit.ID)));
			//svndiff = new Scanner (file);
			System.out.println("Opened diff file for commit " + commit.ID);
			if (file.length() == 0){
				System.out.println("Diff file is empty for " + commit.ID);
				//System.exit(1);
			}
		} 
		catch(NoSuchElementException | FileNotFoundException exception) {
			System.err.println("Error opening file with svndiff");
			System.exit(1);
		}
		
		int i=0;
		while(svndiff.hasNext() && i<100)
		{
			diffLine = svndiff.nextLine();
			if(diffLine.equals("h"))
				continue;			
			if(diffLine.contains("Index: ") && diffLine.substring(0,7).equals("Index: ")) {			
				// increment all counters
				//pos--; neg--;
				sum = pos + neg;
				fileCount++;
				insertions += pos;
				deletions += neg;
				
				changeRatio();
				
				
				String filePath = diffLine.substring(7);
				
				int n = 0;
				if(filePath.length()> 50) {
					do {
					n = filePath.indexOf("/");
					filePath = filePath.substring(n+1);
					} while (filePath.length()>50 && ( n != -1));
				}
				if(filePath.length() < 50) {
					do {
					filePath = filePath + " ";
					} while (filePath.length()<50 && ( n != -1));
				}
				nextFile = filePath;
				if ( !holdFile.equals("")) {
					output.format("...%s \t\t|  %5d %s%s \n", holdFile, sum, spos, sneg);
					System.out.printf("...%s \t\t|  %5d %s%s \n", holdFile, sum, spos, sneg);
				}
				//reset counters
				pos=0; neg=0; sum=0;
				spos=""; sneg="";
				holdFile = nextFile;
				
			}
			
			//testing assignment
			if (diffLine.startsWith("+")){
				pos++;
				spos= spos + "+";
			}
			if(diffLine.startsWith("-")){
				neg++;
				sneg = sneg + "-";
			}
			
			if ( diffLine.length()>3) {
				if (diffLine.substring(0,3).equals("+++")) {
					pos--;
					spos = spos.substring(1);
				}
				if (diffLine.substring(0,3).equals("---")) {
					neg--;
					sneg = sneg.substring(1);
				}
			}
			i++;
		}	//end while
		
				
		//print last file changed		
		sum = pos + neg; insertions += pos; deletions += neg;
		changeRatio();
		output.format("...%s \t\t|  %5d %s%s \n", holdFile, sum, spos, sneg);
		System.out.printf("...%s \t\t|  %5d %s%s \n", holdFile, sum, spos, sneg);
		
		//close svndiff
		closeFile(svndiff);

	}
	
	public void changeRatio()
	{
		if(sum>20)
		{
			int percent = (pos*20)/sum;
			if(spos.length()>percent)
				spos = spos.substring(0, percent);
			if(sneg.length()>(20-percent))
				sneg = sneg.substring(0, (20-percent));
		}
	}
	
		
	public void closeFile(Scanner input)
	{
		if(input != null)
		{
			input.close();
		}
	}
	
	
	public void cleanUp()
	{
		//DELETE ANY STORED FILES
		
		
		//run cleanup
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
	
	public void buildCleanupScript()
	{
		String clean_script = "clean_script";
		try
		{
			output = new Formatter(clean_script); 
			System.out.println("Successfully opened " + clean_script);
		}
		catch( SecurityException | FileNotFoundException exception )
		{
			System.err.println("Error opening file " + clean_script );
		}
		

		output.format("rm 1* 5* 6* 7* 8* 9*");
		output.flush();
		output.close();
	}
	
	// create bash script for svn diff
	public void buildSvnScript(String url, int fileNumber)
	{	   
		/** if file exits do not create it!!! */
		
		svnscript = "svnscript." + fileNumber;
		try
		{
			output = new Formatter(svnscript); 
			System.out.println("Successfully opened " + svnscript);
		}
		catch( SecurityException | FileNotFoundException exception )
		{
			System.err.println("Error opening file " + svnscript );
			System.exit(1);
		}
		
		output.format("%s \n\n%s \n%s \n\t%s \n%s ",
				"#!/bin/bash",
				"if [[ ! $# -eq 0 ]]",
				"then",
				"svn diff -c $1 " + url + " > $1",
				"fi");
		
		output.flush();
		output.close();
		
		file = new File("svnscript");
		file.setExecutable(true);
		
	}
	
	public void buildFragmenter() throws IOException {
		
		file = new File("avro.log");
		int filesize = 0;
		System.out.println("in fragmenter size is " + filesize);
	}
	
	public static void main(String args[])
	{
		
		SvnGit3 git = new SvnGit3();
		//git.buildCleanupScript();
		try {
			git.buildFragmenter();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		git.buildSvnScript("http://svn.apache.org/repos/asf/avro/", 2);
		git.start = System.currentTimeMillis();
		git.read();
		git.stop = System.currentTimeMillis();
		git.diff = git.stop - git.start;
		//git.diff = git.diff / 60000;
		git.output.flush();
		git.cleanUp();
		System.out.println("completed. Total time in read() is " + git.diff );
	}
	
	class Commit {
		String ID;
		String author;
		String  date;
		String message;
		String debug;
	}

}
