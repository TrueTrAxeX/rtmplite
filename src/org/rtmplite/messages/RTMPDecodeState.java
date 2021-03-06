package org.rtmplite.messages;

/**
 * Represents current decode state of the protocol.
 */
public class RTMPDecodeState {

	/**
	 * Session id to which this decoding state belongs.
	 */
	public String sessionId;
	
    /**
     * Decoding finished successfully state constant.
     */
    public static byte DECODER_OK = 0x00;

    /**
     * Decoding continues state constant.
     */
    public static byte DECODER_CONTINUE = 0x01;

    /**
     * Decoder is buffering state constant.
     */
    public static byte DECODER_BUFFER = 0x02;

    /**
     * Classes like the RTMP state object will extend this marker interface.
     */
	private int decoderBufferAmount;

    /**
     * Current decoder state, decoder is stopped by default.
     */
    private byte decoderState = DECODER_OK;
	
	public RTMPDecodeState(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Returns current buffer amount.
	 *
	 * @return	Buffer amount
	 */
	public int getDecoderBufferAmount() {
		return decoderBufferAmount;
	}
	
	/**
	 * Specifies buffer decoding amount
	 * 
	 * @param amount        Buffer decoding amount
	 */
	public void bufferDecoding(int amount) {
		decoderState = DECODER_BUFFER;
		decoderBufferAmount = amount;
	}
	
	/**
	 * Set decoding state as "needed to be continued".
	 */
	public void continueDecoding() {
		decoderState = DECODER_CONTINUE;
	}
	
	/**
	 * Checks whether remaining buffer size is greater or equal than buffer amount and so if it makes sense to start decoding.
	 * 
	 * @param remaining		Remaining buffer size
	 * @return				<code>true</code> if there is data to decode, <code>false</code> otherwise
	 */
	public boolean canStartDecoding(int remaining) {
		return remaining >= decoderBufferAmount;
	}
	
	/**
	 * Starts decoding. Sets state to "ready" and clears buffer amount.
	 */
	public void startDecoding() {
		decoderState = DECODER_OK;
		decoderBufferAmount = 0;
	}
	
	/**
	 * Checks whether decoding is complete.
	 *
	 * @return	<code>true</code> if decoding has finished, <code>false</code> otherwise
	 */
	public boolean hasDecodedObject() {
		return (decoderState == DECODER_OK);
	}
	
	/**
	 * Checks whether decoding process can be continued.
	 *
	 * @return	<code>true</code> if decoding can be continued, <code>false</code> otherwise
	 */
	public boolean canContinueDecoding() {
		return (decoderState != DECODER_BUFFER);
	}

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * @param sessionId the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RTMPDecodeState [sessionId=" + sessionId + ", decoderState=" + decoderState + ", decoderBufferAmount=" + decoderBufferAmount + "]";
	}

}
