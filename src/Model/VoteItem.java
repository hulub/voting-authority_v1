package Model;

import java.io.Serializable;
import java.util.Arrays;

public class VoteItem implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public String message;
	public byte[] Yt;
	
	public VoteItem (String message, byte[] Yt) {
		this.message=message;
		this.Yt = Yt;
	}

	public boolean isLinked(byte[] ytilda) {
		return Arrays.equals(Yt, ytilda);
	}
}
