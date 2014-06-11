import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;
import java.util.NoSuchElementException;
import java.util.Scanner;

//SvnGit2

public class SvnGit2 {
	
	private Scanner input;
	private Scanner svndiff;
	private Formatter output;
	private String srcFile;
	private int fileCount, insertions, deletions;
	private int pos, neg, sum;
	private String spos, sneg;
	
	Commit commit = new Commit();
	
	public void openFile()
	{
		//open svn log file *input
		srcFile = "avro.log";
		try	{
			input = new Scanner( new File(srcFile));	//open file for read
			System.out.println("Successfully opened " + srcFile );
		} 
		catch(NoSuchElementException | IOException exception) {
			System.err.println("Error opening source file " + srcFile );
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
			System.err.println("Error opening output file " + srcFile + ".git" );
			System.exit(1);
		}
	}
	
	public void read()
	{
		String line = "";
		String str = "";
		
		int i=0;
		while (input.hasNext())
		{
			line = input.nextLine();
			if (line.contains("revision=")) {
				str = line.substring(line.indexOf("=") + 2, line.indexOf(">")-1);
				commit.setID(str);
			}
			if(line.contains("<author>")) {
				str = line.substring(line.indexOf(">") + 1, line.indexOf("/")-1);
				commit.setAuthor(str);
			}
			if(line.contains("<date>")){
				str = line.substring(line.indexOf(">") + 1, line.indexOf("/")-1);
				commit.setDate(str);
				
			}
			if(line.contains("<msg")) {
				str = line.substring(line.indexOf(">") + 1);
				if (str.contains("</msg>")) {
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
				commit.setMessage(str);

				//print out the contents
				output.format("\n\n%s\n%s\n%s\n%s\n\n",
						"commit " + commit.getID(),
						"Author: " + commit.getAuthor(),
						"Date: " + commit.getDate(),
						"\n\tMessage: " + commit.getMessage());
				
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
	
	public void processFiles(){
		//System.out.println("Processing files in commit " + commit.getID());
		String diffLine="";
		pos = 0; neg=0; sum=0;
		spos=""; sneg="";
		String holdFile = "", nextFile = "";
				
		//create svn.diff file
		ProcessBuilder pb = new ProcessBuilder("/home/ejo36/research/SvnGit/svnGet", commit.getID());
		try {
			Process p = pb.start();
			p.waitFor();
			p.destroy();
			//System.out.println("Created diff file for " + commit.getID());
		} catch (IOException e) {
			System.err.println("error running svnGet");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		//open svn.diff file
		try	{
			File file = new File(commit.getID());
			svndiff = new Scanner (file);
			System.out.println("Opened diff file for commit " + commit.getID());
			if (file.length() == 0){
				System.out.println("Diff file is empty for " + commit.getID());
			//	System.exit(1);
			}
		} 
		catch(NoSuchElementException | FileNotFoundException exception) {
			System.err.println("Error opening file with svndiff");
			System.exit(1);
		}
		
		int i=0;
		while(svndiff.hasNext())
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
					//System.out.printf("...%s \t\t|  %5d %s%s \n", holdFile, sum, spos, sneg);
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
		//System.out.printf("...%s \t\t|  %5d %s%s \n", holdFile, sum, spos, sneg);
		
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
	}

	public static void main(String args[])
	{
		System.out.println("started");
		SvnGit2 git = new SvnGit2();
		git.openFile();
		git.read();
		git.output.flush();
		git.cleanUp();
		System.out.println("completed");
	}

}
