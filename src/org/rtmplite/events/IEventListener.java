package org.rtmplite.events;

public interface IEventListener {

	/**
	 * Notify of event. 
	 * @param event the event object
	 */
	public void notifyEvent(IEvent event);

}
