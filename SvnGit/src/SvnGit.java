import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;
import java.util.NoSuchElementException;
import java.util.Scanner;



public class SvnGit {
	
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
			System.err.println("Error opening file");
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
			System.err.println(
					"You do not have write access to this file" );
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
		String diffLine="";
		pos = 0; neg=0; sum=0;
		spos=""; sneg="";
		String holdFile = "", nextFile = "";
				
		//create svn.diff file
		ProcessBuilder pb = new ProcessBuilder("/home/ejo36/Research Projects/svnGet", commit.getID());
		try {
			Process p = pb.start();
			p.waitFor();
			p.destroy();
		} catch (IOException e) {
			System.err.println("error running svnGet");
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		//open svn.diff file
		try	{
			svndiff = new Scanner( new File(commit.getID()));	//open file for read
			System.out.println("current commit " + commit.getID());
		} 
		catch(NoSuchElementException | FileNotFoundException exception) {
			System.err.println("Error opening file with svndiff");
			System.exit(1);
		}
		
		int i=0;
		while(svndiff.hasNext())
		{
			diffLine = svndiff.nextLine();
			if(diffLine.equals(""))
				continue;			
			if(diffLine.contains("Index: ") && diffLine.substring(0,7).equals("Index: ")) {
				// increment all counters
				sum = pos + neg;
				fileCount++;
				insertions += pos;
				deletions += neg;
				
				// calculate positive to negative change ratio
				changeRatio();				
				
				String filePath = diffLine.substring(7);
				nextFile = filePath;
				/*if(filePath.length()> 40)
					filePath = filePath.substring((filePath.length()-40), (filePath.length()-1));*/
				if ( !holdFile.equals("")) {
					output.format("..."+ holdFile + "\t\t | " + sum + " " + spos + sneg + "\n");
					System.out.println("..."+ holdFile + "\t\t | " + sum + " " + spos + sneg);
				}				
				//reset counters
				pos=0; neg=0; sum=0;
				spos=""; sneg="";
				holdFile = nextFile;
				
			}
			if(diffLine.substring(0,1).equals("+")){
				pos++;
				spos= spos + "+";
			}
			if(diffLine.substring(0,1).equals("-")){
				neg++;
				sneg = sneg + "-";
			}	
			i++;
		}	//end while
		
		//print last file changed
		sum = pos + neg; insertions += pos; deletions += neg;
		changeRatio();
		output.format("..." + holdFile + "\t\t" + sum + " " + spos + sneg + "\n");
		
		//close svndiff
		closeFile(svndiff);

	}
	
	// calculate positive to negative change ratio
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
	
	
	public static void main(String args[])
	{
		SvnGit git = new SvnGit();
		git.openFile();
		git.read();
		git.output.flush();
		System.out.println("completed");
	}

}
