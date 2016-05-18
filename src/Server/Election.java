package Server;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import Model.*;

public class Election {
	private List<VoteItem> votes;
	public String state;
	public String electionResults;
	public String question;
	public List<byte[]> L;
	public List<String> M;
	private BigInteger h;

	public Election() throws NoSuchAlgorithmException {
		votes = new ArrayList<VoteItem>();
		state = "initial";

		L = new ArrayList<byte[]>();
		byte[] ElTrezorPublicKey65Bytes = new byte[] { 4, -61, 127, -43, -89, 84, -54, -6, 99, -15, 98, 23, 36, 123,
				-12, -66, 84, -31, -54, -59, 3, -67, 55, 100, -65, -121, -91, 1, 37, -120, -125, -99, -32, 55, 51, 96,
				-95, -43, -51, 2, -21, 104, -8, 38, -25, -32, 20, -100, 110, -50, 126, -15, -19, -84, -100, -108, -16,
				51, 109, 67, 53, 22, 117, -117, 33 };
		byte[] ElBandidoPublicKey65Bytes = new byte[] { 4, 57, 116, 59, -118, -35, 98, -90, -31, 93, 81, 3, -112, 58,
				-66, 9, -104, -112, -102, 6, -30, 99, 116, -115, -111, 86, -68, -100, 23, -77, -105, 29, -32, 122, -63,
				-70, 106, 34, 65, 111, -80, -33, 33, 11, -55, 123, 74, 41, 73, -88, 105, -98, -57, 42, -87, 69, 7, -63,
				-73, 15, -83, 117, 46, -53, 79 };
		byte[] FakePublicKey65Bytes = new byte[] { 4, -45, -70, -18, -2, 38, 26, 69, -123, -19, 94, -122, 0, 84, -68,
				56, 115, -3, 103, -91, -22, 58, 117, -84, -1, 79, 54, 18, -64, -42, -98, -41, 93, 122, -97, -50, 96,
				-61, 29, 120, -70, -59, -37, 37, -36, 62, 2, -8, -71, -122, 56, 75, 123, 91, -33, -43, 26, 99, -56, -39,
				56, 33, -17, 100, 46 };

		L.add(ElTrezorPublicKey65Bytes);
		L.add(FakePublicKey65Bytes);
		L.add(ElBandidoPublicKey65Bytes);

		question = "Vote for president: ";
		M = new ArrayList<String>();
		M.add("Basescu");
		M.add("Johannis");
		M.add("Ponta");
	}

	@SuppressWarnings("deprecation")
	private boolean verifySignature(String message, MyVote vote) {

		ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

		BigInteger m = messageToBigInteger(message).mod(ecSpec.getCurve().getOrder());

		// turn h into Point H
		BigInteger h = getElectionHash().mod(ecSpec.getCurve().getOrder());
		ECPoint H = ecSpec.getG().multiply(h);

		// compute the ring
		BigInteger c = new BigInteger(1, vote.c);
		ECPoint Yt = ecSpec.getCurve().decodePoint(vote.getYtilda());

		for (int i = 0; i < vote.n; i++) {
			BigInteger si = new BigInteger(1, vote.s[i]);
			ECPoint Yi = ecSpec.getCurve().decodePoint(L.get(i));

			ECPoint MathG1 = ecSpec.getG().multiply(si);
			ECPoint MathG2 = Yi.multiply(c);
			ECPoint MathG = MathG1.add(MathG2);

			ECPoint MathH1 = H.multiply(si);
			ECPoint MathH2 = Yt.multiply(c);
			ECPoint MathH = MathH1.add(MathH2);

			ECPoint MathT = MathG.add(MathH);
			ECPoint Result = MathT.multiply(m);

			c = Result.getY().toBigInteger();
		}

		BigInteger originalC = new BigInteger(1, vote.c);

		return originalC.equals(c);
	}

	private BigInteger messageToBigInteger(String message) {
		// turn message in BigInteger m
		byte[] mhash = new byte[32];
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			mhash = digest.digest(message.getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return BigInteger.ONE;
		}
		return new BigInteger(1, mhash);
	}

	private BigInteger getElectionHash() {
		
		if (h != null)
			return h;
		
		byte[] ytotalBytes = new byte[65 * L.size()];
		int from = 0;
		for (byte[] pk : L) {
			System.arraycopy(pk, 0, ytotalBytes, from, pk.length);
			from += pk.length;
		}

		byte[] hash = new byte[32];
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			hash = digest.digest(ytotalBytes);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return BigInteger.ONE;
		}

		h = new BigInteger(1, hash);
		return h;
	}

	public synchronized boolean CastVote(MyVote vote) {
		if (state.equals("voting")) {
			for (String message : M)
				if (verifySignature(message, vote)) {
					checkMultipleVoting(vote.getYtilda());
					votes.add(new VoteItem(message, vote.getYtilda()));
					return true;
				}
		}
		return false;
	}

	private void checkMultipleVoting(byte[] Ytilda) {
		VoteItem toRemove = null;
		for (VoteItem existing : votes)
			if (existing.isLinked(Ytilda)) {
				toRemove = existing;
				break;
			}
		if (toRemove != null)
			votes.remove(toRemove);
	}

	public synchronized String getResults() {
		if (state.equals("stopped")) {
			return electionResults;
		}
		return null;
	}

	private void calculateResults() {
		int[] voteCount = new int[M.size()];
		for (int i = 0; i < voteCount.length; i++) {
			voteCount[i] = 0;
		}

		for (VoteItem vote : votes) {
			voteCount[M.indexOf(vote.message)]++;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < M.size(); i++) {
			sb.append(voteCount[i] + " : " + M.get(i) + "\n");
		}

		electionResults = sb.toString();
	}

	public synchronized ElectionParameters getElectionParameters(byte[] pk) {
		int pi = -1;
		int i=0;
		for (byte[] bs : L) {
			if (Arrays.equals(bs, pk)) {
				pi=i;
				break;
			}
			i++;
		}
		return new ElectionParameters(L, question, M, pi);
	}

	public void startElection() {
		if (state.equals("initial")) {
			state = "voting";
		}
	}

	public void stopElection() {
		if (state.equals("voting")) {
			state = "stopped";
			calculateResults();
		}
	}

	public String getElectionInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append(question + "\n");
		sb.append("Candidates : " + "\n");
		for (String candidate : M) {
			sb.append(candidate + "\n");
		}
		return sb.toString();
	}

	public List<VoteItem> getRawVotes() {
		List<VoteItem> listCopy = new ArrayList<VoteItem>();
		for (VoteItem vote : votes) {
			listCopy.add(new VoteItem(vote.message, vote.Yt));
		}
		return listCopy;
	}

}