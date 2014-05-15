
public class Commit 
{
	private String ID;
	private String author;
	private String  date;
	private String message;
	private String debug;
	
	public Commit()
	{
		this( "NULL", "NULL", "NULL", "NULL");
	}
	
	public Commit(String id, String auth, String time, String mssg)
	{
		setID(id);
		setAuthor(auth);
		setDate(time);
		setMessage(mssg);
	}
	
	public void setID(String id)
	{
		ID = id;
	}
	
	public String getID()
	{
		return ID;
	}
	
	public void setAuthor(String auth)
	{
		author = auth;
	}
	
	public String getAuthor()
	{
		return author;
	}
	
	public void setDate(String time)
	{
		date = time;
	}
	
	public String getDate()
	{
		return date;
	}
	
	public void setMessage(String mssg)
	{
		message = mssg;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public void setDebug(String bug)
	{
		debug = bug;
	}
	
	public String getDebug()
	{
		return debug;
	}
	

}
